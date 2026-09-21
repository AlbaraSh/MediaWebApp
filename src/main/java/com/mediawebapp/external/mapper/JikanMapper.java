package com.mediawebapp.external.mapper;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.external.dto.jikan.JikanAnime;
import com.mediawebapp.external.dto.jikan.JikanAnimeDetailsResponse;
import com.mediawebapp.external.dto.jikan.JikanNamedEntry;
import com.mediawebapp.external.dto.jikan.JikanSearchResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class JikanMapper {

	public List<ExternalMediaDTO> toDtos(JikanSearchResponse response) {
		if (response == null || response.data() == null) {
			return List.of();
		}
		return response.data().stream()
				.filter(anime -> anime != null && anime.malId() != null)
				.map(this::toDto)
				.toList();
	}

	public ExternalMediaDTO toDto(JikanAnimeDetailsResponse response) {
		return toDto(response.data());
	}

	public ExternalMediaDTO toDto(JikanAnime anime) {
		return new ExternalMediaDTO(
				blankToNull(anime.title()),
				blankToNull(anime.synopsis()),
				resolveYear(anime),
				"Anime",
				"JIKAN",
				String.valueOf(anime.malId()),
				combinedGenreNames(anime),
				anime.score(),
				anime.scoredBy(),
				posterUrl(anime)
		);
	}

	private static List<String> combinedGenreNames(JikanAnime anime) {
		Set<String> unique = new LinkedHashSet<>();
		addNames(unique, anime.genres());
		addNames(unique, anime.themes());
		addNames(unique, anime.demographics());
		return List.copyOf(unique);
	}

	private static void addNames(Set<String> unique, List<JikanNamedEntry> entries) {
		if (entries == null) {
			return;
		}
		for (JikanNamedEntry entry : entries) {
			if (entry != null && entry.name() != null && !entry.name().isBlank()) {
				unique.add(entry.name().trim());
			}
		}
	}

	private static Short resolveYear(JikanAnime anime) {
		if (anime.year() != null && anime.year() > 0) {
			return anime.year().shortValue();
		}
		if (anime.aired() != null) {
			return parseYear(anime.aired().from());
		}
		return null;
	}

	private static String posterUrl(JikanAnime anime) {
		if (anime.images() == null || anime.images().jpg() == null) {
			return null;
		}
		String large = blankToNull(anime.images().jpg().largeImageUrl());
		if (large != null) {
			return large;
		}
		return blankToNull(anime.images().jpg().imageUrl());
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}

	private static Short parseYear(String date) {
		if (date == null || date.isBlank() || date.length() < 4) {
			return null;
		}
		try {
			return Short.parseShort(date.substring(0, 4));
		} catch (NumberFormatException exception) {
			return null;
		}
	}
}
