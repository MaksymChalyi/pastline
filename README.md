# Pastline

Pastline is a personal journal with search over your own entries: lexical, semantic and hybrid search, plus Ask Pastline answers that cite the source entries.

**Diploma topic (working title):** «Інтелектуальна система обліку та інтелектуального пошуку персональних записів»

**Status:** in development. The application does not exist yet; learning labs are in progress.

## Tech stack

- Backend: Java 21, Spring Boot (modular monolith), Spring Security (server session), Spring AI
- Database: PostgreSQL + pgvector, Flyway
- AI: Ollama (local models); OpenAI embeddings only for the online demo
- Frontend: React + TypeScript (Vite)
- Tests: JUnit, Testcontainers
- Infrastructure: Docker Compose; demo deployment on Vercel + Render

## Documents

- [`PLAN.md`](PLAN.md): plan, architecture, thesis structure, decision log, progress
- [`LABS.md`](LABS.md): learning labs 01–05

## Privacy

Real journal entries never enter this repository: not in code, test fixtures, screenshots or thesis appendices. All data in the repository is synthetic. Secrets live in a local `.env` (ignored by Git); `.env.example` lists variable names only.
