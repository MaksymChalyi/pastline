CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    timezone      VARCHAR(64)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    -- email зберігається в нижньому регістрі, тому звичайний unique дає нечутливість до регістру
    CONSTRAINT users_email_unique UNIQUE (email)
);
