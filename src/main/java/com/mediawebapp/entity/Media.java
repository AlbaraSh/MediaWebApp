package com.mediawebapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping of the {@code media} table — one catalog item (movie, show, etc.).
 * <p>
 * This class stays inside the persistence layer. Controllers and external
 * clients never see it directly; the service maps to/from DTOs instead.
 * Timestamps are owned by PostgreSQL defaults and triggers, so Hibernate
 * treats them as read-only ({@code insertable/updatable = false}).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "media")
public class Media {

	/** Primary key; generated as a UUID by Hibernate / the database. */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Display title; required, max 500 characters (matches DB column). */
	@Column(nullable = false, length = 500)
	private String title;

	/** Optional long-form synopsis stored as TEXT. */
	@Column(columnDefinition = "TEXT")
	private String description;

	/** Optional release year ({@code SMALLINT}); validated in the request DTO when present. */
	@Column(name = "release_year")
	private Short releaseYear;

	/**
	 * Required classification (Movie, TV Show, …).
	 * Loaded lazily by default; read queries use join-fetch when the type is needed.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "media_type_id", nullable = false)
	private MediaType mediaType;

	/** Set by the database on insert; not written by the application. */
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	/** Maintained by the {@code trg_media_updated_at} trigger; read-only in JPA. */
	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private Instant updatedAt;
}
