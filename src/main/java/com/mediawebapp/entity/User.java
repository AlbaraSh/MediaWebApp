package com.mediawebapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping of the existing {@code users} table.
 * <p>
 * Maps to the Flyway-owned schema (UUID primary key, unique username and
 * email, bcrypt hash in {@code password_hash}). This class stays in the
 * persistence layer — auth responses never include the hash or the entity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

	/** Primary key; generated as a UUID to match {@code users.id}. */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Unique display name; not used for login. */
	@Column(nullable = false, length = 50, unique = true)
	private String username;

	/** Unique login identifier. */
	@Column(nullable = false, length = 255, unique = true)
	private String email;

	/** BCrypt hash of the user's password; never returned in API responses. */
	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	/** Set by the database on insert; not written by the application. */
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	/** Maintained by the {@code trg_users_updated_at} trigger; read-only in JPA. */
	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private Instant updatedAt;
}
