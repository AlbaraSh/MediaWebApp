ALTER TABLE media
  ADD COLUMN external_rating DOUBLE PRECISION,
  ADD COLUMN external_rating_count INTEGER,
  ADD COLUMN rating_last_updated_at TIMESTAMPTZ;

ALTER TABLE media
  ADD CONSTRAINT chk_media_external_rating
    CHECK (external_rating IS NULL OR (external_rating >= 0 AND external_rating <= 10));

ALTER TABLE media
  ADD CONSTRAINT chk_media_external_rating_count
    CHECK (external_rating_count IS NULL OR external_rating_count >= 0);
