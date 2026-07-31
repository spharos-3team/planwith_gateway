package com.planwith.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;

@Configuration
@Profile("!test")
public class JwtDecoderConfig {

	@Bean
	JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
			AppProperties appProperties
	) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(appProperties.getJwt().getIssuer());
		OAuth2TokenValidator<Jwt> withAudience = audienceValidator(appProperties.getJwt().getAudience());
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
		return decoder;
	}

	private static OAuth2TokenValidator<Jwt> audienceValidator(String expectedAudience) {
		return jwt -> {
			List<String> audiences = jwt.getAudience();
			if (audiences != null && audiences.contains(expectedAudience)) {
				return OAuth2TokenValidatorResult.success();
			}
			OAuth2Error error = new OAuth2Error("invalid_token", "Required audience is missing", null);
			return OAuth2TokenValidatorResult.failure(error);
		};
	}
}
