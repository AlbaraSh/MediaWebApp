package com.mediawebapp.service;

import com.mediawebapp.dto.AuthResponseDTO;
import com.mediawebapp.dto.LoginRequestDTO;
import com.mediawebapp.dto.RegisterRequestDTO;
import com.mediawebapp.entity.User;
import com.mediawebapp.exception.DuplicateResourceException;
import com.mediawebapp.exception.InvalidCredentialsException;
import com.mediawebapp.repository.UserRepository;
import com.mediawebapp.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login. Issues JWTs via {@link JwtService}; does not read
 * {@code SecurityContext} — that stays in the web/security layer.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	/**
	 * Creates a new user with a BCrypt-hashed password.
	 *
	 * @param request validated register payload
	 * @throws DuplicateResourceException if email or username is already taken
	 */
	@Transactional
	public void register(RegisterRequestDTO request) {
		String email = request.email().trim().toLowerCase();
		String username = request.username().trim();

		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new DuplicateResourceException("Email is already registered");
		}
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw new DuplicateResourceException("Username is already taken");
		}

		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		// Store only the hash — the raw password never leaves this method.
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		userRepository.save(user);
	}

	/**
	 * Validates credentials and returns a signed JWT for subsequent requests.
	 *
	 * @param request validated login payload
	 * @return token wrapper for the client
	 * @throws InvalidCredentialsException if the email is unknown or the password does not match
	 */
	@Transactional(readOnly = true)
	public AuthResponseDTO login(LoginRequestDTO request) {
		String email = request.email().trim().toLowerCase();
		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException("Invalid email or password");
		}

		String token = jwtService.generateToken(user.getId(), user.getEmail());
		return new AuthResponseDTO(token);
	}
}
