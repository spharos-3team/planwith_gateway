package com.planwith.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
			"/actuator/health",
			"/actuator/health/**",
			"/actuator/info",
			"/oauth2/jwks",
			"/api/v1/auth/signup",
			"/api/v1/auth/login",
			"/api/v1/auth/refresh",
			"/api/v1/auth/logout",
			"/api/v1/auth/email/**",
			"/api/v1/auth/check-email",
			"/api/v1/auth/check-nickname",
			"/api/v1/auth/social-login",
			"/api/v1/auth/social-signup",
			"/api/v1/auth/password/reset",
			"/api/v1/auth/profile-image",
			"/api/v1/terms",
			"/api/v1/terms/**",
			// local/test BE helper (inactive in prod profiles on fo-user-be)
			"/api/v1/dev/**"
	};

	@Bean
	@Order(1)
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());
		configuration.setAllowedMethods(appProperties.getCors().getAllowedMethods());
		configuration.setAllowedHeaders(appProperties.getCors().getAllowedHeaders());
		configuration.setAllowCredentials(appProperties.getCors().isAllowCredentials());
		configuration.setExposedHeaders(List.of("X-Request-Id"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
