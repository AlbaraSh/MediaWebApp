-- MediaWebApp PostgreSQL schema
-- Requires PostgreSQL 15+ and pgvector extension

--------------------------------------------------
-- Extensions
--------------------------------------------------

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS vector;

--------------------------------------------------
-- Enum types
--------------------------------------------------

CREATE TYPE user_media_status AS ENUM (
  'PLANNED',
  'WATCHING',
  'COMPLETED',
  'DROPPED'
);

--------------------------------------------------
-- Trigger: auto-update updated_at
--------------------------------------------------

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--------------------------------------------------
-- Core tables
--------------------------------------------------

CREATE TABLE users (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username       VARCHAR(50)  NOT NULL,
  email          VARCHAR(255) NOT NULL UNIQUE,
  password_hash  VARCHAR(255) NOT NULL,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  UNIQUE (username)
);

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE media_types (
  id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE genres (
  id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL UNIQUE
);

--------------------------------------------------
-- Media
--------------------------------------------------

CREATE TABLE media (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title           VARCHAR(500) NOT NULL,
  description     TEXT,
  release_year    SMALLINT CHECK (release_year >= 1800 AND release_year <= 2100),
  media_type_id   UUID NOT NULL REFERENCES media_types (id) ON DELETE RESTRICT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_media_type_id ON media (media_type_id);

CREATE TRIGGER trg_media_updated_at
BEFORE UPDATE ON media
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

--------------------------------------------------
-- Media ↔ Genres
--------------------------------------------------

CREATE TABLE media_genres (
  media_id UUID NOT NULL REFERENCES media (id) ON DELETE CASCADE,
  genre_id UUID NOT NULL REFERENCES genres (id) ON DELETE CASCADE,
  PRIMARY KEY (media_id, genre_id)
);

--------------------------------------------------
-- User ↔ Media
--------------------------------------------------

CREATE TABLE user_media (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  media_id    UUID NOT NULL REFERENCES media (id) ON DELETE CASCADE,
  status      user_media_status NOT NULL,
  rating      NUMERIC(2,1)
               CHECK (rating IS NULL OR (rating >= 1 AND rating <= 10)),
  review      TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, media_id)
);

CREATE INDEX idx_user_media_user_id ON user_media (user_id);
CREATE INDEX idx_user_media_media_id ON user_media (media_id);
CREATE INDEX idx_user_media_user_status ON user_media (user_id, status);

CREATE TRIGGER trg_user_media_updated_at
BEFORE UPDATE ON user_media
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

--------------------------------------------------
-- Embeddings
--------------------------------------------------

CREATE TABLE media_embeddings (
  media_id   UUID PRIMARY KEY REFERENCES media (id) ON DELETE CASCADE,
  embedding  vector(384) NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_embeddings_hnsw
ON media_embeddings
USING hnsw (embedding vector_cosine_ops);

CREATE TRIGGER trg_media_embeddings_updated_at
BEFORE UPDATE ON media_embeddings
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

--------------------------------------------------
-- External IDs
--------------------------------------------------

CREATE TABLE media_external_ids (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  media_id     UUID NOT NULL REFERENCES media (id) ON DELETE CASCADE,
  source       VARCHAR(50)  NOT NULL,
  external_id  VARCHAR(255) NOT NULL,
  UNIQUE (source, external_id),
  UNIQUE (media_id, source)
);

CREATE INDEX idx_media_external_ids_media_id ON media_external_ids (media_id);

--------------------------------------------------
-- Seed data
--------------------------------------------------

INSERT INTO media_types (name) VALUES
  ('Movie'),
  ('TV Show'),
  ('Anime'),
  ('Game')
ON CONFLICT (name) DO NOTHING;