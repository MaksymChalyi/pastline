-- Лексичний пошук: PostgreSQL сам перераховує вектор при кожній зміні запису.
-- Конфігурація 'simple' без стемінгу: українського стемера в PostgreSQL немає,
-- тому "пробіжка" і "пробіжку" лексично різні слова (це обмеження міряє експеримент).
ALTER TABLE journal_entries
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (to_tsvector('simple', coalesce(title, '') || ' ' || content)) STORED;

CREATE INDEX journal_entries_search_idx ON journal_entries USING GIN (search_vector);
