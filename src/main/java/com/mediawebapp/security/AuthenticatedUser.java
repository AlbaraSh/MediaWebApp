package com.mediawebapp.security;

import java.util.UUID;

/**
 * Identity stored on the Spring Security {@code Authentication} after a JWT is validated.
 * <p>
 * {@link JwtCurrentUserProvider} reads {@code userId} from here. This is not a
 * domain entity — just the principal the filter attaches to the request.
 */
public record AuthenticatedUser(UUID userId, String email) {
}
