package com.mediawebapp.repository;

import com.mediawebapp.entity.MediaExternalId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for {@link MediaExternalId} mappings.
 * Used by import for idempotency checks and race-recovery lookups.
 */
public interface MediaExternalIdRepository extends JpaRepository<MediaExternalId, UUID> {

	@Query("""
			SELECT DISTINCT e FROM MediaExternalId e
			JOIN FETCH e.media m
			JOIN FETCH m.mediaType
			LEFT JOIN FETCH m.genres
			WHERE e.source = :source AND e.externalId = :externalId
			""")
	Optional<MediaExternalId> findBySourceAndExternalIdWithMedia(
			@Param("source") String source,
			@Param("externalId") String externalId);

	Optional<MediaExternalId> findFirstByMedia_Id(UUID mediaId);
}
