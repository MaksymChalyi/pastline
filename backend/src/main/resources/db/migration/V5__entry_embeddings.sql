-- Вектор запису для семантичного пошуку. Рядок відсутній або content_hash не збігається з записом:
-- вектор потрібно (пере)рахувати, це робить EmbeddingIndexer. Окремого статусу PENDING немає.
CREATE TABLE entry_embeddings (
    entry_id     UUID         PRIMARY KEY REFERENCES journal_entries (id) ON DELETE CASCADE,
    -- 768 = розмірність embeddinggemma; інша модель з іншою розмірністю потребує нової міграції
    embedding    vector(768),
    model        VARCHAR(100) NOT NULL,
    content_hash VARCHAR(64)  NOT NULL,
    status       VARCHAR(16)  NOT NULL,
    error        VARCHAR(200),
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT entry_embeddings_status_check CHECK (status IN ('READY', 'FAILED')),
    CONSTRAINT entry_embeddings_ready_has_vector CHECK (status <> 'READY' OR embedding IS NOT NULL)
);
-- Без HNSW: точний пошук серед записів одного користувача (рішення в PLAN.md, розділ 3)
