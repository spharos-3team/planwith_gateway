package com.planwith.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class JwtDecoderConfigTest {

	private static final String SECRET = "test-member-gateway-jwt-secret-at-least-32-bytes";
	private static final String OTHER_SECRET = "different-gateway-jwt-secret-at-least-32-bytes";
	private static final String ISSUER = "http://localhost:8082";
	private static final String AUDIENCE = "planwith-api";

	private final JwtDecoderConfig config = new JwtDecoderConfig();

	@Test
	void acceptsHs256TokenWithMatchingSecretAndClaims() throws Exception {
		ReactiveJwtDecoder decoder = decoder(SECRET);

		var jwt = decoder.decode(hs256Token(SECRET, ISSUER, AUDIENCE)).block();

		assertThat(jwt).isNotNull();
		assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
		assertThat(jwt.getSubject()).isEqualTo("member-uuid");
	}

	@Test
	void rejectsHs256TokenSignedWithDifferentSecret() throws Exception {
		ReactiveJwtDecoder decoder = decoder(SECRET);

		assertThatThrownBy(() -> decoder.decode(hs256Token(OTHER_SECRET, ISSUER, AUDIENCE)).block())
				.isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsRs256Token() throws Exception {
		ReactiveJwtDecoder decoder = decoder(SECRET);

		assertThatThrownBy(() -> decoder.decode(rs256Token()).block())
				.isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsIssuerOrAudienceMismatch() throws Exception {
		ReactiveJwtDecoder decoder = decoder(SECRET);

		assertThatThrownBy(() -> decoder.decode(hs256Token(SECRET, "https://wrong-issuer", AUDIENCE)).block())
				.isInstanceOf(JwtException.class);
		assertThatThrownBy(() -> decoder.decode(hs256Token(SECRET, ISSUER, "wrong-audience")).block())
				.isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsSecretShorterThan32Bytes() {
		assertThatThrownBy(() -> JwtDecoderConfig.createSecretKey("short-secret"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32 bytes");
	}

	private ReactiveJwtDecoder decoder(String secret) {
		GatewayJwtProperties properties = new GatewayJwtProperties();
		properties.setIssuer(ISSUER);
		properties.setAudience(AUDIENCE);
		properties.setSecret(secret);
		return config.reactiveJwtDecoder(properties);
	}

	private String hs256Token(String secret, String issuer, String audience) throws Exception {
		SignedJWT signedJwt = new SignedJWT(
				new JWSHeader(JWSAlgorithm.HS256),
				claims(issuer, audience)
		);
		signedJwt.sign(new MACSigner(
				new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
		));
		return signedJwt.serialize();
	}

	private String rs256Token() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();
		SignedJWT signedJwt = new SignedJWT(
				new JWSHeader(JWSAlgorithm.RS256),
				claims(ISSUER, AUDIENCE)
		);
		signedJwt.sign(new RSASSASigner(keyPair.getPrivate()));
		return signedJwt.serialize();
	}

	private JWTClaimsSet claims(String issuer, String audience) {
		Instant now = Instant.now();
		return new JWTClaimsSet.Builder()
				.issuer(issuer)
				.audience(List.of(audience))
				.subject("member-uuid")
				.issueTime(Date.from(now))
				.notBeforeTime(Date.from(now.minusSeconds(1)))
				.expirationTime(Date.from(now.plusSeconds(300)))
				.build();
	}
}
