package com.mediawebapp.repository;

import com.mediawebapp.entity.MediaType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link MediaType} lookup rows.
 * <p>
 * Used by {@code MediaService} to verify that a create request references an
 * existing media type before inserting into {@code media}. Import looks up
 * types by seeded {@code name} and never creates new rows at runtime.
 */
public interface MediaTypeRepository extends JpaRepository<MediaType, UUID> {

	Optional<MediaType> findByName(String name);
}
