package com.mediawebapp.repository;

import com.mediawebapp.entity.Genre;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GenreRepository extends JpaRepository<Genre, UUID> {

	Optional<Genre> findByName(String name);

	@Query("""
			SELECT DISTINCT g.name FROM Media m
			JOIN m.genres g
			ORDER BY g.name
			""")
	List<String> findDistinctNamesUsedByCatalogMedia();
}
