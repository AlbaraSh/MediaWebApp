package com.mediawebapp.entity;

/**
 * Allowed {@code user_media.status} values — must match the PostgreSQL
 * {@code user_media_status} enum exactly.
 */
public enum UserMediaStatus {
	PLANNED,
	WATCHING,
	COMPLETED,
	DROPPED
}
