package com.mediawebapp.dto;

/**
 * Result of importing external media into the catalog.
 *
 * @param body    persisted media representation
 * @param created {@code true} when a new row was inserted; {@code false} when
 *                the mapping already existed (idempotent return)
 */
public record ImportMediaResult(
		MediaResponseDTO body,
		boolean created
) {
}
