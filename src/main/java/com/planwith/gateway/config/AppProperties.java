package com.planwith.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private final Gateway gateway = new Gateway();
	private final Jwt jwt = new Jwt();
	private final Cors cors = new Cors();

	public Gateway getGateway() {
		return gateway;
	}

	public Jwt getJwt() {
		return jwt;
	}

	public Cors getCors() {
		return cors;
	}

	public static class Gateway {
		private String internalToken = "";

		public String getInternalToken() {
			return internalToken;
		}

		public void setInternalToken(String internalToken) {
			this.internalToken = internalToken;
		}
	}

	public static class Jwt {
		private String issuer = "http://localhost:8080";
		private String audience = "planwith-api";

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
	}

	public static class Cors {
		private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
		private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
		private boolean allowCredentials = true;

		public List<String> getAllowedOrigins() {
			return allowedOrigins;
		}

		public void setAllowedOrigins(List<String> allowedOrigins) {
			this.allowedOrigins = allowedOrigins;
		}

		public List<String> getAllowedMethods() {
			return allowedMethods;
		}

		public void setAllowedMethods(List<String> allowedMethods) {
			this.allowedMethods = allowedMethods;
		}

		public List<String> getAllowedHeaders() {
			return allowedHeaders;
		}

		public void setAllowedHeaders(List<String> allowedHeaders) {
			this.allowedHeaders = allowedHeaders;
		}

		public boolean isAllowCredentials() {
			return allowCredentials;
		}

		public void setAllowCredentials(boolean allowCredentials) {
			this.allowCredentials = allowCredentials;
		}
	}
}
