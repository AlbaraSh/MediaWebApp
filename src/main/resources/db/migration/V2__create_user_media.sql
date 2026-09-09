-- V2: user-media tracking (user-specific state over the global media catalog)

--------------------------------------------------
-- Status enum (exact DB values)
--------------------------------------------------

CREATE TYPE user_media_status AS ENUM (
  'PLANNED',
  'WATCHING',
  'COMPLETED',
  'DROPPED'
);

--------------------------------------------------
-- user_media
--------------------------------------------------

CREATE TABLE user_media (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  media_id    UUID NOT NULL REFERENCES media (id) ON DELETE CASCADE,
  status      user_media_status NOT NULL,
  rating      INTEGER
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
-- Dev user for CurrentUserProvider (no auth yet)
-- UUID must match DevCurrentUserProvider.DEV_USER_ID
--------------------------------------------------

INSERT INTO users (id, username, email, password_hash)
VALUES (
  '00000000-0000-0000-0000-000000000001',
  'devuser',
  'dev@mediawebapp.local',
  'dev-placeholder-hash'
)
ON CONFLICT (email) DO NOTHING;
