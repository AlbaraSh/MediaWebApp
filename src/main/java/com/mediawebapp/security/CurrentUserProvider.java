package com.mediawebapp.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Abstraction for resolving the authenticated user's id.
 * <p>
 * Controllers call this and pass the UUID into the service layer. Services
 * never resolve identity themselves so business logic stays auth-agnostic.
 * Implemented by {@link JwtCurrentUserProvider}.
 */
public interface CurrentUserProvider {

	/** @throws IllegalStateException when the request has no authenticated user */
	UUID getCurrentUserId();

	/**
	 * Optional identity for public routes that may carry a JWT.
	 * Empty when SecurityContext has no {@code AuthenticatedUser}.
	 */
	Optional<UUID> getCurrentUserIdIfPresent();
}
