package com.mediawebapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping of the {@code media} table — one catalog item (movie, show, etc.).
 * <p>
 * Genres live in {@code genres} / {@code media_genres}, not as a column here.
 * External rating fields are the current snapshot only (no history).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "media")
public class Media {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "release_year")
	private Short releaseYear;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "media_type_id", nullable = false)
	private MediaType mediaType;

	@Column(name = "external_rating")
	private Double externalRating;

	@Column(name = "external_rating_count")
	private Integer externalRatingCount;

	@Column(name = "rating_last_updated_at")
	private Instant ratingLastUpdatedAt;

	@Column(name = "poster_url", columnDefinition = "TEXT")
	private String posterUrl;

	@ManyToMany
	@JoinTable(
			name = "media_genres",
			joinColumns = @JoinColumn(name = "media_id"),
			inverseJoinColumns = @JoinColumn(name = "genre_id")
	)
	private Set<Genre> genres = new LinkedHashSet<>();

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private Instant updatedAt;
}
