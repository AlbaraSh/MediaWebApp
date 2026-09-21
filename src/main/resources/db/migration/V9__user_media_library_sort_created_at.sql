-- Library list sort is first-added (created_at DESC), not rating.
DROP INDEX IF EXISTS idx_user_media_library_sort;

CREATE INDEX IF NOT EXISTS idx_user_media_library_sort
	ON user_media (user_id, status, created_at DESC);
