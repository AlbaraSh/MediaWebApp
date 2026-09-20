-- Invalidate JWTs after logout (and keep deleted users unusable).
ALTER TABLE users
  ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

-- Reviews are a few sentences, not unbounded text.
ALTER TABLE user_media
  ADD CONSTRAINT user_media_review_length_chk
  CHECK (review IS NULL OR char_length(review) <= 2000);
