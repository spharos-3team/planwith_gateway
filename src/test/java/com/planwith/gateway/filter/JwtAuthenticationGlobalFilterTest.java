package com.planwith.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class JwtAuthenticationGlobalFilterTest {

	private ReactiveJwtDecoder jwtDecoder;
	private JwtAuthenticationGlobalFilter filter;

	@BeforeEach
	void setUp() {
		jwtDecoder = mock(ReactiveJwtDecoder.class);
		filter = new JwtAuthenticationGlobalFilter(jwtDecoder);
	}

	@Test
	void addsAuthUserIdFromSubClaim_andIgnoresClientSpoofedHeader() {
		Jwt jwt = jwtBuilder()
				.subject("member-uuid-from-token")
				.claim("roles", List.of("ROLE_USER"))
				.claim("scope", "read")
				.claim("session_id", "session-1")
				.build();
		when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

		MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/members/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
				.header(JwtAuthenticationGlobalFilter.HEADER_AUTH_USER_ID, "attacker-uuid")
				.header(JwtAuthenticationGlobalFilter.HEADER_MEMBER_UUID_ALIAS, "attacker-uuid")
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
		GatewayFilterChain chain = e -> {
			captured.set(e);
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		HttpHeaders headers = captured.get().getRequest().getHeaders();
		assertThat(headers.getFirst(JwtAuthenticationGlobalFilter.HEADER_AUTH_USER_ID))
				.isEqualTo("member-uuid-from-token");
		assertThat(headers.getFirst(JwtAuthenticationGlobalFilter.HEADER_MEMBER_UUID_ALIAS)).isNull();
		assertThat(headers.getFirst(JwtAuthenticationGlobalFilter.HEADER_AUTH_ROLES)).isEqualTo("ROLE_USER");
		assertThat(headers.getFirst(JwtAuthenticationGlobalFilter.HEADER_AUTH_SCOPES)).isEqualTo("read");
		assertThat(headers.getFirst(JwtAuthenticationGlobalFilter.HEADER_AUTH_SESSION_ID)).isEqualTo("session-1");
		verify(jwtDecoder).decode("valid.token");
	}

	@Test
	void continuesWithoutIdentityHeaders_whenBearerTokenMissing() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
				.header(JwtAuthenticationGlobalFilter.HEADER_AUTH_USER_ID, "spoofed")
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
		GatewayFilterChain chain = e -> {
			captured.set(e);
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		assertThat(captured.get().getRequest().getHeaders()
				.getFirst(JwtAuthenticationGlobalFilter.HEADER_AUTH_USER_ID)).isNull();
	}

	@Test
	void continuesWithoutIdentityHeaders_whenJwtValidationFails() {
		when(jwtDecoder.decode(anyString())).thenReturn(Mono.error(new JwtException("expired")));

		MockServerHttpRequest request = MockServerHttpRequest.get("/api/planwith-fo-schedule/x")
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired.token")
				.header(JwtAuthenticationGlobalFilter.HEADER_AUTH_USER_ID, "spoofed")
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
		GatewayFilterChain chain = e -> {
			captured.set(e);
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		assertThat(captured.get().getRequest().getHeaders()
				.getFirst(JwtAuthenticationGlobalFilter.HEADER_AUTH_USER_ID)).isNull();
	}

	@Test
	void continuesWithoutIdentityHeaders_whenSubClaimMissing() {
		Jwt jwt = jwtBuilder().claim("roles", List.of("ROLE_USER")).build();
		when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

		MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/members/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer no-sub.token")
				.build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
		GatewayFilterChain chain = e -> {
			captured.set(e);
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		assertThat(captured.get().getRequest().getHeaders()
				.getFirst(JwtAuthenticationGlobalFilter.HEADER_AUTH_USER_ID)).isNull();
	}

	private static Jwt.Builder jwtBuilder() {
		Instant now = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60));
	}
}
