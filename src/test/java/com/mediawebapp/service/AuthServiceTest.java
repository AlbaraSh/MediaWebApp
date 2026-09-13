package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.dto.AuthResponseDTO;
import com.mediawebapp.dto.LoginRequestDTO;
import com.mediawebapp.dto.RegisterRequestDTO;
import com.mediawebapp.entity.User;
import com.mediawebapp.exception.DuplicateResourceException;
import com.mediawebapp.exception.InvalidCredentialsException;
import com.mediawebapp.repository.UserRepository;
import com.mediawebapp.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private JwtService jwtService;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, passwordEncoder, jwtService);
	}

	@Test
	void register_hashesPasswordAndPersistsUser() {
		when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
		when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		authService.register(new RegisterRequestDTO("Alice@example.com", "alice", "secret123"));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		User saved = captor.getValue();

		assertThat(saved.getEmail()).isEqualTo("alice@example.com");
		assertThat(saved.getUsername()).isEqualTo("alice");
		assertThat(saved.getPasswordHash()).isNotEqualTo("secret123");
		assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
	}

	@Test
	void register_throwsWhenEmailAlreadyExists() {
		when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(
				new RegisterRequestDTO("alice@example.com", "alice", "secret123")))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Email is already registered");

		verify(userRepository, never()).save(any());
	}

	@Test
	void register_throwsWhenUsernameAlreadyExists() {
		when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(false);
		when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(
				new RegisterRequestDTO("alice@example.com", "alice", "secret123")))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Username is already taken");

		verify(userRepository, never()).save(any());
	}

	@Test
	void login_returnsTokenWhenCredentialsAreValid() {
		UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		User user = new User();
		user.setId(userId);
		user.setEmail("alice@example.com");
		user.setPasswordHash(passwordEncoder.encode("secret123"));

		when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));
		when(jwtService.generateToken(userId, "alice@example.com")).thenReturn("signed.jwt.token");

		AuthResponseDTO response = authService.login(
				new LoginRequestDTO("Alice@example.com", "secret123"));

		assertThat(response.token()).isEqualTo("signed.jwt.token");
	}

	@Test
	void login_throwsWhenEmailUnknown() {
		when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(
				new LoginRequestDTO("missing@example.com", "secret123")))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");

		verify(jwtService, never()).generateToken(any(), any());
	}

	@Test
	void login_throwsWhenPasswordDoesNotMatch() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("alice@example.com");
		user.setPasswordHash(passwordEncoder.encode("secret123"));

		when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(
				new LoginRequestDTO("alice@example.com", "wrong-password")))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");

		verify(jwtService, never()).generateToken(any(), any());
	}
}
