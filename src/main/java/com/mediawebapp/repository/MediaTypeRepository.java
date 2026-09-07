package com.mediawebapp.repository;

import com.mediawebapp.entity.MediaType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaTypeRepository extends JpaRepository<MediaType, UUID> {
}
