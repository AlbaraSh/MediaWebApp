package com.mediawebapp.repository;

import com.mediawebapp.dto.LibrarySort;
import com.mediawebapp.dto.SortDirection;
import com.mediawebapp.dto.UserMediaStatusCounts;
import com.mediawebapp.entity.UserMediaStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Native Library queries: filter/sort/count/page in PostgreSQL, never in Java.
 */
@Repository
@RequiredArgsConstructor
public class UserMediaQueryRepository {

	private static final String LIST_FROM_WHERE = """
			FROM user_media um
			JOIN media m ON m.id = um.media_id
			JOIN media_types mt ON mt.id = m.media_type_id
			WHERE um.user_id = :userId
			  AND um.status = CAST(:status AS user_media_status)
			  AND (CAST(:typeName AS varchar) IS NULL OR mt.name = CAST(:typeName AS varchar))
			  AND (CAST(:minRating AS integer) IS NULL OR um.rating >= CAST(:minRating AS integer))
			  AND (CAST(:maxRating AS integer) IS NULL OR um.rating <= CAST(:maxRating AS integer))
			  AND (CAST(:q AS varchar) IS NULL OR m.title ILIKE CAST(:q AS varchar) ESCAPE '\\')
			  AND (CAST(:genre AS varchar) IS NULL OR EXISTS (
			      SELECT 1 FROM media_genres mg
			      JOIN genres g ON g.id = mg.genre_id
			      WHERE mg.media_id = m.id AND g.name = CAST(:genre AS varchar)
			  ))
			""";

	private static final String COUNT_SQL = "SELECT COUNT(*) " + LIST_FROM_WHERE;

	private static final String PAGE_SQL = "SELECT um.id " + LIST_FROM_WHERE
			+ " ORDER BY %s"
			+ " LIMIT :limit OFFSET :offset";

	private static final String SHELF_GENRES_SQL = """
			SELECT DISTINCT g.name
			FROM user_media um
			JOIN media_genres mg ON mg.media_id = um.media_id
			JOIN genres g ON g.id = mg.genre_id
			WHERE um.user_id = :userId
			ORDER BY g.name
			""";

	private static final String COUNTS_SQL = """
			SELECT CAST(um.status AS varchar) AS status, COUNT(*) AS total
			FROM user_media um
			JOIN media m ON m.id = um.media_id
			JOIN media_types mt ON mt.id = m.media_type_id
			WHERE um.user_id = :userId
			  AND (CAST(:typeName AS varchar) IS NULL OR mt.name = CAST(:typeName AS varchar))
			  AND (CAST(:q AS varchar) IS NULL OR m.title ILIKE CAST(:q AS varchar) ESCAPE '\\')
			  AND (CAST(:genre AS varchar) IS NULL OR EXISTS (
			      SELECT 1 FROM media_genres mg
			      JOIN genres g ON g.id = mg.genre_id
			      WHERE mg.media_id = m.id AND g.name = CAST(:genre AS varchar)
			  ))
			GROUP BY um.status
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public long countLibrary(
			UUID userId,
			UserMediaStatus status,
			String typeName,
			String genreName,
			Integer minRating,
			Integer maxRating,
			String q) {
		Long total = jdbcTemplate.queryForObject(
				COUNT_SQL,
				bindListFilters(userId, status, typeName, genreName, minRating, maxRating, q),
				Long.class);
		return total == null ? 0L : total;
	}

	public List<UUID> findLibraryIds(
			UUID userId,
			UserMediaStatus status,
			String typeName,
			String genreName,
			Integer minRating,
			Integer maxRating,
			String q,
			LibrarySort sort,
			SortDirection direction,
			int limit,
			long offset) {
		MapSqlParameterSource params = bindListFilters(
				userId, status, typeName, genreName, minRating, maxRating, q)
				.addValue("limit", limit)
				.addValue("offset", offset);
		String sql = PAGE_SQL.formatted(orderBy(sort, direction));
		return jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getObject("id", UUID.class));
	}

	public List<String> findShelfGenreNames(UUID userId) {
		return jdbcTemplate.query(
				SHELF_GENRES_SQL,
				new MapSqlParameterSource("userId", userId),
				(rs, rowNum) -> rs.getString("name"));
	}

	public UserMediaStatusCounts countByStatus(
			UUID userId,
			String typeName,
			String genreName,
			String q) {
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("userId", userId)
				.addValue("typeName", typeName)
				.addValue("genre", genreName)
				.addValue("q", q == null ? null : SqlLike.contains(q));
		Map<UserMediaStatus, Long> totals = new EnumMap<>(UserMediaStatus.class);
		for (UserMediaStatus status : UserMediaStatus.values()) {
			totals.put(status, 0L);
		}
		jdbcTemplate.query(COUNTS_SQL, params, (RowCallbackHandler) rs -> {
			UserMediaStatus status = UserMediaStatus.valueOf(rs.getString("status"));
			totals.put(status, rs.getLong("total"));
		});
		return new UserMediaStatusCounts(
				totals.get(UserMediaStatus.PLANNED),
				totals.get(UserMediaStatus.WATCHING),
				totals.get(UserMediaStatus.COMPLETED),
				totals.get(UserMediaStatus.DROPPED));
	}

	private static MapSqlParameterSource bindListFilters(
			UUID userId,
			UserMediaStatus status,
			String typeName,
			String genreName,
			Integer minRating,
			Integer maxRating,
			String q) {
		return new MapSqlParameterSource()
				.addValue("userId", userId)
				.addValue("status", status.name())
				.addValue("typeName", typeName)
				.addValue("genre", genreName)
				.addValue("minRating", minRating)
				.addValue("maxRating", maxRating)
				.addValue("q", q == null ? null : SqlLike.contains(q));
	}

	private static String orderBy(LibrarySort sort, SortDirection direction) {
		String dir = direction.name();
		return switch (sort) {
			case ADDED -> "um.created_at " + dir + ", m.id ASC";
			case RATING -> "um.rating " + dir + " NULLS LAST, um.created_at DESC, m.id ASC";
		};
	}
}
