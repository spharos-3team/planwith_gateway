package com.planwith.gateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * JWT 검증은 {@link com.planwith.gateway.filter.JwtAuthenticationGlobalFilter}에서 수행한다.
 * 이번 단계에서는 모든 exchange를 permitAll로 두어 기존 public API / route 정책을 유지한다.
 *
 * <p>Spring Security가 classpath에 있으면 Gateway {@code globalcors} 대신 Security CORS가 사용되므로
 * 동일한 origin 정책을 여기서 유지한다.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.cors(Customizer.withDefaults())
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
				.logout(ServerHttpSecurity.LogoutSpec::disable)
				.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
				.authorizeExchange(exchange -> exchange.anyExchange().permitAll())
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${CORS_ALLOWED_ORIGIN_LOCAL:http://localhost:3000}") String originLocal,
			@Value("${CORS_ALLOWED_ORIGIN_AWS:http://localhost:3000}") String originAws,
			@Value("${CORS_ALLOWED_ORIGIN_LOCALHOST:http://localhost:8000}") String originLocalhost,
			@Value("${CORS_ALLOWED_ORIGIN_LOOPBACK:http://127.0.0.1:8000}") String originLoopback,
			@Value("${CORS_ALLOWED_ORIGIN_VITE_LOCALHOST:http://localhost:5173}") String viteLocalhost,
			@Value("${CORS_ALLOWED_ORIGIN_VITE_LOCALHOST_5174:http://localhost:5174}") String viteLocalhost5174,
			@Value("${CORS_ALLOWED_ORIGIN_VITE_LOOPBACK:http://127.0.0.1:5173}") String viteLoopback,
			@Value("${CORS_ALLOWED_ORIGIN_VITE_LOOPBACK_5174:http://127.0.0.1:5174}") String viteLoopback5174) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(
				originLocal,
				originAws,
				originLocalhost,
				originLoopback,
				viteLocalhost,
				viteLocalhost5174,
				viteLoopback,
				viteLoopback5174));
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
