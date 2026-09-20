-- V4: switch media_embeddings from the V1 384-d HNSW stub to OpenAI
-- text-embedding-3-small (1536-d) with an IVFFlat cosine index.
-- updated_at and its trigger are retained for later re-embedding.

DROP INDEX IF EXISTS idx_media_embeddings_hnsw;

ALTER TABLE media_embeddings DROP COLUMN embedding;

ALTER TABLE media_embeddings
  ADD COLUMN embedding vector(1536) NOT NULL;

CREATE INDEX media_embeddings_embedding_idx
ON media_embeddings
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
