package com.mediawebapp.controller;

import com.mediawebapp.service.GenreService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Distinct catalog genre names for Discover/Library filter UIs.
 * Small lookup list — not paginated.
 */
@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

	private final GenreService genreService;

	@GetMapping
	public ResponseEntity<List<String>> listCatalogGenres() {
		return ResponseEntity.ok(genreService.listDistinctCatalogNames());
	}
}
