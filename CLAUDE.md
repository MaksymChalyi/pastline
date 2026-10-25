# Pastline Claude Code Instructions

Pastline is a monorepo for one product and one diploma project.

## Sources of truth

- `PASTLINE_MASTER_PROMPT.md` is the roadmap, architecture baseline and acceptance-criteria source of truth.
- `PROGRESS.md` is the current project state and current checkpoint.
- Before significant work, read `PROGRESS.md` and the relevant section of `PASTLINE_MASTER_PROMPT.md`.
- During initial repository bootstrap, read `PASTLINE_MASTER_PROMPT.md` completely.
- Do not modify `PASTLINE_MASTER_PROMPT.md` unless the user explicitly asks to change the roadmap.

- Active plan is `docs/plan/SPRINT_PLAN_4W.md`; it overrides the 12-week schedule in the master prompt. In this sprint you implement code; after each checkpoint explain the key design decisions in 5-10 lines so the user can defend them.

## Repository boundaries

- `backend/` contains production Java/Spring Boot code only.
- `frontend/` contains production React/TypeScript code only.
- `android/` is post-V1. During V1 keep only its README and do not generate an Android project.
- `backend/` must remain client-independent so the future Android client can reuse the same business API.
- `labs/` contains isolated learning experiments and must not be a production dependency.
- `docs/learning/` contains learning notes based on work actually completed.
- `docs/adr/` contains architecture decisions.
- `docs/architecture/` describes the implemented architecture.
- `docs/thesis/` maps implementation evidence to diploma material.
- `docs/experiments/` contains reproducible experiment definitions and results.
- `infra/` contains deployment and infrastructure files.
- `scripts/` contains repeatable project automation.

## Execution rules

- Work only on the first incomplete checkpoint in `PROGRESS.md` unless the user explicitly changes scope.
- Do not implement future roadmap phases early.
- Do not generate the whole application in one step.
- During Week 1 keep experiments in `labs/` and do not put learning code into production `backend/`.
- Explain the minimum theory before a new technical concept is implemented.
- Prefer small, verifiable increments.
- Run relevant tests before declaring a checkpoint complete.
- Update the related documentation and `PROGRESS.md` only from work that was actually completed.
- Do not invent benchmark results, commands, test results, API behavior or documentation claims.
- Inspect existing files before making claims about the codebase.
- Import (4.6, 15A) and answer grounding (18A) are in scope; query routing (17A) is cut by the sprint plan and recorded as a threat to validity.
- Never build an Ask Pastline path that can return text without verified source identifiers.
- Never place real personal journal data in the repository, including test fixtures and thesis appendices.
- When a checkpoint's definition of done fails twice, propose a scope cut from Section 2A.3 instead of proposing more work.
- Record actual hours worked at the end of each week in `PROGRESS.md`.

## Safety and Git

- Never commit API keys, tokens, passwords, `.env` files or real private journal data.
- Never commit or push unless the user explicitly requests it.
- Do not use destructive Git commands without explicit instruction.
- Do not rewrite unrelated code while fixing a focused problem.

## Working mode

The user is the orchestrator, Claude writes the code.

- One checkpoint per session. Present a plan first and wait for approval before editing files.
- A checkpoint is done only when relevant tests pass; show the test output.
- After each checkpoint append 5-10 lines to `docs/thesis/DEFENSE_NOTES.md`: decision, reason, rejected alternative, one likely committee question with a short answer.
- Never commit or push; the user reviews the diff and commits.
- Reject unnecessary complexity: no microservices, Kafka, JWT, CQRS, Lombok, Kubernetes.
