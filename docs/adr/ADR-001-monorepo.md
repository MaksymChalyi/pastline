# ADR-001: One monorepo for the complete Pastline product

## Status

Accepted

## Context

Pastline is one product and one diploma project made of several parts that change together:

- a Java 21 / Spring Boot backend, built as a modular monolith;
- a React + TypeScript web frontend, the only V1 client;
- a reserved boundary for a post-V1 Kotlin Android client that must reuse the same backend;
- isolated learning labs from Week 1 (raw Ollama/OpenAI HTTP calls, embeddings, Spring AI);
- infrastructure material for local Docker Compose and later AWS deployment;
- thesis documentation: learning notes, ADRs, experiment definitions and raw results that must be traceable to a specific commit.

There is one developer. A thesis experiment is only reproducible if the code, the dataset, the query set and the results can all be pinned to one commit SHA (Sections 7.5 and 20.14).

## Decision

Keep the backend, the web frontend, the future Android client, the labs, the infrastructure and the documentation in one Git repository, with clear top-level boundaries (`backend/`, `frontend/`, `android/`, `labs/`, `infra/`, `docs/`, `scripts/`).

There is one shared backend for the web client and future mobile clients. Both call the same versioned `/api/v1/...` business API. No separate mobile backend is planned.

## Alternatives considered

1. **Separate repositories for backend, frontend and mobile.** Rejected. An API change would need coordinated pull requests across repositories, and a thesis result could not be pinned to a single commit covering backend, evaluation tooling and experiment data. With one developer, the extra coordination costs time and gives no isolation benefit.
2. **A separate backend for the mobile client.** Rejected. It would duplicate journal, search and AI business logic, the retrieval behaviour measured in the thesis would diverge between clients, and Sections 7.6 and 45.1 require the Android client to reuse the same backend use cases.
3. **Labs in a separate scratch repository.** Rejected. The learning notes in `docs/learning/` reference lab code, and keeping them together preserves the link between what was learned and what was built. Isolation is enforced by a rule instead: production code must not depend on `labs/`.

## Consequences

- One version history and one place for documentation. A thesis freeze can reference one commit SHA or tag.
- Coordinated backend and frontend changes land in a single commit or pull request.
- CI and deployment must scope jobs to the relevant directories (for example, backend jobs run on `backend/**` changes), so an unrelated change does not trigger every pipeline.
- Boundaries are enforced by convention and review, not by repository separation. `labs/` must never become a production dependency, and `android/` must stay documentation-only during V1.
- The repository must stay free of secrets and real journal data, because one leaked file exposes the whole project history.

## Date

2026-10-25
