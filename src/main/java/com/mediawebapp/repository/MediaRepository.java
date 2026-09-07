package com.mediawebapp.repository;

import com.mediawebapp.entity.Media;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MediaRepository extends JpaRepository<Media, UUID> {

	@Query("SELECT m FROM Media m JOIN FETCH m.mediaType")
	List<Media> findAllWithMediaType();

	@Query("SELECT m FROM Media m JOIN FETCH m.mediaType WHERE m.id = :id")
	Optional<Media> findByIdWithMediaType(@Param("id") UUID id);
}
