package com.mediawebapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Creates and verifies JWTs used as the API's bearer tokens.
 * <p>
 * Tokens carry {@code userId} (subject + claim) and {@code email}, are signed
 * with HS256, and expire after 24 hours. The signing secret is injected from
 * {@link JwtProperties} ({@code jwt.secret}).
 */
@Component
@RequiredArgsConstructor
public class JwtService {

	/** Token lifetime required by the auth spec. */
	private static final Duration TOKEN_TTL = Duration.ofHours(24);

	/** HS256 needs at least 256 bits (32 bytes) of key material. */
	private static final int MIN_SECRET_BYTES = 32;

	private final JwtProperties jwtProperties;

	@PostConstruct
	void validateSecret() {
		byte[] secretBytes = secretBytes();
		if (secretBytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"jwt.secret must be at least " + MIN_SECRET_BYTES
							+ " characters so HS256 has a 256-bit key");
		}
	}

	/**
	 * Builds a signed token for the given user.
	 *
	 * @param userId account primary key (UUID, same type as {@code users.id})
	 * @param email  included as a claim so callers can display identity without a DB hit
	 * @return compact JWT string
	 */
	public String generateToken(UUID userId, String email) {
		return generateToken(userId, email, 0);
	}

	/**
	 * Builds a signed token that is bound to the user's current {@code token_version}.
	 */
	public String generateToken(UUID userId, String email, int tokenVersion) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userId.toString())
				.claim("userId", userId.toString())
				.claim("email", email)
				.claim("ver", tokenVersion)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(TOKEN_TTL)))
				.signWith(signingKey())
				.compact();
	}

	/**
	 * Verifies signature and expiry, then reads identity claims.
	 *
	 * @param token compact JWT from the {@code Authorization} header
	 * @return user id and email from the payload
	 * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
	 */
	public AuthenticatedUser parseToken(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(signingKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();

		UUID userId = UUID.fromString(claims.getSubject());
		String email = claims.get("email", String.class);
		Number versionClaim = claims.get("ver", Number.class);
		int tokenVersion = versionClaim == null ? 0 : versionClaim.intValue();
		return new AuthenticatedUser(userId, email, tokenVersion);
	}

	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(secretBytes());
	}

	private byte[] secretBytes() {
		String secret = jwtProperties.secret();
		if (secret == null) {
			return new byte[0];
		}
		return secret.getBytes(StandardCharsets.UTF_8);
	}
}
