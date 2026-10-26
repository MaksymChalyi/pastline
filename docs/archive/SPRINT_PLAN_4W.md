# Pastline 4-week sprint plan

Status: Active
Adopted: 2026-10-25
Deadline: 2026-11-22 (thesis ready)
Code freeze and experiment freeze: 2026-11-12
Overrides: the 12-week schedule in `PASTLINE_MASTER_PROMPT.md` Section 11 and the Week 1 learning plan in Section 12. Architecture principles, acceptance criteria and safety rules in the master prompt still apply unless this file cuts them explicitly.

## Why this plan exists

The thesis must be ready by 2026-11-22. The 12-week roadmap ends no earlier than mid-January 2027, so it does not fit. This plan targets the minimum defensible thesis from Section 2A.2 and cuts everything else in a recorded way.

Minimum defensible thesis (Section 2A.2), and where this plan delivers it:

| Floor item | Delivered in |
|---|---|
| Journal CRUD with strict user isolation | Week 1, checkpoints 1.3-1.4 |
| Lexical, semantic and hybrid search | Week 1 (1.7, 1.8), Week 2 (2.1) |
| Import of at least one external format | Week 1, checkpoint 1.5 (JSON + Markdown) |
| Versioned corpus and at least 60 labeled queries | Week 1 (1.6), Week 2 (2.4-2.5) |
| Recall@K, MRR, nDCG@K, p50/p95 latency for three modes | Week 2 (2.6), Week 3 (3.1) |
| Error analysis and threats to validity | Week 3 (3.3, 3.5) |
| Localhost demo that does not fail | Week 2 (2.3), Week 4 (4.2) |

## Working mode

- Required pace: about 25 hours per week, which is roughly double the realistic capacity in Section 2A.1 (10-14 h). This is the main schedule risk. Record actual hours every Sunday in `PROGRESS.md`.
- Claude Code implements the code. After each checkpoint it explains the key design decisions in 5-10 lines so that I can defend them. I must be able to repeat those explanations without notes.
- Hour figures below are planning estimates, not measurements.
- Verify current versions of Spring Boot, Spring AI, the pgvector image and Ollama models from official docs before using them (master prompt baseline: Spring Boot 4.1.1, Spring AI 2.0.1, as of 2026-09-19).

## Week 1 (2026-10-26 .. 2026-11-01): backend core and two search modes

| # | Checkpoint | Done when | Est. h |
|---|---|---|---|
| 1.1 | AI basics evening: Ollama install, `curl` to `/api/chat` and `/api/embed`, cosine similarity by hand on a few vectors; pick one chat and one embedding model | The Day 1 learning note is filled from what I actually ran; I can explain the retrieval vs generation flow and why they are evaluated separately | 3 |
| 1.2 | Spring Boot backend in `backend/`, Docker Compose with pgvector PostgreSQL, Flyway baseline migration, `ddl-auto=validate` | `docker compose up -d` plus backend start works; migration startup test passes on Testcontainers | 3 |
| 1.3 | Session auth: register, login, logout, password hashing, CSRF, authenticated user resolved behind an application boundary | API tests for auth pass | 4 |
| 1.4 | Entry CRUD under `/api/v1/entries`, DTOs, pagination, consistent error shape | Integration test: user A cannot read, update or delete user B's entry by UUID | 4 |
| 1.5 | Import: JSON (Pastline export shape) and Markdown with YAML front matter, dry-run report, content-hash idempotency | Parser unit tests incl. malformed input; re-import of the same file creates zero entries | 3 |
| 1.6 | Synthetic corpus generator in `scripts/` with a fixed seed, Ukrainian and English entries, 500+ entries in `docs/experiments/corpus-public-v1/`, covering the topics in `docs/plan/QUERY_COLLECTION.md` | All 30 external queries were received before this checkpoint started; the same seed regenerates a byte-identical corpus (checksum recorded) | 2 |
| 1.7 | Lexical search: PostgreSQL FTS (`simple` config to start, decision recorded in ADR-005), GIN index, user-scoped query | Lexical results on the imported corpus; known successes and failures noted | 3 |
| 1.8 | Semantic search: Spring AI + Ollama embeddings behind an `EmbeddingModelPort`, `entry_embeddings` table, exact pgvector cosine search with ownership filter in SQL, embedding status and reindex command | Semantic results on the corpus; editing an entry re-embeds it; deleting removes it | 4 |

**Parallel task, start 2026-10-26, deadline before checkpoint 1.6 (target 2026-10-29): external query collection.** Three people receive only the short topic description of the corpus, with no entries, and each writes 10 search queries. The topic description and the message to send are in `docs/plan/QUERY_COLLECTION.md`. These 30 queries reduce query formulation bias, because their authors never saw the data (Section 20.12).

**Gate (Sunday 2026-11-01): lexical and semantic search both return results over the imported public corpus through the backend API.**

ADRs written this week: ADR-002 modular monolith, ADR-005 lexical strategy for Ukrainian, ADR-006 embedding model.

## Week 2 (2026-11-02 .. 2026-11-08): hybrid search, client, evaluation set, benchmark

| # | Checkpoint | Done when | Est. h |
|---|---|---|---|
| 2.1 | Hybrid search: top-N lexical + top-N semantic, Reciprocal Rank Fusion with `k` as a parameter, deduplication | RRF unit tests; ADR-007 records N and `k` | 3 |
| 2.2 | Unified `GET /api/v1/search?q=&mode=lexical|semantic|hybrid`, one `SearchService` contract, debug ranks only in dev mode | One query runs in all three modes through the same API | 2 |
| 2.3 | Minimal React + TypeScript client: login, timeline, entry editor, search page with mode switch | I can log in, write an entry and search it in a browser | 8 |
| 2.4 | Labeling guideline (`docs/experiments/LABELING_GUIDELINE.md`), fixed before any labeling; relevance scale 0/1/2 | Guideline committed before the first label | 1 |
| 2.5 | Query set `docs/experiments/search-query-set-v1.json`: 60-80 queries with categories, at least 20% no-answer. Every query carries a `source` tag: `external-p1`, `external-p2`, `external-p3` for the 30 collected queries (included verbatim, typos kept), `author` for the rest | Every query has a category, a source tag and graded labels; no-answer share is at least 20%; metrics can be reported separately for external and author queries | 6 |
| 2.6 | Benchmark runner (Java): runs every query in every mode, computes Recall@K, MRR, nDCG@K, records latency with warm-up, writes raw JSON per run to `docs/experiments/results/` with commit, model and corpus version | One command produces result files | 5 |

**Gate (Sunday 2026-11-08): the benchmark produces Recall@K, MRR, nDCG@K and latency numbers for all three modes.**

## Week 3 (2026-11-09 .. 2026-11-15): experiment, analysis, freeze

| # | Checkpoint | Done when | Est. h |
|---|---|---|---|
| 3.1 | Final experiment run on the complete query set, repeated latency runs, p50/p95 with sample counts | Raw per-query results committed; nothing edited by hand | 4 |
| 3.2 | Charts generated by a script from raw results: nDCG@10, Recall@10, MRR by mode, quality by query category, latency p50/p95 | Charts regenerate from raw files with one command | 3 |
| 3.3 | Error analysis of at least 10 failures: query, expected, lexical/semantic/hybrid results, reason, remediation | `docs/experiments/ERROR_ANALYSIS.md` | 5 |
| 3.4 | `docs/thesis/THESIS_FREEZE.md` by **2026-11-12**: commit SHA/tag, corpus and query-set checksums, label version, model names and dimensions, RRF parameters, PostgreSQL/pgvector versions, hardware, runner command, run date | Freeze file committed; code freeze in force | 1 |
| 3.5 | `docs/thesis/VALIDITY_THREATS.md`, including every cut below that weakens a claim | Each threat has a mitigation and a remaining limitation | 3 |
| 3.6 | Optional: minimal Ask Pastline (`POST /api/v1/ai/ask`): hybrid retrieval, bounded context, Ollama chat, post-generation validation that drops any source ID not in the retrieved set, refusal when nothing passes | **Only if the Week 1 and Week 2 gates were met on time.** Tests with poisoned model output show an invented source ID never reaches the user | 6 |

After the freeze, any material configuration change means a new experiment version, not regenerated charts.

## Week 4 (2026-11-16 .. 2026-11-22): thesis text and demo

| # | Checkpoint | Done when | Est. h |
|---|---|---|---|
| 4.1 | Thesis text: introduction, analysis of approaches, system design, implementation, evaluation, conclusions, using the evidence map and frozen results | Every number in the text traces to a committed raw result file | 20 |
| 4.2 | Five-minute demo script on localhost with the public corpus: timeline, new entry, lexical exact phrase, paraphrase where lexical fails, semantic, hybrid, chart (plus Ask Pastline only if 3.6 was built) | Demo rehearsed end to end twice without failure | 3 |

## Explicit cut list

Every cut is recorded either as a threat to validity (it weakens a claim the thesis makes) or as future work (it is outside the claim).

| Cut | Master prompt reference | Recorded as |
|---|---|---|
| Week 1 Java labs (raw HttpClient, OpenAI raw HTTP and SDK, provider comparison lab, hand-written embeddings lab) | Section 12, Days 3-7 | Future work; the AI basics evening replaces them as the learning basis |
| OpenAI provider | Sections 4.5, 18.5 | Future work; the port abstraction is kept so a cloud provider can be added. Cost-by-provider is not measured |
| AWS deployment | Sections 34-35 | Future work; localhost Docker Compose demo is acceptable (Section 2A.3, item 2) |
| CI/CD | Section 33 | Future work |
| Observability beyond health and basic timers | Section 19.6, 37 | Future work |
| Query routing, date resolution and period summaries | Section 17A | Threat to validity and future work: temporal, aggregation and absence questions (Section 4.7 types 3-5) are not answered by a dedicated route; H5 and H7 are not tested |
| Tags, mood, location | Sections 4.2, 24 | Future work |
| HNSW / IVFFlat comparison | Section 16.6 | Future work; exact search only, stated as a configuration of the experiment |
| Private corpus (Corpus A) | Section 20.3 | Threat to validity (corpus realism): results come from the synthetic public corpus only |
| Five problem interviews (Section 2A.5), replaced by 3 external query writers who see only a topic description | Section 2A.5, 20.4 | Threat to validity (query formulation bias): only 30 queries come from people who never saw the corpus, and they describe imagined searches over someone else's journal, not their own failed searches |
| CSV and plain-text import | Section 4.6 | Future work; JSON and Markdown satisfy "at least one external format" |
| Answer evaluation set and H6 | Section 18A.4, 20.2 | Threat to validity / future work if 3.6 is skipped; if 3.6 is built, only the validation layer is claimed, not measured attribution metrics |

## Never cut (Section 2A.3)

- user isolation and its tests;
- the labeled query set, including no-answer queries;
- the reproducible benchmark and the freeze record;
- the grounding rule: if any Ask Pastline path is built, it can never return text without verified source identifiers.

## When a gate is missed

A missed gate triggers a scope cut, not extra hours (Section 2A.4). Decide on the Monday after the gate and record the cut in `PROGRESS.md`. Cut in this order:

1. Optional Ask Pastline (3.6).
2. React client reduced to login and a search page with mode switch; timeline and editor dropped.
3. Markdown import dropped; JSON import kept.
4. Query set capped at 60.

Week 4 thesis writing starts on 2026-11-16 regardless of what was cut.
