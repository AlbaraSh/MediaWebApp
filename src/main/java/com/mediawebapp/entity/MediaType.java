package com.mediawebapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping of the {@code media_types} lookup table.
 * <p>
 * Seeded values (Movie, TV Show, Anime, Game) classify each {@link Media} row.
 * Exposed to API clients only through {@code MediaTypeDTO}, never as a raw entity.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "media_types")
public class MediaType {

	/** Primary key for the media type. */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** Unique human-readable type name (e.g. {@code Movie}). */
	@Column(nullable = false, unique = true, length = 50)
	private String name;
}
