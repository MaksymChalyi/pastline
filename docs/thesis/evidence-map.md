# Thesis evidence map

A living table that links sprint checkpoints to the evidence they produce and to the thesis section they can support. Update a row when its evidence file actually exists. Never fill a row with expected results.

Rows follow the active plan, `docs/plan/SPRINT_PLAN_4W.md`. Evidence file paths are planned locations until the Status column says otherwise. Thesis section numbers follow the working structure in `PASTLINE_MASTER_PROMPT.md` Section 21.2 and must be adapted to the university template.

Status values: `NOT_STARTED`, `IN_PROGRESS`, `EVIDENCE_EXISTS`, `REVIEWED`.

| Roadmap phase | Implementation/experiment | Evidence file | Possible thesis section | Defense demo value | Status |
|---|---|---|---|---|---|
| Sprint W1 / 1.1 | AI basics: Ollama chat and embed via curl, cosine similarity by hand, model choice | `docs/learning/week-01-ai-foundations/day-01-llm-fundamentals.md`, ADR-006 | 2. Analysis of approaches (embeddings, RAG) | Low: explain retrieval vs generation | NOT_STARTED |
| Sprint W1 / 1.2 | Spring Boot modular monolith, pgvector Docker Compose, Flyway | ADR-002, migration startup test | 3. System design (architecture, schema) | Low | NOT_STARTED |
| Sprint W1 / 1.3-1.4 | Session auth, entry CRUD, user isolation tests | backend auth/isolation integration tests | 3. Privacy/security decisions; 4. Implementation | High: user A cannot read user B's entry | NOT_STARTED |
| Sprint W1 / 1.5 | JSON + Markdown import with dry-run and content-hash idempotency | import parser tests, re-import test | 4. Implementation; 5. Dataset | Medium: dry-run report | NOT_STARTED |
| Sprint W1 / parallel | External query collection from 3 people who never saw the corpus | `docs/plan/QUERY_COLLECTION.md`, `docs/experiments/external-queries-raw.md` | 5. Query set; threats to validity | Low | NOT_STARTED |
| Sprint W1 / 1.6 | Synthetic corpus generator with fixed seed | `scripts/`, `docs/experiments/corpus-public-v1/`, corpus checksum | 5. Dataset | Low | NOT_STARTED |
| Sprint W1 / 1.7 | Lexical FTS baseline for Ukrainian | ADR-005, known successes/failures note | 2. Lexical retrieval; 5. Baseline | High: exact-phrase lexical search | NOT_STARTED |
| Sprint W1 / 1.8 | Semantic search: Spring AI + Ollama, exact pgvector search, re-embedding on edit | ADR-006, semantic search integration tests | 2. Vector retrieval; 4. Implementation | High: paraphrase found by semantic mode | NOT_STARTED |
| Sprint W2 / 2.1-2.2 | Hybrid RRF and unified search API with mode | ADR-007, RRF unit tests | 3. Search pipeline; 4. Implementation | High: same query in three modes | NOT_STARTED |
| Sprint W2 / 2.3 | Minimal React client | `frontend/`, `docs/screenshots/` | 4. Implementation (React client) | High: working journal in a browser | NOT_STARTED |
| Sprint W2 / 2.4-2.5 | Labeling guideline and 60-80 labeled queries with source tags | `docs/experiments/LABELING_GUIDELINE.md`, `docs/experiments/search-query-set-v1.json` | 5. Query set, relevance labels | Low | NOT_STARTED |
| Sprint W2 / 2.6 | Benchmark runner: Recall@K, MRR, nDCG@K, latency | runner command, `docs/experiments/results/` | 5. Metrics; reproducibility | Medium | NOT_STARTED |
| Sprint W3 / 3.1-3.2 | Final experiment run and charts from raw results | `docs/experiments/results/`, chart script | 5. Results | High: charts from raw results | NOT_STARTED |
| Sprint W3 / 3.3 | Error analysis of at least 10 failures | `docs/experiments/ERROR_ANALYSIS.md` | 5. Error analysis | Medium | NOT_STARTED |
| Sprint W3 / 3.4 | Thesis freeze (2026-11-12) | `docs/thesis/THESIS_FREEZE.md` | 5. Reproducibility | Low | NOT_STARTED |
| Sprint W3 / 3.5 | Threats to validity, including every sprint cut | `docs/thesis/VALIDITY_THREATS.md` | 5. Threats to validity; 6. Limitations | Medium | NOT_STARTED |
| Sprint W3 / 3.6 (optional) | Minimal Ask Pastline with source-ID validation | poisoned-output tests | 4. Ask Pastline | High: answer with sources and refusal | NOT_STARTED |
| Sprint W4 / 4.1-4.2 | Thesis text and five-minute demo script | thesis manuscript (outside repo), demo script | 6. Conclusions | High: the demo | NOT_STARTED |
| All checkpoints | Design decisions with committee questions | `docs/thesis/DEFENSE_NOTES.md` | Defense preparation | n/a | NOT_STARTED |
