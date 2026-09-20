package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.entity.Genre;
import com.mediawebapp.repository.GenreRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

	@Mock
	private GenreRepository genreRepository;

	@Mock
	private PlatformTransactionManager transactionManager;

	private GenreService genreService;

	@BeforeEach
	void setUp() {
		org.mockito.Mockito.lenient()
				.when(transactionManager.getTransaction(any()))
				.thenReturn(new SimpleTransactionStatus());
		genreService = new GenreService(genreRepository, transactionManager);
	}

	@Test
	void canonicalize_titleCasesAndCollapsesWhitespace() {
		assertThat(GenreService.canonicalize("action")).isEqualTo("Action");
		assertThat(GenreService.canonicalize("ACTION")).isEqualTo("Action");
		assertThat(GenreService.canonicalize("  tv   movie ")).isEqualTo("Tv Movie");
		assertThat(GenreService.canonicalize("sci-fi")).isEqualTo("Sci-Fi");
		assertThat(GenreService.canonicalize(null)).isNull();
		assertThat(GenreService.canonicalize("   ")).isNull();
		assertThat(GenreService.canonicalize("")).isNull();
	}

	@Test
	void resolveAll_skipsBlanksAndDedupesCaseVariants() {
		Genre action = persisted("Action");
		when(genreRepository.findByName("Action")).thenReturn(Optional.of(action));

		Set<Genre> resolved = genreService.resolveAll(Arrays.asList("action", "  ", "ACTION", null, " Action "));

		assertThat(resolved).containsExactly(action);
		verify(genreRepository, times(1)).findByName("Action");
		verify(genreRepository, never()).saveAndFlush(any());
	}

	@Test
	void resolveAll_insertsMissingCanonicalName() {
		Genre saved = persisted("Sci-Fi");
		when(genreRepository.findByName("Sci-Fi"))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(saved));
		when(genreRepository.saveAndFlush(any(Genre.class))).thenAnswer(invocation -> {
			Genre genre = invocation.getArgument(0);
			genre.setId(saved.getId());
			return genre;
		});

		Set<Genre> resolved = genreService.resolveAll(List.of("sci-fi"));

		assertThat(resolved).containsExactly(saved);
		verify(genreRepository).saveAndFlush(any(Genre.class));
		verify(genreRepository, times(2)).findByName("Sci-Fi");
	}

	@Test
	void resolveAll_reReadsExistingRowOnUniqueConflict() {
		Genre existing = persisted("Action");
		when(genreRepository.findByName("Action"))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(existing));
		when(genreRepository.saveAndFlush(any(Genre.class)))
				.thenThrow(new DataIntegrityViolationException("genres_name_key"));

		Set<Genre> resolved = genreService.resolveAll(List.of("action"));

		assertThat(resolved).containsExactly(existing);
		verify(genreRepository, times(2)).findByName("Action");
	}

	@Test
	void resolveAll_emptyOrNullListIsValid() {
		assertThat(genreService.resolveAll(null)).isEmpty();
		assertThat(genreService.resolveAll(List.of())).isEmpty();
		verify(genreRepository, never()).findByName(any());
	}

	@Test
	void listDistinctCatalogNames_returnsRepositoryOrder() {
		when(genreRepository.findDistinctNamesUsedByCatalogMedia()).thenReturn(List.of("Action", "Drama"));

		assertThat(genreService.listDistinctCatalogNames()).containsExactly("Action", "Drama");
	}

	@Test
	void listDistinctCatalogNames_emptyCatalog() {
		when(genreRepository.findDistinctNamesUsedByCatalogMedia()).thenReturn(List.of());

		assertThat(genreService.listDistinctCatalogNames()).isEmpty();
	}

	private static Genre persisted(String name) {
		Genre genre = new Genre();
		genre.setId(UUID.randomUUID());
		genre.setName(name);
		return genre;
	}
}
