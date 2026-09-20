package com.mediawebapp.repository;

import com.mediawebapp.entity.UserMedia;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for {@link UserMedia} entries.
 * <p>
 * Join-fetch queries load nested {@code media}, {@code mediaType}, and genres
 * so the service can build response DTOs without lazy-loading after the transaction.
 */
public interface UserMediaRepository extends JpaRepository<UserMedia, UUID> {

	@Query("""
			SELECT DISTINCT um FROM UserMedia um
			JOIN FETCH um.media m
			JOIN FETCH m.mediaType
			LEFT JOIN FETCH m.genres
			WHERE um.userId = :userId AND m.id = :mediaId
			""")
	Optional<UserMedia> findByUserIdAndMediaIdWithMedia(
			@Param("userId") UUID userId,
			@Param("mediaId") UUID mediaId);

	@Query("""
			SELECT DISTINCT um FROM UserMedia um
			JOIN FETCH um.media m
			JOIN FETCH m.mediaType
			LEFT JOIN FETCH m.genres
			WHERE um.id IN :ids
			""")
	List<UserMedia> findAllByIdInWithMedia(@Param("ids") Collection<UUID> ids);

	@Query("""
			SELECT um.media.id FROM UserMedia um
			WHERE um.userId = :userId AND um.media.id IN :mediaIds
			""")
	List<UUID> findMediaIdsByUserIdAndMediaIdIn(
			@Param("userId") UUID userId,
			@Param("mediaIds") Collection<UUID> mediaIds);

	@Query("""
			SELECT DISTINCT um FROM UserMedia um
			JOIN FETCH um.media m
			JOIN FETCH m.mediaType
			LEFT JOIN FETCH m.genres
			WHERE um.userId = :userId AND um.rating >= :minRating
			""")
	List<UserMedia> findAllByUserIdAndRatingGreaterThanEqualWithMedia(
			@Param("userId") UUID userId,
			@Param("minRating") int minRating);

	boolean existsByUserIdAndMedia_Id(UUID userId, UUID mediaId);

	void deleteByUserIdAndMedia_Id(UUID userId, UUID mediaId);
}
