package com.mediawebapp.repository;

import com.mediawebapp.entity.MediaType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for {@link MediaType} lookup rows.
 * <p>
 * Used by {@code MediaService} to verify that a create request references an
 * existing media type before inserting into {@code media}.
 */
public interface MediaTypeRepository extends JpaRepository<MediaType, UUID> {
}
