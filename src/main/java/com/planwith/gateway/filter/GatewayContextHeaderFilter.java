package com.planwith.gateway.filter;

import com.planwith.gateway.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * After Gateway JWT validation, inject trusted identity headers for Backend.
 * Client-supplied X-Auth-* / X-Gateway-Internal-Token values are ignored.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class GatewayContextHeaderFilter extends OncePerRequestFilter {

	public static final String HEADER_INTERNAL_TOKEN = "X-Gateway-Internal-Token";
	public static final String HEADER_USER_ID = "X-Auth-User-Id";
	public static final String HEADER_ROLES = "X-Auth-Roles";
	public static final String HEADER_SCOPES = "X-Auth-Scopes";
	public static final String HEADER_SESSION_ID = "X-Auth-Session-Id";
	public static final String HEADER_REQUEST_ID = "X-Request-Id";

	private static final Set<String> STRIPPED_HEADERS = Set.of(
			HEADER_INTERNAL_TOKEN,
			HEADER_USER_ID,
			HEADER_ROLES,
			HEADER_SCOPES,
			HEADER_SESSION_ID
	);

	private final AppProperties appProperties;

	public GatewayContextHeaderFilter(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		Map<String, String> overrides = new LinkedHashMap<>();

		String requestId = request.getHeader(HEADER_REQUEST_ID);
		if (requestId == null || requestId.isBlank()) {
			requestId = UUID.randomUUID().toString();
		}
		overrides.put(HEADER_REQUEST_ID, requestId);
		response.setHeader(HEADER_REQUEST_ID, requestId);

		String internalToken = appProperties.getGateway().getInternalToken();
		if (internalToken != null && !internalToken.isBlank()) {
			overrides.put(HEADER_INTERNAL_TOKEN, internalToken);
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			Jwt jwt = jwtAuthentication.getToken();
			overrides.put(HEADER_USER_ID, jwt.getSubject());
			overrides.put(HEADER_ROLES, joinRoles(jwt.getClaim("roles")));
			overrides.put(HEADER_SCOPES, normalizeScopes(jwt.getClaim("scope")));
			Object sessionId = jwt.getClaim("session_id");
			if (sessionId != null && !sessionId.toString().isBlank()) {
				overrides.put(HEADER_SESSION_ID, sessionId.toString());
			}
		}

		filterChain.doFilter(new HeaderOverrideRequest(request, overrides), response);
	}

	private static String joinRoles(Object rolesClaim) {
		if (rolesClaim instanceof List<?> list) {
			return String.join(",", list.stream().map(String::valueOf).toList());
		}
		if (rolesClaim == null) {
			return "";
		}
		return rolesClaim.toString();
	}

	private static String normalizeScopes(Object scopeClaim) {
		if (scopeClaim == null) {
			return "";
		}
		return scopeClaim.toString().trim().replace(' ', ',');
	}

	private static final class HeaderOverrideRequest extends HttpServletRequestWrapper {

		private final Map<String, String> overrides;

		private HeaderOverrideRequest(HttpServletRequest request, Map<String, String> overrides) {
			super(request);
			this.overrides = overrides;
		}

		@Override
		public String getHeader(String name) {
			String override = overrides.get(canonical(name));
			if (override != null) {
				return override;
			}
			if (isStripped(name)) {
				return null;
			}
			return super.getHeader(name);
		}

		@Override
		public Enumeration<String> getHeaders(String name) {
			String override = overrides.get(canonical(name));
			if (override != null) {
				return Collections.enumeration(List.of(override));
			}
			if (isStripped(name)) {
				return Collections.emptyEnumeration();
			}
			return super.getHeaders(name);
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			Set<String> names = new LinkedHashSet<>();
			Enumeration<String> original = super.getHeaderNames();
			while (original.hasMoreElements()) {
				String name = original.nextElement();
				if (!isStripped(name)) {
					names.add(name);
				}
			}
			names.addAll(overrides.keySet());
			return Collections.enumeration(names);
		}

		private static boolean isStripped(String name) {
			return STRIPPED_HEADERS.contains(canonical(name));
		}

		private static String canonical(String name) {
			if (name == null) {
				return "";
			}
			for (String candidate : STRIPPED_HEADERS) {
				if (candidate.equalsIgnoreCase(name)) {
					return candidate;
				}
			}
			if (HEADER_REQUEST_ID.equalsIgnoreCase(name)) {
				return HEADER_REQUEST_ID;
			}
			return name;
		}
	}
}
