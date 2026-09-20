-- V5: IVFFlat with lists=100 and default probes=1 misses true neighbors
-- on small catalogs (integration tests and early production data).
-- HNSW remains accurate with few rows and is a better fit for 1536-d OpenAI embeddings.

DROP INDEX IF EXISTS media_embeddings_embedding_idx;

CREATE INDEX media_embeddings_embedding_idx
ON media_embeddings
USING hnsw (embedding vector_cosine_ops);
