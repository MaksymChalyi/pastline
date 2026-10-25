# Pastline

Pastline is a personal journal and personal-history system. A user stores life records and later retrieves them through chronological browsing, lexical search, semantic search and AI-assisted questions answered only from their own records, with source entries shown for every answer. The core problem is information retrieval over personal records, not a generic chatbot.

**Diploma working title:** «Інтелектуальна система обліку та інтелектуального пошуку персональних записів»

**Status:** pre-V1 / active development. No application exists yet.

**Current phase:** Phase 0 - Repository bootstrap (see [`PROGRESS.md`](PROGRESS.md) for the live state).

## Repository map

```text
pastline/
├── backend/      Java 21 / Spring Boot modular monolith (generated in Week 2)
├── frontend/     React + TypeScript web client (generated in Week 3)
├── android/      reserved for the post-V1 Kotlin client; documentation only during V1
├── labs/         isolated learning experiments; never a production dependency
├── infra/        deployment and infrastructure material
├── docs/         learning notes, ADRs, architecture, thesis evidence, experiments
├── scripts/      repeatable project automation
├── CLAUDE.md     persistent Claude Code instructions
├── PROGRESS.md   current checkpoint and session handoff
└── PASTLINE_MASTER_PROMPT.md   roadmap and architecture source of truth
```

Web V1 talks to one shared Java backend through a versioned, client-neutral `/api/v1` API. The `android/` directory is reserved for a post-V1 Kotlin client that will reuse the same backend rather than a separate mobile backend.

## Setup

Setup and run commands will be added when executable applications exist. There is nothing to build or run yet.

## Roadmap

The full roadmap, architecture baseline and acceptance criteria are in [`PASTLINE_MASTER_PROMPT.md`](PASTLINE_MASTER_PROMPT.md).

## Privacy

Never commit personal diary data or API secrets. Real journal entries must not enter this repository in any form, including test fixtures, screenshots with readable text and thesis appendices. Secrets belong in a local `.env` file (ignored by Git); `.env.example` lists variable names only.
