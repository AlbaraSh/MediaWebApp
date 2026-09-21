package com.mediawebapp.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native-SQL access to {@code media_embeddings}. Vector columns are never mapped
 * through JPA — insert, read, and cosine search all go through PostgreSQL.
 */
@Repository
@RequiredArgsConstructor
public class MediaEmbeddingRepository {

	private static final String UPSERT = """
			INSERT INTO media_embeddings (media_id, embedding)
			VALUES (:mediaId, CAST(:embedding AS vector))
			ON CONFLICT (media_id) DO UPDATE SET embedding = CAST(:embedding AS vector)
			""";

	private static final String FIND_EMBEDDING = """
			SELECT embedding::text AS embedding
			FROM media_embeddings
			WHERE media_id = :mediaId
			""";

	private static final String FIND_EMBEDDINGS = """
			SELECT media_id, embedding::text AS embedding
			FROM media_embeddings
			WHERE media_id IN (:mediaIds)
			""";

	private static final String FIND_SIMILAR = """
			SELECT m.id AS media_id,
			       m.title AS title,
			       mt.name AS media_type,
			       m.release_year AS release_year,
			       m.external_rating AS external_rating,
			       m.external_rating_count AS external_rating_count,
			       m.poster_url AS poster_url,
			       1 - (me.embedding <=> CAST(:embedding AS vector)) AS similarity
			FROM media_embeddings me
			JOIN media m ON m.id = me.media_id
			JOIN media_types mt ON mt.id = m.media_type_id
			WHERE m.id <> :mediaId
			ORDER BY me.embedding <=> CAST(:embedding AS vector)
			LIMIT :limit
			""";

	private static final String FIND_SIMILAR_BY_TYPE = """
			SELECT m.id AS media_id,
			       m.title AS title,
			       mt.name AS media_type,
			       m.release_year AS release_year,
			       m.external_rating AS external_rating,
			       m.external_rating_count AS external_rating_count,
			       m.poster_url AS poster_url,
			       1 - (me.embedding <=> CAST(:embedding AS vector)) AS similarity
			FROM media_embeddings me
			JOIN media m ON m.id = me.media_id
			JOIN media_types mt ON mt.id = m.media_type_id
			WHERE mt.name = :typeName
			  AND NOT EXISTS (
			      SELECT 1 FROM user_media um
			      WHERE um.user_id = :userId AND um.media_id = m.id
			  )
			ORDER BY me.embedding <=> CAST(:embedding AS vector)
			LIMIT :limit
			""";

	private static final String FIND_TOP_RATED_BY_TYPE = """
			SELECT m.id AS media_id,
			       m.title AS title,
			       mt.name AS media_type,
			       m.release_year AS release_year,
			       m.external_rating AS external_rating,
			       m.external_rating_count AS external_rating_count,
			       m.poster_url AS poster_url,
			       NULL::float8 AS similarity
			FROM media m
			JOIN media_types mt ON mt.id = m.media_type_id
			WHERE mt.name = :typeName
			  AND NOT EXISTS (
			      SELECT 1 FROM user_media um
			      WHERE um.user_id = :userId AND um.media_id = m.id
			  )
			ORDER BY
			""" + BayesianQualitySql.CASE_ZERO_UNRATED + """
			 DESC, m.id ASC
			LIMIT :limit
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public void upsert(UUID mediaId, float[] embedding) {
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("mediaId", mediaId)
				.addValue("embedding", EmbeddingVectorFormat.toLiteral(embedding));
		jdbcTemplate.update(UPSERT, params);
	}

	public Optional<String> findEmbeddingLiteral(UUID mediaId) {
		MapSqlParameterSource params = new MapSqlParameterSource("mediaId", mediaId);
		List<String> rows = jdbcTemplate.query(
				FIND_EMBEDDING,
				params,
				(rs, rowNum) -> rs.getString("embedding"));
		return rows.stream().findFirst();
	}

	public Map<UUID, float[]> findEmbeddingsByMediaIds(Collection<UUID> mediaIds) {
		if (mediaIds == null || mediaIds.isEmpty()) {
			return Map.of();
		}
		MapSqlParameterSource params = new MapSqlParameterSource("mediaIds", mediaIds);
		List<Map.Entry<UUID, float[]>> rows = jdbcTemplate.query(
				FIND_EMBEDDINGS,
				params,
				(rs, rowNum) -> Map.entry(
						rs.getObject("media_id", UUID.class),
						EmbeddingVectorFormat.fromLiteral(rs.getString("embedding"))));
		Map<UUID, float[]> embeddings = new LinkedHashMap<>();
		for (Map.Entry<UUID, float[]> row : rows) {
			embeddings.put(row.getKey(), row.getValue());
		}
		return embeddings;
	}

	public List<MediaSimilarityRow> findSimilar(String embeddingLiteral, UUID excludeMediaId, int limit) {
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("embedding", embeddingLiteral)
				.addValue("mediaId", excludeMediaId)
				.addValue("limit", limit);
		return jdbcTemplate.query(FIND_SIMILAR, params, this::mapRow);
	}

	public List<MediaSimilarityRow> findSimilarByType(
			String embeddingLiteral,
			String typeName,
			UUID userId,
			int limit) {
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("embedding", embeddingLiteral)
				.addValue("typeName", typeName)
				.addValue("userId", userId)
				.addValue("limit", limit);
		return jdbcTemplate.query(FIND_SIMILAR_BY_TYPE, params, this::mapRow);
	}

	public List<MediaSimilarityRow> findTopRatedByType(
			String typeName,
			UUID userId,
			double globalAverage,
			int bayesianM,
			int limit) {
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("typeName", typeName)
				.addValue("userId", userId)
				.addValue("globalAverage", globalAverage)
				.addValue("m", bayesianM)
				.addValue("limit", limit);
		return jdbcTemplate.query(FIND_TOP_RATED_BY_TYPE, params, this::mapRow);
	}

	private MediaSimilarityRow mapRow(ResultSet rs, int rowNum) throws SQLException {
		Object year = rs.getObject("release_year");
		Object rating = rs.getObject("external_rating");
		Object ratingCount = rs.getObject("external_rating_count");
		Object similarity = rs.getObject("similarity");
		return new MediaSimilarityRow(
				rs.getObject("media_id", UUID.class),
				rs.getString("title"),
				rs.getString("media_type"),
				year == null ? null : ((Number) year).shortValue(),
				rating == null ? null : ((Number) rating).doubleValue(),
				ratingCount == null ? null : ((Number) ratingCount).intValue(),
				rs.getString("poster_url"),
				similarity == null ? null : ((Number) similarity).doubleValue());
	}
}
