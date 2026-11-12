CREATE TABLE journal_entries (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title        VARCHAR(200),
    content      TEXT         NOT NULL,
    entry_date   DATE         NOT NULL,
    content_hash VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    -- той самий текст за ту саму дату в одного користувача лише раз (основа ідемпотентного імпорту)
    CONSTRAINT journal_entries_dedup_unique UNIQUE (user_id, entry_date, content_hash)
);

-- стрічка користувача: нові зверху, фільтр за датами
CREATE INDEX journal_entries_user_date_idx ON journal_entries (user_id, entry_date DESC);
