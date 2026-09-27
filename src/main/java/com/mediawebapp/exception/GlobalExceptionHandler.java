package com.mediawebapp.exception;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Maps domain and validation failures to {@link ApiErrorResponse}. Controllers do not catch these. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
			ResourceNotFoundException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				exception.getMessage(),
				HttpStatus.NOT_FOUND.value()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
	}

	/** Field errors go in {@code details} so forms can highlight inputs. */
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

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
			InvalidCredentialsException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				exception.getMessage(),
				HttpStatus.UNAUTHORIZED.value()
		);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicateResource(
			DuplicateResourceException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				exception.getMessage(),
				HttpStatus.CONFLICT.value()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
	}

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

	/** Upstream failures. The message must not include the provider body or request URL. */
	@ExceptionHandler(ExternalProviderException.class)
	public ResponseEntity<ApiErrorResponse> handleExternalProvider(
			ExternalProviderException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				exception.getMessage(),
				HttpStatus.SERVICE_UNAVAILABLE.value()
		);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingParameter(
			MissingServletRequestParameterException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				"Required parameter '" + exception.getParameterName() + "' is missing",
				HttpStatus.BAD_REQUEST.value()
		);
		return ResponseEntity.badRequest().body(body);
	}

	/** Parser detail stays in the logs. Clients only see a generic malformed-JSON message. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
			HttpMessageNotReadableException ignored) {
		ApiErrorResponse body = ApiErrorResponse.of(
				"Malformed JSON request",
				HttpStatus.BAD_REQUEST.value()
		);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
			MethodArgumentTypeMismatchException exception) {
		ApiErrorResponse body = ApiErrorResponse.of(
				"Invalid value for parameter '" + exception.getName() + "'",
				HttpStatus.BAD_REQUEST.value()
		);
		return ResponseEntity.badRequest().body(body);
	}

	/** Unique violations (SQLState 23505) are 409. Other integrity errors stay 400 and hide the SQL. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
			DataIntegrityViolationException exception) {
		boolean unique = isUniqueViolation(exception);
		HttpStatus status = unique ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
		ApiErrorResponse body = ApiErrorResponse.of(
				unique ? "Resource already exists" : "Request could not be completed",
				status.value()
		);
		return ResponseEntity.status(status).body(body);
	}

	private static boolean isUniqueViolation(Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
