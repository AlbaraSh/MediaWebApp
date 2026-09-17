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
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping of {@code media_external_ids} — links catalog media to a provider id.
 * <p>
 * {@code source} is stored UPPERCASE ({@code TMDB}, {@code JIKAN}, {@code RAWG}).
 * Only the import flow writes this table.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
		name = "media_external_ids",
		uniqueConstraints = {
				@UniqueConstraint(columnNames = {"source", "external_id"}),
				@UniqueConstraint(columnNames = {"media_id", "source"})
		}
)
public class MediaExternalId {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "media_id", nullable = false)
	private Media media;

	@Column(nullable = false, length = 50)
	private String source;

	@Column(name = "external_id", nullable = false, length = 255)
	private String externalId;
}
