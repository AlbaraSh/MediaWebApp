-- Additive indexes for Discover (year/genre) and Library (status + rating sort).
-- Title search stays btree-unfriendly (ILIKE '%q%'); no pg_trgm / FTS.

CREATE INDEX IF NOT EXISTS idx_media_release_year ON media (release_year);

CREATE INDEX IF NOT EXISTS idx_media_genres_genre_id ON media_genres (genre_id);

CREATE INDEX IF NOT EXISTS idx_user_media_library_sort
	ON user_media (user_id, status, rating DESC NULLS LAST, updated_at DESC);
