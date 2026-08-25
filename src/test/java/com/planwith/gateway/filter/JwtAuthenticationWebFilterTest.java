package com.planwith.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class JwtAuthenticationWebFilterTest {

	private ReactiveJwtDecoder jwtDecoder;
	private JwtAuthenticationWebFilter filter;

	@BeforeEach
	void setUp() {
		jwtDecoder = mock(ReactiveJwtDecoder.class);
		filter = new JwtAuthenticationWebFilter(jwtDecoder);
	}

	@Test
	void storesJwtOnExchange_andSetsSecurityContext() {
		Jwt jwt = jwtBuilder().subject("member-a").claim("roles", List.of("ROLE_USER")).build();
		when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/members/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
						.build()
		);

		AtomicReference<Object> authentication = new AtomicReference<>();
		WebFilterChain chain = e -> ReactiveSecurityContextHolder.getContext()
				.doOnNext(ctx -> authentication.set(ctx.getAuthentication()))
				.then();

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		assertThat((Jwt) exchange.getAttribute(JwtAuthenticationWebFilter.EXCHANGE_JWT_ATTR)).isEqualTo(jwt);
		assertThat(authentication.get()).isInstanceOf(JwtAuthenticationToken.class);
		assertThat(((JwtAuthenticationToken) authentication.get()).getToken().getSubject()).isEqualTo("member-a");
		verify(jwtDecoder).decode("valid.token");
	}

	@Test
	void continuesWithoutAuthentication_whenJwtInvalid() {
		when(jwtDecoder.decode(anyString())).thenReturn(Mono.error(new JwtException("expired")));

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/api/v1/auth/login")
						.header(HttpHeaders.AUTHORIZATION, "Bearer expired.token")
						.build()
		);

		AtomicBoolean continued = new AtomicBoolean(false);
		AtomicBoolean hasJwtAttr = new AtomicBoolean(true);
		WebFilterChain chain = e -> {
			continued.set(true);
			hasJwtAttr.set(e.getAttribute(JwtAuthenticationWebFilter.EXCHANGE_JWT_ATTR) != null);
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		assertThat(continued).isTrue();
		assertThat(hasJwtAttr).isFalse();
	}

	@Test
	void skipsDecoder_whenBearerMissing() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/auth/login").build()
		);

		WebFilterChain chain = e -> Mono.empty();
		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();
	}

	@Test
	void doesNotTreatDownstreamErrorsAsJwtFailure() {
		Jwt jwt = jwtBuilder().subject("member-a").build();
		when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/members/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
						.build()
		);

		WebFilterChain chain = e -> Mono.error(new IllegalStateException("downstream 503"));
		StepVerifier.create(filter.filter(exchange, chain))
				.expectErrorMessage("downstream 503")
				.verify();
		assertThat((Jwt) exchange.getAttribute(JwtAuthenticationWebFilter.EXCHANGE_JWT_ATTR)).isEqualTo(jwt);
	}

	private static Jwt.Builder jwtBuilder() {
		Instant now = Instant.now();
		return Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60));
	}
}
