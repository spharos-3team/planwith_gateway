package com.planwith.gateway.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class JwtDecoderConfig {

	@Bean
	ReactiveJwtDecoder reactiveJwtDecoder(GatewayJwtProperties properties) {
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
				.responseTimeout(Duration.ofSeconds(2));
		WebClient webClient = WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();

		NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
				.withJwkSetUri(loopbackJwkSetUri(properties.getJwkSetUri()))
				.webClient(webClient)
				.build();

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(properties.getIssuer());
		OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
				JwtClaimNames.AUD,
				audience -> audience != null && audience.contains(properties.getAudience())
		);

		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
		return decoder;
	}

	private static String loopbackJwkSetUri(String jwkSetUri) {
		if (jwkSetUri == null || jwkSetUri.isBlank()) {
			return "http://127.0.0.1:8082/oauth2/jwks";
		}
		return jwkSetUri.replace("://localhost", "://127.0.0.1");
	}
}
