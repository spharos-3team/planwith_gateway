package com.planwith.gateway.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"eureka.client.enabled=false",
		"app.jwt.jwk-set-uri=http://127.0.0.1:65534/oauth2/jwks"
})
class GatewaySecurityConfigTest {

	@LocalServerPort
	private int port;

	@MockitoBean
	private ReactiveJwtDecoder jwtDecoder;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		webTestClient = WebTestClient.bindToServer()
				.baseUrl("http://127.0.0.1:" + port)
				.build();
	}

	@Test
	void protectedApiWithoutToken_returns401() {
		webTestClient.get()
				.uri("/api/v1/members/me")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void protectedApiWithInvalidToken_returns401() {
		when(jwtDecoder.decode(anyString())).thenReturn(Mono.error(new JwtException("expired")));

		webTestClient.get()
				.uri("/api/v1/members/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired.token")
				.exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void publicLoginWithoutToken_isNot401() {
		webTestClient.post()
				.uri("/api/v1/auth/login")
				.exchange()
				.expectStatus().value(status -> {
					if (status == HttpStatus.UNAUTHORIZED.value()) {
						throw new AssertionError("login must stay public");
					}
				});
	}

	@Test
	void publicLoginWithInvalidToken_isNot401() {
		when(jwtDecoder.decode(anyString())).thenReturn(Mono.error(new JwtException("expired")));

		webTestClient.post()
				.uri("/api/v1/auth/login")
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired.token")
				.exchange()
				.expectStatus().value(status -> {
					if (status == HttpStatus.UNAUTHORIZED.value()) {
						throw new AssertionError("invalid leftover token must not block public login");
					}
				});
	}

	@Test
	void signupWithoutToken_isNot401() {
		webTestClient.post()
				.uri("/api/v1/members")
				.exchange()
				.expectStatus().value(status -> {
					if (status == HttpStatus.UNAUTHORIZED.value()) {
						throw new AssertionError("signup must stay public");
					}
				});
	}

	@Test
	void swaggerWithoutToken_isNot401() {
		webTestClient.get()
				.uri("/swagger-ui.html")
				.exchange()
				.expectStatus().value(status -> {
					if (status == HttpStatus.UNAUTHORIZED.value()) {
						throw new AssertionError("swagger must stay public");
					}
				});
	}

	@Test
	void protectedApiWithValidJwt_isNot401() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.subject("member-a")
				.claim("roles", List.of("ROLE_USER"))
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

		webTestClient.get()
				.uri("/api/v1/members/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
				.exchange()
				.expectStatus().value(status -> {
					if (status == HttpStatus.UNAUTHORIZED.value()) {
						throw new AssertionError("valid JWT must pass Gateway security");
					}
				});
	}
}
