package com.mediawebapp.repository;

import com.mediawebapp.entity.Media;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for {@link Media} entities.
 * <p>
 * Extends Spring Data {@link JpaRepository} for standard CRUD. Custom queries
 * join-fetch {@code mediaType} so the service can map nested media-type DTOs
 * without lazy-loading after the transaction ends.
 */
public interface MediaRepository extends JpaRepository<Media, UUID> {

	/**
	 * Loads all media rows with their media type in a single query.
	 *
	 * @return every media entity with {@code mediaType} initialized
	 */
	@Query("SELECT m FROM Media m JOIN FETCH m.mediaType")
	List<Media> findAllWithMediaType();

	/**
	 * Loads one media row by id, including its media type.
	 *
	 * @param id media primary key
	 * @return the entity if present, otherwise empty
	 */
	@Query("SELECT m FROM Media m JOIN FETCH m.mediaType WHERE m.id = :id")
	Optional<Media> findByIdWithMediaType(@Param("id") UUID id);
}
