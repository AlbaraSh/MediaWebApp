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

/** Account endpoints. Callers are identified by the JWT, not by a user id in the body. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping("/register")
	public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {
		authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		authService.logout(currentUserProvider.getCurrentUserId());
		return ResponseEntity.noContent().build();
	}
}
