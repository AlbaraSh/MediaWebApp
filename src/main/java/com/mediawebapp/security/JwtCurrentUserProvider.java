package com.mediawebapp.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the current user from the JWT that {@link JwtAuthenticationFilter}
 * already validated and stored in {@code SecurityContext}.
 * <p>
 * Controllers call this and pass {@code userId} into services. Services must
 * not depend on Spring Security.
 */
@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

	@Override
	public UUID getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			throw new IllegalStateException("No authenticated user in security context");
		}
		return user.userId();
	}
}
