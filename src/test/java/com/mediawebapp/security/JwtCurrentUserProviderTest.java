package com.mediawebapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtCurrentUserProviderTest {

	private final JwtCurrentUserProvider provider = new JwtCurrentUserProvider();
	private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getCurrentUserId_throwsWhenNoUser() {
		SecurityContextHolder.clearContext();

		assertThatThrownBy(provider::getCurrentUserId)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No authenticated user");
	}

	@Test
	void getCurrentUserIdIfPresent_emptyWhenNoUser() {
		SecurityContextHolder.clearContext();

		assertThat(provider.getCurrentUserIdIfPresent()).isEmpty();
	}

	@Test
	void getCurrentUserIdIfPresent_emptyForAnonymousAuthentication() {
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
				"key",
				"anonymousUser",
				List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

		assertThat(provider.getCurrentUserIdIfPresent()).isEmpty();
		assertThatThrownBy(provider::getCurrentUserId)
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void bothMethods_returnIdWhenJwtPrincipalPresent() {
		AuthenticatedUser user = new AuthenticatedUser(userId, "alice@example.com");
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(user, null, List.of()));

		assertThat(provider.getCurrentUserIdIfPresent()).contains(userId);
		assertThat(provider.getCurrentUserId()).isEqualTo(userId);
	}
}
