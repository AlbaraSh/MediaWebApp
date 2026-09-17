package com.mediawebapp.external.mapper;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.external.dto.rawg.RawgGame;
import com.mediawebapp.external.dto.rawg.RawgNamedEntry;
import com.mediawebapp.external.dto.rawg.RawgSearchResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RawgMapper {

	public List<ExternalMediaDTO> toDtos(RawgSearchResponse response) {
		if (response == null || response.results() == null) {
			return List.of();
		}
		return response.results().stream()
				.filter(game -> game != null && game.id() != null)
				.map(this::toDto)
				.toList();
	}

	public ExternalMediaDTO toDto(RawgGame game) {
		return new ExternalMediaDTO(
				blankToNull(game.name()),
				blankToNull(game.descriptionRaw()),
				parseYear(game.released()),
				"Game",
				"RAWG",
				String.valueOf(game.id()),
				genreNames(game.genres()),
				RawgRatingConverter.toTenPointScale(game.rating()),
				game.ratingsCount()
		);
	}

	private static List<String> genreNames(List<RawgNamedEntry> genres) {
		if (genres == null || genres.isEmpty()) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		for (RawgNamedEntry genre : genres) {
			if (genre != null && genre.name() != null && !genre.name().isBlank()) {
				names.add(genre.name().trim());
			}
		}
		return List.copyOf(names);
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
