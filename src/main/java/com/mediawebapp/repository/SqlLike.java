package com.mediawebapp.repository;

/**
 * Builds a contains pattern for {@code ILIKE ... ESCAPE '\'} without treating
 * user {@code %} / {@code _} as wildcards.
 */
public final class SqlLike {

	private SqlLike() {
	}

	public static String contains(String raw) {
		String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}
}
