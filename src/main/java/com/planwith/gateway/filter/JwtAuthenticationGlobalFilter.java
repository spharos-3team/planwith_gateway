package com.planwith.gateway.filter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Member Service가 기대하는 사용자 식별 헤더를 downstream에 전달한다.
 *
 * <p>memberUuid는 JWT {@code sub} claim에서 추출하며, 헤더는 {@code X-Auth-User-Id}로 전달한다
 * (Member {@code GatewayAuthenticationContextResolver} 규약).
 *
 * <p>클라이언트가 보낸 식별 헤더는 신뢰하지 않고 제거한 뒤, 검증된 JWT 값으로만 다시 설정한다.
 * JWT 검증 자체는 {@link JwtAuthenticationWebFilter} + SecurityConfig가 담당한다.
 */
@Component
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGlobalFilter.class);

	public static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";
	public static final String HEADER_AUTH_ROLES = "X-Auth-Roles";
	public static final String HEADER_AUTH_SCOPES = "X-Auth-Scopes";
	public static final String HEADER_AUTH_SESSION_ID = "X-Auth-Session-Id";
	/** 문서/외부 관례용 이름. 클라이언트가 보낸 값은 제거하고 신뢰하지 않는다. */
	public static final String HEADER_MEMBER_UUID_ALIAS = "X-Member-UUID";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest sanitized = stripClientIdentityHeaders(exchange.getRequest());
		Jwt jwt = exchange.getAttribute(JwtAuthenticationWebFilter.EXCHANGE_JWT_ATTR);
		ServerHttpRequest downstream = jwt != null ? applyJwtIdentity(sanitized, jwt) : sanitized;
		return chain.filter(exchange.mutate().request(downstream).build());
	}

	private ServerHttpRequest stripClientIdentityHeaders(ServerHttpRequest request) {
		return request.mutate()
				.headers(headers -> {
					headers.remove(HEADER_AUTH_USER_ID);
					headers.remove(HEADER_AUTH_ROLES);
					headers.remove(HEADER_AUTH_SCOPES);
					headers.remove(HEADER_AUTH_SESSION_ID);
					headers.remove(HEADER_MEMBER_UUID_ALIAS);
				})
				.build();
	}

	private ServerHttpRequest applyJwtIdentity(ServerHttpRequest request, Jwt jwt) {
		String memberUuid = jwt.getSubject();
		if (!StringUtils.hasText(memberUuid)) {
			log.warn("memberUuid extraction failed: missing sub claim");
			return request;
		}

		log.info("JWT validation succeeded");
		log.info("memberUuid extracted");

		return request.mutate()
				.headers(headers -> {
					headers.set(HEADER_AUTH_USER_ID, memberUuid);

					String roles = claimAsCommaSeparated(jwt.getClaim("roles"));
					if (StringUtils.hasText(roles)) {
						headers.set(HEADER_AUTH_ROLES, roles);
					}

					String scopes = claimAsCommaSeparated(jwt.getClaim("scope"));
					if (StringUtils.hasText(scopes)) {
						headers.set(HEADER_AUTH_SCOPES, scopes);
					}

					String sessionId = jwt.getClaimAsString("session_id");
					if (StringUtils.hasText(sessionId)) {
						headers.set(HEADER_AUTH_SESSION_ID, sessionId);
					}
				})
				.build();
	}

	@SuppressWarnings("unchecked")
	private static String claimAsCommaSeparated(Object claim) {
		if (claim == null) {
			return null;
		}
		if (claim instanceof String value) {
			return value.trim();
		}
		if (claim instanceof Collection<?> values) {
			return values.stream()
					.map(String::valueOf)
					.filter(StringUtils::hasText)
					.collect(Collectors.joining(","));
		}
		if (claim instanceof String[] values) {
			return String.join(",", List.of(values));
		}
		return String.valueOf(claim);
	}

	@Override
	public int getOrder() {
		return -100;
	}
}
