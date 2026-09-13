package com.mediawebapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping of {@code user_media} — a user's personal tracking state for one
 * catalog {@link Media} item. Does not duplicate catalog fields; only stores
 * status, optional rating/review, and ownership via {@code userId}.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
		name = "user_media",
		uniqueConstraints = @UniqueConstraint(
				name = "user_media_user_id_media_id_key",
				columnNames = {"user_id", "media_id"}
		)
)
public class UserMedia {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Owner of this list entry; resolved by the controller via CurrentUserProvider. */
	@Column(name = "user_id", nullable = false, updatable = false)
	private UUID userId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "media_id", nullable = false, updatable = false)
	private Media media;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false, columnDefinition = "user_media_status")
	private UserMediaStatus status;

	/** Optional integer rating from 1–10; {@code null} means not rated. */
	@Column
	private Integer rating;

	@Column(columnDefinition = "TEXT")
	private String review;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private Instant updatedAt;
}
