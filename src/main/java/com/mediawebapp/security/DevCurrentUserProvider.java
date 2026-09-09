package com.mediawebapp.security;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Development-only {@link CurrentUserProvider} that always returns a fixed user id.
 * <p>
 * The UUID matches the seed row inserted by Flyway {@code V2__create_user_media.sql}.
 * Replace this bean with a JWT-backed implementation when authentication lands.
 */
@Component
public class DevCurrentUserProvider implements CurrentUserProvider {

	/** Must stay in sync with the seeded {@code users.id} in V2. */
	public static final UUID DEV_USER_ID =
			UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Override
	public UUID getCurrentUserId() {
		return DEV_USER_ID;
	}
}
