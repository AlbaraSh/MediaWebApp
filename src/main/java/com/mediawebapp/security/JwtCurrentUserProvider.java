package com.mediawebapp.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Reads the user the JWT filter already placed on the security context. */
@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

	@Override
	public UUID getCurrentUserId() {
		return getCurrentUserIdIfPresent()
				.orElseThrow(() -> new IllegalStateException("No authenticated user in security context"));
	}

	@Override
	public Optional<UUID> getCurrentUserIdIfPresent() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			return Optional.empty();
		}
		return Optional.of(user.userId());
	}
}
