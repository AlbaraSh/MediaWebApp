package com.mediawebapp.service;

import com.mediawebapp.entity.Genre;
import com.mediawebapp.repository.GenreRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shared genre lookup-or-insert used by catalog create and external import.
 * Names are canonicalized to Title Case before every read and write.
 */
@Service
public class GenreService {

	private final GenreRepository genreRepository;
	private final TransactionTemplate requiresNewTransaction;

	public GenreService(GenreRepository genreRepository, PlatformTransactionManager transactionManager) {
		this.genreRepository = genreRepository;
		this.requiresNewTransaction = new TransactionTemplate(transactionManager);
		this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public Set<Genre> resolveAll(List<String> rawNames) {
		Set<Genre> resolved = new LinkedHashSet<>();
		Set<String> seen = new LinkedHashSet<>();
		if (rawNames == null) {
			return resolved;
		}
		for (String rawName : rawNames) {
			String canonical = canonicalize(rawName);
			if (canonical == null || !seen.add(canonical)) {
				continue;
			}
			resolved.add(resolveOne(canonical));
		}
		return resolved;
	}

	/**
	 * Title Case: trim, split on whitespace and hyphens, capitalize each token.
	 * Returns {@code null} for blank input.
	 */
	public static String canonicalize(String rawName) {
		if (rawName == null || rawName.isBlank()) {
			return null;
		}
		String trimmed = rawName.trim().replaceAll("\\s+", " ");
		List<String> words = new ArrayList<>();
		for (String word : trimmed.split(" ")) {
			words.add(titleCaseHyphenated(word));
		}
		String joined = String.join(" ", words);
		return joined.isBlank() ? null : joined;
	}

	private Genre resolveOne(String canonical) {
		return genreRepository.findByName(canonical)
				.orElseGet(() -> insertOrReRead(canonical));
	}

	private Genre insertOrReRead(String canonical) {
		try {
			requiresNewTransaction.execute(status -> {
				Genre genre = new Genre();
				genre.setName(canonical);
				return genreRepository.saveAndFlush(genre);
			});
		} catch (DataIntegrityViolationException exception) {
			return genreRepository.findByName(canonical)
					.orElseThrow(() -> exception);
		}
		return genreRepository.findByName(canonical)
				.orElseThrow(() -> new IllegalStateException(
						"Genre not found after insert: " + canonical));
	}

	private static String titleCaseHyphenated(String word) {
		if (word.isBlank()) {
			return word;
		}
		String[] parts = word.split("-", -1);
		for (int i = 0; i < parts.length; i++) {
			parts[i] = titleCaseToken(parts[i]);
		}
		return String.join("-", parts);
	}

	private static String titleCaseToken(String token) {
		if (token.isEmpty()) {
			return token;
		}
		if (token.length() == 1) {
			return token.toUpperCase(Locale.ROOT);
		}
		return Character.toUpperCase(token.charAt(0))
				+ token.substring(1).toLowerCase(Locale.ROOT);
	}
}
