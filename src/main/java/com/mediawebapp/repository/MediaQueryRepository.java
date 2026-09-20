package com.mediawebapp.repository;

import com.mediawebapp.dto.DiscoverSort;
import com.mediawebapp.dto.SortDirection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native Discover queries: filter/sort/count/page in PostgreSQL, never in Java.
 */
@Repository
@RequiredArgsConstructor
public class MediaQueryRepository {

	private static final String FROM_WHERE = """
			FROM media m
			JOIN media_types mt ON mt.id = m.media_type_id
			WHERE (CAST(:typeName AS varchar) IS NULL OR mt.name = CAST(:typeName AS varchar))
			  AND (CAST(:year AS smallint) IS NULL OR m.release_year = CAST(:year AS smallint))
			  AND (CAST(:q AS varchar) IS NULL OR m.title ILIKE CAST(:q AS varchar) ESCAPE '\\'
			       OR m.description ILIKE CAST(:q AS varchar) ESCAPE '\\')
			  AND (CAST(:genre AS varchar) IS NULL OR EXISTS (
			      SELECT 1 FROM media_genres mg
			      JOIN genres g ON g.id = mg.genre_id
			      WHERE mg.media_id = m.id AND g.name = CAST(:genre AS varchar)
			  ))
			""";

	private static final String COUNT_SQL = "SELECT COUNT(*) " + FROM_WHERE;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public long countDiscover(String typeName, String genreName, Short year, String q) {
		Long total = jdbcTemplate.queryForObject(COUNT_SQL, bindFilters(typeName, genreName, year, q), Long.class);
		return total == null ? 0L : total;
	}

	public List<UUID> findDiscoverIds(
			String typeName,
			String genreName,
			Short year,
			String q,
			DiscoverSort sort,
			SortDirection direction,
			double globalAverage,
			int bayesianM,
			int limit,
			long offset) {
		String sql = "SELECT m.id " + FROM_WHERE + " ORDER BY " + orderBy(sort, direction)
				+ " LIMIT :limit OFFSET :offset";
		MapSqlParameterSource params = bindFilters(typeName, genreName, year, q)
				.addValue("limit", limit)
				.addValue("offset", offset);
		if (sort == DiscoverSort.QUALITY) {
			params.addValue("globalAverage", globalAverage).addValue("m", bayesianM);
		}
		return jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getObject("id", UUID.class));
	}

	private static MapSqlParameterSource bindFilters(String typeName, String genreName, Short year, String q) {
		return new MapSqlParameterSource()
				.addValue("typeName", typeName)
				.addValue("genre", genreName)
				.addValue("year", year)
				.addValue("q", q == null ? null : SqlLike.contains(q));
	}

	private static String orderBy(DiscoverSort sort, SortDirection direction) {
		String dir = direction.name();
		return switch (sort) {
			case QUALITY -> BayesianQualitySql.CASE_NULL_UNRATED + " " + dir + " NULLS LAST, m.id ASC";
			case YEAR -> "m.release_year " + dir + " NULLS LAST, m.id ASC";
			case TITLE -> "m.title " + dir + ", m.id ASC";
		};
	}
}
