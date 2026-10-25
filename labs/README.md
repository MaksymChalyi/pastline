# labs/

Isolated learning experiments. Each lab lives in its own subdirectory with its own build.

**Status:** empty. The first labs are created during Week 1.

Planned Week 1 labs (from the roadmap, not yet created):

| Lab | Roadmap day | Purpose |
|-----|-------------|---------|
| `labs/llm-http-java` | Day 3 | Call Ollama from plain Java 21 `HttpClient` |
| `labs/openai-java` | Day 4 | Call OpenAI through raw HTTP and the official Java SDK |
| `labs/embeddings-java` | Day 6 | Embeddings and cosine similarity without a vector database |
| Spring AI lab | Day 7 | Rebuild the same concepts with Spring AI |

## Rules

- Production code in `backend/` or `frontend/` must never depend on code in `labs/`.
- Lab code does not move into production by copy-paste without review.
- Labs never contain API keys or real journal entries. Use environment variables and synthetic data.
