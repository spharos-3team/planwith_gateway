package com.planwith.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class GatewayJwtProperties {

	/**
	 * Must match Member Service {@code app.jwt.issuer} / JWT {@code iss} claim.
	 */
	private String issuer = "http://localhost:8082";

	/**
	 * Must match Member Service {@code app.jwt.audience} / JWT {@code aud} claim.
	 */
	private String audience = "planwith-api";

	/**
	 * Member JWKS endpoint (e.g. {@code http://localhost:8082/oauth2/jwks}).
	 */
	private String jwkSetUri = "http://localhost:8082/oauth2/jwks";

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getAudience() {
		return audience;
	}

	public void setAudience(String audience) {
		this.audience = audience;
	}

	public String getJwkSetUri() {
		return jwkSetUri;
	}

	public void setJwkSetUri(String jwkSetUri) {
		this.jwkSetUri = jwkSetUri;
	}
}
