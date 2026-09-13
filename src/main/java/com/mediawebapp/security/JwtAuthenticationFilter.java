package com.mediawebapp.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <jwt>}, validates it, and puts the user
 * into {@code SecurityContext} so later filters and {@link JwtCurrentUserProvider}
 * can see who is calling.
 * <p>
 * Missing tokens are ignored here — {@code SecurityFilterChain} then returns 401
 * for protected routes. Invalid/expired tokens fail immediately via the
 * {@link AuthenticationEntryPoint} so the JSON body matches the rest of the API.
 * <p>
 * Registered only on the Spring Security filter chain (not as a servlet filter)
 * to avoid running twice.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = header.substring(BEARER_PREFIX.length()).trim();
		try {
			AuthenticatedUser user = jwtService.parseToken(token);
			// Empty authorities: this iteration has no roles; authenticated() is enough.
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(user, null, List.of());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
			filterChain.doFilter(request, response);
		} catch (JwtException | IllegalArgumentException ex) {
			SecurityContextHolder.clearContext();
			AuthenticationException authEx = new BadCredentialsException("Invalid or expired JWT", ex);
			authenticationEntryPoint.commence(request, response, authEx);
		}
	}
}
