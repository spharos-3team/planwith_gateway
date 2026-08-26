package com.planwith.gateway.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class JwtDecoderConfig {

	private static final int MIN_SECRET_BYTES = 32;

	@Bean
	ReactiveJwtDecoder reactiveJwtDecoder(GatewayJwtProperties properties) {
		NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
				.withSecretKey(createSecretKey(properties.getSecret()))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(properties.getIssuer());
		OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
				JwtClaimNames.AUD,
				audience -> audience != null && audience.contains(properties.getAudience())
		);

		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
		return decoder;
	}

	static SecretKey createSecretKey(String secret) {
		if (!StringUtils.hasText(secret)) {
			throw new IllegalStateException("JWT_SECRET must be configured.");
		}
		byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("JWT_SECRET must be at least 32 bytes.");
		}
		return new SecretKeySpec(secretBytes, "HmacSHA256");
	}
}
