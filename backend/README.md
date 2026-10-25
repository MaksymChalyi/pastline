# backend/

Production boundary for the Pastline backend: one Java 21 / Spring Boot modular monolith.

**Status:** empty boundary. The Spring Boot application is generated in Week 2 (Checkpoint 2.2), not before.

## What belongs here

- the production Spring Boot application and its Maven build;
- backend-specific tests and configuration;
- Flyway migrations.

## What does not belong here

- learning experiments (those live in `labs/`);
- frontend code;
- deployment infrastructure (that lives in `infra/`).

## Boundary rules

- The backend is client-independent: React web is the first client, and a future Kotlin Android client must reuse the same business API under `/api/v1/...`.
- Journal, search, export and AI application services receive an authenticated user identity without depending on browser cookies or future token details.
- Production code must not depend on anything inside `labs/`.

See `PASTLINE_MASTER_PROMPT.md` Sections 7, 13 and 23-27.
