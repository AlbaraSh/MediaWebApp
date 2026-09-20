package com.mediawebapp.controller;

import com.mediawebapp.dto.AuthResponseDTO;
import com.mediawebapp.dto.LoginRequestDTO;
import com.mediawebapp.dto.RegisterRequestDTO;
import com.mediawebapp.security.CurrentUserProvider;
import com.mediawebapp.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints. Identity for the rest of the API comes from
 * the JWT returned here, not from a client-supplied user id.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final CurrentUserProvider currentUserProvider;

	/**
	 * Registers a new account.
	 *
	 * @param request email, unique username (display name), and password
	 * @return {@code 201 Created} with an empty body
	 */
	@PostMapping("/register")
	public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {
		authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	/**
	 * Authenticates with email + password and returns a JWT.
	 *
	 * @param request login credentials
	 * @return {@code 200 OK} with {@link AuthResponseDTO}
	 */
	@PostMapping("/login")
	public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
		return ResponseEntity.ok(authService.login(request));
	}

	/**
	 * Invalidates the caller's current JWT (and any other tokens for that account).
	 *
	 * @return {@code 204 No Content}
	 */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		authService.logout(currentUserProvider.getCurrentUserId());
		return ResponseEntity.noContent().build();
	}
}
