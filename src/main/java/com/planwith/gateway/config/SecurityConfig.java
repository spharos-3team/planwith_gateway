package com.planwith.gateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.planwith.gateway.filter.JwtAuthenticationWebFilter;

/**
 * 로그인 필수 여부는 여기서만 정한다. JWT 검증은 {@link JwtAuthenticationWebFilter},
 * downstream 식별 헤더는 {@link com.planwith.gateway.filter.JwtAuthenticationGlobalFilter}.
 *
 * <p>공개(permitAll): 로그인/회원가입/약관/토큰 갱신, CORS preflight, Swagger, BO 로그인.
 * BO {@code /api/admin/**} 는 Member JWT가 아니라 BO 자체 JWT를 쓰므로 Gateway는 통과시키고
 * planwith-bo-management Security가 막는다.
 *
 * <p>그 외 모든 API는 authenticated(). Bearer가 없거나 검증 실패면 401.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	JwtAuthenticationWebFilter jwtAuthenticationWebFilter(ReactiveJwtDecoder jwtDecoder) {
		return new JwtAuthenticationWebFilter(jwtDecoder);
	}

	@Bean
	SecurityWebFilterChain securityWebFilterChain(
			ServerHttpSecurity http,
			JwtAuthenticationWebFilter jwtAuthenticationWebFilter
	) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.cors(Customizer.withDefaults())
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
				.logout(ServerHttpSecurity.LogoutSpec::disable)
				.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
				.addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
				.authorizeExchange(exchange -> exchange
						.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.pathMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/webjars/**",
								"/v3/api-docs/**",
								"/docs/**",
								"/actuator/health",
								"/actuator/health/**",
								"/favicon.ico"
						).permitAll()
						.pathMatchers(
								"/api/v1/auth/**",
								"/api/v1/terms",
								"/api/v1/terms/**"
						).permitAll()
						.pathMatchers(HttpMethod.POST, "/api/v1/members", "/api/v1/members/").permitAll()
						.pathMatchers(HttpMethod.GET, "/api/v1/members/nicknames/availability").permitAll()
						.pathMatchers(HttpMethod.GET, "/api/v1/members/me", "/api/v1/members/me/**").authenticated()
						.pathMatchers(
								HttpMethod.GET,
								"/api/v1/members/*/profile",
								"/api/v1/members/*/profile-image"
						).permitAll()
						.pathMatchers(HttpMethod.GET, "/api/v1/meetings/me", "/api/v1/meetings/me/**").authenticated()
						.pathMatchers(HttpMethod.GET, "/api/v1/meetings", "/api/v1/meetings/*").permitAll()
						.pathMatchers(HttpMethod.GET, "/api/v1/meetings/*/cover-image").permitAll()
						.pathMatchers("/api/admin/**").permitAll()
						.anyExchange().authenticated()
				)
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint((exchange, ex) -> {
							exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
							exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
							return exchange.getResponse().setComplete();
						})
				)
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${CORS_ALLOWED_ORIGIN_LOCALHOST:http://localhost:3000}") String originLocalhost,
			@Value("${CORS_ALLOWED_ORIGIN_LOOPBACK:http://127.0.0.1:3000}") String originLoopback,
			@Value("${CORS_ALLOWED_ORIGIN_GW_LOCALHOST:http://localhost:8000}") String originGwLocalhost,
			@Value("${CORS_ALLOWED_ORIGIN_GW_LOOPBACK:http://127.0.0.1:8000}") String originGwLoopback,
			@Value("${CORS_ALLOWED_ORIGIN_LOCAL:http://localhost:3000}") String originLocal,
			@Value("${CORS_ALLOWED_ORIGIN_VITE_LOCALHOST:http://localhost:5173}") String viteLocalhost,
			@Value("${CORS_ALLOWED_ORIGIN_VITE_LOCALHOST_5174:http://localhost:5174}") String viteLocalhost5174,
			@Value("${CORS_ALLOWED_ORIGIN_VITE_LOOPBACK:http://127.0.0.1:5173}") String viteLoopback,
			@Value("${CORS_ALLOWED_ORIGIN_VITE_LOOPBACK_5174:http://127.0.0.1:5174}") String viteLoopback5174,
			@Value("${CORS_ALLOWED_ORIGIN_BO_VERCEL:https://planwith-bo-fe.vercel.app}") String originBoVercel,
			@Value("${CORS_ALLOWED_ORIGIN_BO_ADMIN:https://admin.planwith.store}") String originBoAdmin,
			@Value("${CORS_ALLOWED_ORIGIN_AWS:https://planwith.store}") String originAws,
			@Value("${CORS_ALLOWED_ORIGIN_FO_VERCEL:https://planwith-fo-fe.vercel.app}") String originFoVercel
	) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(
				originLocalhost,
				originLoopback,
				originGwLocalhost,
				originGwLoopback,
				originLocal,
				viteLocalhost,
				viteLocalhost5174,
				viteLoopback,
				viteLoopback5174,
				originBoVercel,
				originBoAdmin,
				originAws,
				originFoVercel
		));
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.addAllowedHeader("*");
		config.setExposedHeaders(List.of("Location", "Content-Disposition"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
