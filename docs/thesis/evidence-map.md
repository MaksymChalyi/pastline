# Thesis evidence map

A living table that links roadmap work to the evidence it produces and to the thesis section it can support. Update a row when its evidence file actually exists. Never fill a row with expected results.

Evidence file paths are planned locations from the roadmap until the Status column says otherwise. Thesis section numbers follow the working structure in `PASTLINE_MASTER_PROMPT.md` Section 21.2 and must be adapted to the university template.

Status values: `NOT_STARTED`, `IN_PROGRESS`, `EVIDENCE_EXISTS`, `REVIEWED`.

| Roadmap phase | Implementation/experiment | Evidence file | Possible thesis section | Defense demo value | Status |
|---|---|---|---|---|---|
| Week 1 | LLM, embedding and RAG fundamentals | `docs/learning/week-01-ai-foundations/day-01-llm-fundamentals.md` | 2. Analysis of approaches (RAG and grounding) | Low: explain the retrieval vs generation flow | NOT_STARTED |
| Week 1 | Local Ollama and raw Java HTTP client | `docs/learning/week-01-ai-foundations/day-02-ollama-local.md`, `day-03-java-ollama.md`, `labs/llm-http-java` | 3. System design (AI/provider boundary) | Low | NOT_STARTED |
| Week 1 | OpenAI API and provider comparison | `docs/learning/week-01-ai-foundations/day-04-openai-api.md`, `day-05-provider-comparison.md` | 3. System design (local vs cloud trade-offs) | Medium: provider comparison table | NOT_STARTED |
| Week 1 | Hand-written cosine similarity retrieval | `docs/learning/week-01-ai-foundations/day-06-embeddings.md`, `labs/embeddings-java` | 2. Analysis of approaches (vector retrieval) | Medium: show the similarity calculation | NOT_STARTED |
| Week 1 | Spring AI rebuild of the labs | `docs/learning/week-01-ai-foundations/day-07-spring-ai.md` | 4. Implementation (Spring AI behind ports) | Low | NOT_STARTED |
| Week 2 | Modular monolith, auth, journal CRUD, user isolation tests | `docs/adr/ADR-002-modular-monolith.md`, backend tests | 3. System design; 4. Implementation | High: user A cannot read user B's entry | NOT_STARTED |
| Week 3 | React web client and journal UX | `frontend/`, `docs/screenshots/` | 4. Implementation (React client) | High: basic journal in a browser | NOT_STARTED |
| Week 4 | Lexical search baseline and Ukrainian language decision | `docs/adr/` (ADR-005), `docs/experiments/search-query-set-v1.json` | 2. Lexical retrieval; 5. Baseline | High: exact-phrase lexical search | NOT_STARTED |
| Week 4 | Import pipeline with dry-run and idempotency | `docs/adr/` (ADR-012), import tests, learning note | 4. Implementation; 5. Dataset | Medium: import dry-run report | NOT_STARTED |
| Week 5 | pgvector semantic search and embedding model decision | `docs/adr/` (ADR-006), EXP-002 / EXP-003 results | 2. Vector retrieval; 4. Implementation | High: paraphrase found by semantic mode | NOT_STARTED |
| Week 6 | Hybrid retrieval with RRF | `docs/adr/` (ADR-007), EXP-004 results | 5. Experimental evaluation | High: same query in three modes | NOT_STARTED |
| Week 6-7 | Query routing, date resolution, period summaries, coverage | `docs/adr/` (ADR-013, ADR-014), routing accuracy results | 3. Search pipeline; 5. H5 | High: aggregation answer with reported coverage | NOT_STARTED |
| Week 7 | Ask Pastline RAG with grounding validation | `docs/adr/` (ADR-015), poisoned-output tests, `docs/experiments/answer-eval-v1.json` | 4. Ask Pastline; 5. H6 | High: answer with clickable sources and refusal | NOT_STARTED |
| Week 8 | Privacy, export, logging policy, third-party data position | `docs/architecture/PRIVACY.md`, ADR-009, ADR-016 | 3. Privacy/security decisions | Medium: show what leaves the system | NOT_STARTED |
| Week 9 | Evaluation dataset, relevance labels and benchmark runner | `docs/experiments/corpus-public-v1/`, labeled query set, runner command | 5. Dataset, query set, relevance labels | Medium | NOT_STARTED |
| Week 10 | Experiment execution, charts, error analysis, validity threats | `docs/experiments/results/`, `docs/thesis/VALIDITY_THREATS.md`, `docs/thesis/RESEARCH_LOG.md` | 5. Results, error analysis, threats to validity | High: charts from raw results | NOT_STARTED |
| Week 11 | AWS deployment, CI/CD, monitoring | `infra/aws/`, `.github/workflows/`, ADR-010 | 4. Testing/deployment | Medium: deployed system (cuttable, Section 2A.3) | NOT_STARTED |
| Week 12 | Demo scenario, release `v1.0-demo`, thesis freeze | `docs/thesis/THESIS_FREEZE.md`, demo script, README | 5. Reproducibility; 6. Conclusions | High: the five-minute demo | NOT_STARTED |
