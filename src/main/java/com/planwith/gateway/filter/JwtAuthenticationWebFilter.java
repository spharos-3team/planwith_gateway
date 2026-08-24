package com.planwith.gateway.filter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Bearer JWT를 검증해 SecurityContext에 올린다. 인가(permitAll vs authenticated)는
 * {@link com.planwith.gateway.config.SecurityConfig}가 수행한다.
 *
 * <p>공개 API에 만료/위조 토큰이 실려 있어도 여기서 401을 내지 않는다. 보호 API는
 * Authentication이 없으면 Security가 401을 낸다.
 */
public class JwtAuthenticationWebFilter implements WebFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationWebFilter.class);

	public static final String EXCHANGE_JWT_ATTR = JwtAuthenticationWebFilter.class.getName() + ".JWT";

	private static final String BEARER_PREFIX = "Bearer ";

	private final ReactiveJwtDecoder jwtDecoder;

	public JwtAuthenticationWebFilter(ReactiveJwtDecoder jwtDecoder) {
		this.jwtDecoder = jwtDecoder;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (!StringUtils.hasText(authorization) || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			return chain.filter(exchange);
		}

		String token = authorization.substring(BEARER_PREFIX.length()).trim();
		if (!StringUtils.hasText(token)) {
			return chain.filter(exchange);
		}

		return jwtDecoder.decode(token)
				.materialize()
				.flatMap(signal -> {
					if (signal.isOnError()) {
						log.warn("JWT validation failed: {}", signal.getThrowable().getMessage());
						return chain.filter(exchange);
					}
					Jwt jwt = signal.get();
					if (jwt == null || !StringUtils.hasText(jwt.getSubject())) {
						if (jwt != null) {
							log.warn("memberUuid extraction failed: missing sub claim");
						}
						return chain.filter(exchange);
					}
					exchange.getAttributes().put(EXCHANGE_JWT_ATTR, jwt);
					JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities(jwt));
					return chain.filter(exchange)
							.contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
				});
	}

	@SuppressWarnings("unchecked")
	private static Collection<GrantedAuthority> authorities(Jwt jwt) {
		Object claim = jwt.getClaim("roles");
		if (claim instanceof Collection<?> values) {
			return values.stream()
					.map(String::valueOf)
					.filter(StringUtils::hasText)
					.map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
					.map(SimpleGrantedAuthority::new)
					.collect(Collectors.toList());
		}
		if (claim instanceof String value && StringUtils.hasText(value)) {
			String role = value.startsWith("ROLE_") ? value : "ROLE_" + value;
			return List.of(new SimpleGrantedAuthority(role));
		}
		return List.of();
	}
}
