package com.planwith.gateway.filter;

import com.planwith.gateway.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayContextHeaderFilterTest {

	@Test
	@DisplayName("strips client identity headers and injects gateway internal token")
	void injectsInternalTokenAndStripsClientHeaders() throws Exception {
		AppProperties properties = new AppProperties();
		properties.getGateway().setInternalToken("gateway-secret");
		GatewayContextHeaderFilter filter = new GatewayContextHeaderFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Auth-User-Id", "spoofed");
		request.addHeader("X-Gateway-Internal-Token", "client-forged");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		HttpServletRequest forwarded = (HttpServletRequest) chain.getRequest();
		assertThat(forwarded.getHeader("X-Gateway-Internal-Token")).isEqualTo("gateway-secret");
		assertThat(forwarded.getHeader("X-Auth-User-Id")).isNull();
		assertThat(forwarded.getHeader("X-Request-Id")).isNotBlank();
		assertThat(response.getHeader("X-Request-Id")).isNotBlank();
	}

	@Test
	@DisplayName("maps JWT claims to trusted X-Auth headers")
	void mapsJwtClaims() throws Exception {
		AppProperties properties = new AppProperties();
		properties.getGateway().setInternalToken("gateway-secret");
		GatewayContextHeaderFilter filter = new GatewayContextHeaderFilter(properties);

		Jwt jwt = new Jwt(
				"token-value",
				Instant.now(),
				Instant.now().plusSeconds(60),
				Map.of("alg", "RS256"),
				Map.of(
						"sub", "42",
						"roles", List.of("USER"),
						"scope", "profile:read plan:read",
						"session_id", "sid-1"
				)
		);
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
		try {
			MockHttpServletRequest request = new MockHttpServletRequest();
			MockFilterChain chain = new MockFilterChain();
			filter.doFilter(request, new MockHttpServletResponse(), chain);

			HttpServletRequest forwarded = (HttpServletRequest) chain.getRequest();
			assertThat(forwarded.getHeader("X-Auth-User-Id")).isEqualTo("42");
			assertThat(forwarded.getHeader("X-Auth-Roles")).isEqualTo("USER");
			assertThat(forwarded.getHeader("X-Auth-Scopes")).isEqualTo("profile:read,plan:read");
			assertThat(forwarded.getHeader("X-Auth-Session-Id")).isEqualTo("sid-1");
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
}
