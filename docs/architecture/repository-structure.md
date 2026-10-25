# Repository structure

Pastline is a monorepo (see [ADR-001](../adr/ADR-001-monorepo.md)). This note describes the responsibility of every top-level directory and the dependency rules between them. It is updated whenever the structure actually changes.

## Top-level directories

| Directory | Responsibility | Current state |
|-----------|----------------|---------------|
| `backend/` | The production Java 21 / Spring Boot modular monolith, its tests, configuration and Flyway migrations. This is the single shared backend for all clients and exposes a client-neutral `/api/v1` API. | README only. Application generated in Week 2. |
| `frontend/` | The production React + TypeScript web client and its tests. It is one API client of the backend and contains no journal or search business rules. | README only. Application generated in Week 3. |
| `android/` | Reserved for the post-V1 Kotlin Android client, which reuses the same backend API. | README only. No Android/Gradle project during V1. |
| `labs/` | Isolated learning experiments (Week 1 raw HTTP, SDK, embedding and Spring AI labs). Disposable. | README only. |
| `infra/` | Deployment and infrastructure material: Docker image material, local Docker Compose, AWS deployment. | README only. Compose file added in Week 2. |
| `docs/` | Learning notes, ADRs, architecture descriptions, thesis evidence mapping, experiment definitions and results, screenshots. | Phase 0 skeleton. |
| `scripts/` | Repeatable project automation, such as the synthetic corpus generator and experiment runners. | README only. |
| `.github/` | CI workflows. | Not created. Added only when backend build/test commands exist. |

Root files:

- `PASTLINE_MASTER_PROMPT.md`: roadmap, architecture baseline and acceptance criteria. Read-only unless the roadmap itself is being changed.
- `CLAUDE.md`: short persistent instructions for Claude Code.
- `PROGRESS.md`: current checkpoint and the handoff between sessions.
- `README.md`, `.gitignore`, `.editorconfig`, `.env.example`.

## Dependency rules

```text
frontend/  --HTTP /api/v1-->  backend/
android/   --HTTP /api/v1-->  backend/        (post-V1)

backend/   --X-->  labs/      forbidden
frontend/  --X-->  labs/      forbidden
```

- **Production code must not depend on code inside `labs/`.** No Maven or npm dependency, no source path, no shared module. When a lab idea becomes production code, it is rewritten or reviewed into `backend/`, never copied blindly.
- Clients depend on the backend only through its public HTTP API, never through shared business code.
- `backend/` does not depend on React, browser APIs or Android.
- `infra/` and `scripts/` may build or run the applications but contain no business logic.
- `docs/experiments/` holds results produced by committed code. Results are never edited by hand.

## Data rules

- `docs/experiments/corpus-public-v1/` (created later) is the only corpus ever committed.
- The private corpus of real entries never enters Git in any form: fixtures, screenshots with readable text or thesis appendices.
- Secrets live only in a local, ignored `.env` or a cloud secret store.

## Rule for adding files

Configuration files such as `pom.xml`, `package.json`, `docker-compose.yml` and CI workflow YAML appear only in the phase where they become executable and verifiable. Directories that must exist earlier get a short README instead of an empty placeholder.
