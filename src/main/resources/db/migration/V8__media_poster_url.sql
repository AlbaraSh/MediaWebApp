-- Nullable catalog poster URL. Existing rows and failed/missing provider images stay NULL.
ALTER TABLE media ADD COLUMN poster_url TEXT;
