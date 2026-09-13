package com.mediawebapp.security;

import com.mediawebapp.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON 401 for unauthenticated requests (missing/invalid JWT).
 * <p>
 * Spring Security invokes this <em>before</em> controllers, so
 * {@code GlobalExceptionHandler} never sees these failures. The body still
 * uses {@link ApiErrorResponse} so clients always get the same error shape.
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final JsonMapper jsonMapper;

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {

		// Invalid/expired tokens get a specific message from the JWT filter.
		// Missing tokens (and other auth failures) use a generic Unauthorized body
		// so we never leak Spring Security's default wording.
		String message = authException instanceof BadCredentialsException
				&& "Invalid or expired JWT".equals(authException.getMessage())
				? "Invalid or expired JWT"
				: "Unauthorized";
		write(response, HttpStatus.UNAUTHORIZED, message);
	}

	private void write(HttpServletResponse response, HttpStatus status, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(
				response.getOutputStream(),
				ApiErrorResponse.of(message, status.value())
		);
	}
}
