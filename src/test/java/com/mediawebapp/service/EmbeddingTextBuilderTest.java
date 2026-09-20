package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingTextBuilderTest {

	@Test
	void build_concatenatesTitleDescriptionAndGenres() {
		String text = EmbeddingTextBuilder.build(
				"Attack on Titan",
				"Humanity fights giants",
				List.of("Action", "Drama", "Fantasy"));

		assertThat(text).isEqualTo("Attack on Titan. Humanity fights giants. Action, Drama, Fantasy");
	}

	@Test
	void build_omitsBlankFields() {
		assertThat(EmbeddingTextBuilder.build("Dune", null, List.of("Sci-Fi")))
				.isEqualTo("Dune. Sci-Fi");
		assertThat(EmbeddingTextBuilder.build("Dune", "  ", List.of()))
				.isEqualTo("Dune");
		assertThat(EmbeddingTextBuilder.build("Dune", "Desert planet", null))
				.isEqualTo("Dune. Desert planet");
	}
}
