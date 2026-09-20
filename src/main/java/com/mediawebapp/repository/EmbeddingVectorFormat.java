package com.mediawebapp.repository;

/**
 * Formats float vectors as PostgreSQL {@code vector} literals and parses them back.
 * Native SQL is still required for insert/search; this only converts the payload.
 */
public final class EmbeddingVectorFormat {

	private EmbeddingVectorFormat() {
	}

	public static String toLiteral(float[] embedding) {
		StringBuilder builder = new StringBuilder(embedding.length * 8);
		builder.append('[');
		for (int i = 0; i < embedding.length; i++) {
			if (i > 0) {
				builder.append(',');
			}
			builder.append(embedding[i]);
		}
		builder.append(']');
		return builder.toString();
	}

	public static float[] fromLiteral(String literal) {
		if (literal == null || literal.isBlank()) {
			return new float[0];
		}
		String trimmed = literal.trim();
		if (trimmed.length() < 2 || trimmed.charAt(0) != '[' || trimmed.charAt(trimmed.length() - 1) != ']') {
			return new float[0];
		}
		String inner = trimmed.substring(1, trimmed.length() - 1).trim();
		if (inner.isEmpty()) {
			return new float[0];
		}
		String[] parts = inner.split(",");
		float[] values = new float[parts.length];
		for (int i = 0; i < parts.length; i++) {
			values[i] = Float.parseFloat(parts[i].trim());
		}
		return values;
	}
}
