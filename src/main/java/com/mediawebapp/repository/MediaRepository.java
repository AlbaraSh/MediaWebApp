package com.mediawebapp.repository;

import com.mediawebapp.entity.Media;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MediaRepository extends JpaRepository<Media, UUID> {

	@Query("""
			SELECT DISTINCT m FROM Media m
			JOIN FETCH m.mediaType
			LEFT JOIN FETCH m.genres
			WHERE m.id = :id
			""")
	Optional<Media> findByIdWithMediaType(@Param("id") UUID id);

	@Query("""
			SELECT m.id FROM Media m
			WHERE (m.ratingLastUpdatedAt IS NULL OR m.ratingLastUpdatedAt < :cutoff)
			AND EXISTS (SELECT 1 FROM MediaExternalId e WHERE e.media.id = m.id)
			""")
	List<UUID> findIdsWithStaleRatings(@Param("cutoff") Instant cutoff);

	@Query("SELECT AVG(m.externalRating) FROM Media m WHERE m.externalRating IS NOT NULL")
	Double findAverageExternalRating();

	@Query("""
			SELECT DISTINCT m FROM Media m
			JOIN FETCH m.mediaType
			LEFT JOIN FETCH m.genres
			WHERE m.id IN :ids
			""")
	List<Media> findAllWithMediaTypeAndGenresByIdIn(@Param("ids") Collection<UUID> ids);

	@Query("""
			SELECT DISTINCT m FROM Media m
			LEFT JOIN FETCH m.genres
			WHERE m.id IN :ids
			""")
	List<Media> findAllWithGenresByIdIn(@Param("ids") Collection<UUID> ids);
}
