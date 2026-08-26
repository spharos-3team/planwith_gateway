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
	 * Must be the same 32-byte-or-longer secret used by Member Service.
	 */
	private String secret;

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

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
}
