package com.mediawebapp.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralizes HTTP error translation for the REST API.
 * <p>
 * Controllers stay free of try/catch blocks: domain and validation failures
 * bubble up and are converted here into consistent {@link ApiErrorResponse}
 * JSON. This is the single place that decides status codes and public messages
 * for handled exception types.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Maps missing resources (media, media type, etc.) to HTTP 404.
	 *
	 * @param exception not-found signal from the service layer
	 * @return JSON body with {@code error} and {@code status}
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
			ResourceNotFoundException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				exception.getMessage(),
				HttpStatus.NOT_FOUND.value()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	/**
	 * Maps Bean Validation failures on {@code @Valid} request bodies to HTTP 400.
	 * <p>
	 * Collects each invalid field into {@code details} so clients can highlight
	 * specific form errors without parsing a free-text message.
	 *
	 * @param exception Spring validation failure for a request DTO
	 * @return JSON body with overall error plus per-field details
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationErrors(
			MethodArgumentNotValidException exception) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		ApiErrorResponse body = ApiErrorResponse.of(
				"Validation failed",
				HttpStatus.BAD_REQUEST.value(),
				fieldErrors
		);
		return ResponseEntity.badRequest().body(body);
	}

	/**
	 * Maps failed login (unknown email or wrong password) to HTTP 401.
	 */
	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
			InvalidCredentialsException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				exception.getMessage(),
				HttpStatus.UNAUTHORIZED.value()
		);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
	}

	/**
	 * Maps unique-constraint collisions (email/username) to HTTP 409.
	 */
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicateResource(
			DuplicateResourceException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				exception.getMessage(),
				HttpStatus.CONFLICT.value()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

	/**
	 * Maps client-input errors that are not Bean Validation failures to HTTP 400.
	 */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException exception) {
		ApiErrorResponse body = exception.getDetails() == null
				? ApiErrorResponse.of(exception.getMessage(), HttpStatus.BAD_REQUEST.value())
				: ApiErrorResponse.of(
						exception.getMessage(),
						HttpStatus.BAD_REQUEST.value(),
						exception.getDetails());
		return ResponseEntity.badRequest().body(body);
	}

	/**
	 * Maps upstream provider failures to HTTP 503. Messages are adapter-owned
	 * and must never include raw provider payloads or request URIs.
	 */
	@ExceptionHandler(ExternalProviderException.class)
	public ResponseEntity<ApiErrorResponse> handleExternalProvider(
			ExternalProviderException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				exception.getMessage(),
				HttpStatus.SERVICE_UNAVAILABLE.value()
		);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
	}

	/**
	 * Maps missing required query parameters (e.g. {@code type} on search) to HTTP 400.
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingParameter(
			MissingServletRequestParameterException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				"Required parameter '" + exception.getParameterName() + "' is missing",
				HttpStatus.BAD_REQUEST.value()
		);
		return ResponseEntity.badRequest().body(body);
	}
}
