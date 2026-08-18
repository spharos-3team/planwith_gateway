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
			@Value("${CORS_ALLOWED_ORIGIN_LOCAL:http://192.168.10.167:8000}") String originLocal,
			@Value("${CORS_ALLOWED_ORIGIN_AWS:http://54.116.113.176:8000}") String originAws,
			@Value("${CORS_ALLOWED_ORIGIN_LOCALHOST:http://localhost:8000}") String originLocalhost,
			@Value("${CORS_ALLOWED_ORIGIN_LOOPBACK:http://127.0.0.1:8000}") String originLoopback
	) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(originLocal, originAws, originLocalhost, originLoopback));
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
