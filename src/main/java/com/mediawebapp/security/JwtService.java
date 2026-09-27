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

/** HS256 bearer tokens. Claims are user id, email, and {@code ver} (token version). Lifetime is 24 hours. */
@Component
@RequiredArgsConstructor
public class JwtService {

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

	public String generateToken(UUID userId, String email) {
		return generateToken(userId, email, 0);
	}

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

	public AuthenticatedUser parseToken(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(signingKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();

		UUID userId = UUID.fromString(claims.getSubject());
		String email = claims.get("email", String.class);
		Number versionClaim = claims.get("ver", Number.class);
		// Older tokens have no ver claim and match token_version 0.
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
