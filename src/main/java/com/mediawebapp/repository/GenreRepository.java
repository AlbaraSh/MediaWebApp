package com.mediawebapp.repository;

import com.mediawebapp.entity.Genre;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, UUID> {

	Optional<Genre> findByName(String name);
}
