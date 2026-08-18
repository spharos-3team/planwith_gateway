package com.planwith.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class JwtDecoderConfig {

	@Bean
	ReactiveJwtDecoder reactiveJwtDecoder(GatewayJwtProperties properties) {
		NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
				.withJwkSetUri(properties.getJwkSetUri())
				.build();

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(properties.getIssuer());
		OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
				JwtClaimNames.AUD,
				audience -> audience != null && audience.contains(properties.getAudience())
		);

		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
		return decoder;
	}
}
