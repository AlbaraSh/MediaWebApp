package com.mediawebapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.dto.AuthResponseDTO;
import com.mediawebapp.exception.DuplicateResourceException;
import com.mediawebapp.exception.GlobalExceptionHandler;
import com.mediawebapp.exception.InvalidCredentialsException;
import com.mediawebapp.security.CurrentUserProvider;
import com.mediawebapp.security.TestSecurityConfig;
import com.mediawebapp.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private CurrentUserProvider currentUserProvider;

	@Test
	void register_returns201WithEmptyBody() throws Exception {
		String body = """
				{
				  "email": "alice@example.com",
				  "username": "alice",
				  "password": "secret123"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(content().string(""));

		verify(authService).register(any());
	}

	@Test
	void register_returns400WhenPasswordTooShort() throws Exception {
		String body = """
				{
				  "email": "alice@example.com",
				  "username": "alice",
				  "password": "short"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.details.password").exists());

		verify(authService, never()).register(any());
	}

	@Test
	void register_returns400WhenPasswordBlank() throws Exception {
		String body = """
				{
				  "email": "alice@example.com",
				  "username": "alice",
				  "password": "   "
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.password").exists());

		verify(authService, never()).register(any());
	}

	@Test
	void register_returns400WhenEmailInvalid() throws Exception {
		String body = """
				{
				  "email": "not-an-email",
				  "username": "alice",
				  "password": "secret123"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.email").exists());

		verify(authService, never()).register(any());
	}

	@Test
	void register_returns409WhenEmailDuplicate() throws Exception {
		doThrow(new DuplicateResourceException("Email is already registered"))
				.when(authService).register(any());

		String body = """
				{
				  "email": "alice@example.com",
				  "username": "alice",
				  "password": "secret123"
				}
				""";

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Email is already registered"))
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void login_returns200AndToken() throws Exception {
		when(authService.login(any())).thenReturn(new AuthResponseDTO("signed.jwt.token"));

		String body = """
				{
				  "email": "alice@example.com",
				  "password": "secret123"
				}
				""";

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("signed.jwt.token"));
	}

	@Test
	void login_returns401WhenCredentialsInvalid() throws Exception {
		when(authService.login(any()))
				.thenThrow(new InvalidCredentialsException("Invalid email or password"));

		String body = """
				{
				  "email": "alice@example.com",
				  "password": "wrong-password"
				}
				""";

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid email or password"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void login_returns400WhenEmailMissing() throws Exception {
		String body = """
				{
				  "password": "secret123"
				}
				""";

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.email").exists());

		verify(authService, never()).login(any());
	}

	@Test
	void logout_returns204() throws Exception {
		UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		when(currentUserProvider.getCurrentUserId()).thenReturn(userId);

		mockMvc.perform(post("/api/auth/logout"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(authService).logout(userId);
	}
}
