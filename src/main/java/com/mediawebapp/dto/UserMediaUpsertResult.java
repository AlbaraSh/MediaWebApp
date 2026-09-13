package com.mediawebapp.dto;

/**
 * Result of an upsert on a user's media list entry.
 *
 * @param body    response DTO for the created or updated entry
 * @param created {@code true} when a new row was inserted; {@code false} on update
 */
public record UserMediaUpsertResult(
		UserMediaResponseDTO body,
		boolean created
) {
}
