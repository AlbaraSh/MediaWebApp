package com.mediawebapp.security;

import com.mediawebapp.exception.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Caps POST {@code /api/media/import} per authenticated user.
 */
@RequiredArgsConstructor
public class ImportRateLimitFilter extends OncePerRequestFilter {

	private static final String IMPORT_PATH = "/api/media/import";

	private final ImportRateLimiter importRateLimiter;
	private final JsonMapper jsonMapper;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !(HttpMethod.POST.matches(request.getMethod())
				&& IMPORT_PATH.equals(request.getServletPath()));
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		if (!importRateLimiter.tryAcquire(clientKey(request))) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			jsonMapper.writeValue(
					response.getOutputStream(),
					ApiErrorResponse.of(
							"Too many imports. Try again later.",
							HttpStatus.TOO_MANY_REQUESTS.value()));
			return;
		}
		filterChain.doFilter(request, response);
	}

	private static String clientKey(HttpServletRequest request) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null
				&& authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return "user:" + user.userId();
		}
		return "ip:" + clientIp(request);
	}

	private static String clientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			int comma = forwarded.indexOf(',');
			return (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
		}
		String remote = request.getRemoteAddr();
		return remote == null || remote.isBlank() ? "unknown" : remote;
	}
}
