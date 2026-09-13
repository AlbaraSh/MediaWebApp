package com.mediawebapp.security;

import java.util.UUID;

/**
 * Abstraction for resolving the authenticated user's id.
 * <p>
 * Controllers call this and pass the UUID into the service layer. Services
 * never resolve identity themselves so business logic stays auth-agnostic.
 * Implemented by {@link JwtCurrentUserProvider}.
 */
public interface CurrentUserProvider {

	/**
	 * @return the current user's primary key
	 */
	UUID getCurrentUserId();
}
