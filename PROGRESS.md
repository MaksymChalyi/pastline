# Pastline Progress

Plan: PASTLINE_MASTER_PROMPT.md v2.2 (2026-10-26)
Current step: Етап 0 + Лаба 01 (2026-10-26)
Current branch: main
Last completed date: 2026-10-25

## Key dates (master prompt, section 3)

- Labs done: 2026-10-30
- Gate 1: 2026-11-08 (lexical and semantic work on the 500+ corpus, isolation test green)
- Gate 2: 2026-11-15 (numbers for 3 search modes, Ask Pastline poisoned-answer test green)
- Application ready and deployed: 2026-11-20
- Experiment freeze: 2026-11-25
- Full thesis text: 2026-12-05
- Defense: 2026-12-15
- Pace: ~18-22 h/week, ~3 pages/day of text

## Completed
- [x] Pastline monorepo initialized
- [x] Documentation skeleton created
- [x] ADR-001 monorepo decision recorded
- [x] Phase 0 reviewed manually, committed and pushed (52d0963)
- [x] Switched to plan v2.1; v1.6 and the 4-week sprint plan archived in `docs/archive/`; lab structure `labs/01`–`05` created

## Current
- [ ] Етап 0: installation checks (section 4), `ollama list` shows `qwen3:4b` and `embeddinggemma`
- [ ] Лаба 01: Ollama curl (section 5.1)

## Blockers
- None

## Decisions
- ADR-001: use one monorepo for backend, web frontend, future Android client, labs, infra and docs
- 2026-10-25 (superseded by v2.1): 4-week sprint plan, now `docs/archive/SPRINT_PLAN_4W.md`.
- 2026-10-26: adopt PASTLINE_MASTER_PROMPT.md v2.1 as the only plan. Labs (Етап 1) are written by me; Claude writes the application (Етап 2). v1.6 archived as `docs/archive/PASTLINE_MASTER_PROMPT_v1.6.md`.
- 2026-10-26: plan v2.2. New deadlines: app deployed 2026-11-20, text 2026-12-05, defense 2026-12-15. Ask Pastline is required checkpoint 2.10 with answer evaluation; Markdown import and deployment (Vercel + Render, profile `cloud` with OpenAI embeddings, demo only) are back in scope; thesis has 3 chapters. The experiment stays local on Ollama.

## Hours
- Week of 2026-10-19: 1

## Experiments
- None yet

## Next session
1. Read CLAUDE.md and PROGRESS.md.
2. Finish Етап 0 checks (section 4).
3. Do Лаба 01 (section 5.1): materials → curl chat/embed → measurements → "опитай мене" → README.
