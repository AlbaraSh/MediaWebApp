package com.mediawebapp.repository;

import com.mediawebapp.entity.UserMedia;
import com.mediawebapp.entity.UserMediaStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for {@link UserMedia} entries.
 * <p>
 * Join-fetch queries load nested {@code media} and {@code mediaType} so the
 * service can build response DTOs without lazy-loading after the transaction.
 */
public interface UserMediaRepository extends JpaRepository<UserMedia, UUID> {

	@Query("""
			SELECT um FROM UserMedia um
			JOIN FETCH um.media m
			JOIN FETCH m.mediaType
			WHERE um.userId = :userId AND m.id = :mediaId
			""")
	Optional<UserMedia> findByUserIdAndMediaIdWithMedia(
			@Param("userId") UUID userId,
			@Param("mediaId") UUID mediaId);

	@Query("""
			SELECT um FROM UserMedia um
			JOIN FETCH um.media m
			JOIN FETCH m.mediaType
			WHERE um.userId = :userId
			""")
	List<UserMedia> findAllByUserIdWithMedia(@Param("userId") UUID userId);

	@Query("""
			SELECT um FROM UserMedia um
			JOIN FETCH um.media m
			JOIN FETCH m.mediaType
			WHERE um.userId = :userId AND um.status = :status
			""")
	List<UserMedia> findAllByUserIdAndStatusWithMedia(
			@Param("userId") UUID userId,
			@Param("status") UserMediaStatus status);

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
