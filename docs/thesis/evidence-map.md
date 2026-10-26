# Карта доказів диплома

Таблиця зв'язує кроки `PASTLINE_MASTER_PROMPT.md` (v2.2) з доказами, які вони створюють, і з підрозділами диплома з `docs/thesis/OUTLINE.md`. Рядок оновлюється лише тоді, коли файл-доказ реально існує. Очікувані результати сюди не вписуються.

Шляхи до доказів є запланованими, поки статус не `EVIDENCE_EXISTS` або `REVIEWED`. Номери розділів відповідають робочій структурі з 3 розділів; адаптувати під шаблон кафедри.

Статуси: `NOT_STARTED`, `IN_PROGRESS`, `EVIDENCE_EXISTS`, `REVIEWED`.

## Етап 1: лабораторні (розділ 5 плану)

| Крок | Що зроблено | Доказ | Розділ диплома | Цінність для демо/захисту | Статус |
|---|---|---|---|---|---|
| Лаба 01 | Ollama через curl: `/api/chat`, `/api/embed`, холодний старт, довжина вектора | `labs/01-ollama-curl/README.md` | 1.4 | Низька: пояснення inference і embedding | NOT_STARTED |
| Лаба 02 | Java `HttpClient` → Ollama: таймаути, помилки, тести без Ollama | `labs/02-java-http-client/README.md` | 1.4 | Низька | NOT_STARTED |
| Лаба 03 (EXP-000) | Embeddings і cosine з нуля, Hit@1/Hit@5/MRR проти лексичної базової лінії | `labs/03-embeddings-cosine/README.md`, `labs/03-embeddings-cosine/results-YYYYMMDD.md` | 1.4, 1.8 | Середня: пілотні числа і провали semantic | NOT_STARTED |
| Лаба 04 | Spring AI: `ChatClient`, `EmbeddingModel`, порівняння з лабами 02–03 | `labs/04-spring-ai-demo/README.md` | 1.4, 2.2 | Низька: що абстрагує Spring AI | NOT_STARTED |
| Лаба 05 | Міні-RAG на Spring AI + pgvector, таблиця галюцинацій, prompt injection, пост-валідація id | `labs/05-spring-ai-rag/README.md` | 1.6, 2.7 | Висока: чому RAG галюцинує і навіщо валідація джерел | NOT_STARTED |

## Етап 2: застосунок (розділ 6 плану)

| Крок | Що зроблено | Доказ | Розділ диплома | Цінність для демо/захисту | Статус |
|---|---|---|---|---|---|
| 2.1 | Каркас Spring Boot, pgvector у Docker Compose, Flyway V1, health | ADR-002, ADR-003, тест старту міграцій, вивід `mvn verify` | 2.2, 2.3 | Низька | NOT_STARTED |
| 2.2 | Реєстрація, логін, логаут, `GET /api/v1/me`, сесія + CSRF | ADR-004, API-тести auth | 2.4 | Середня | NOT_STARTED |
| 2.3 | CRUD записів, пагінація, фільтр за датою | тест ізоляції (A отримує 404 на запис B), валідаційні тести | 2.4 | Висока: витоку між користувачами немає | NOT_STARTED |
| 2.4 | Імпорт JSON і Markdown з dry-run та ідемпотентністю; генератор корпусу | ADR-012, тести імпорту й повторного імпорту, `scripts/generate-corpus`, `docs/experiments/corpus-public-v1/` + SHA-256 | 2.5, 3.2 | Середня: звіт dry-run | NOT_STARTED |
| 2.5 | Лексичний пошук: FTS `simple`, GIN, `websearch_to_tsquery`, `ts_rank` | ADR-005, інтеграційні тести, вивід `EXPLAIN ANALYZE` | 2.6 | Висока: точний запит у lexical | NOT_STARTED |
| 2.6 | Семантичний пошук: `EmbeddingModelPort`, exact pgvector, статуси embeddings, reindex | ADR-006, інтеграційні тести (оновлення, видалення, ізоляція) | 2.2, 2.6 | Висока: перефразований запит у semantic | NOT_STARTED |
| 2.7 | Гібридний пошук RRF (k=60), debug-ранги | ADR-007, юніт-тести RRF | 2.6 | Висока: один запит у трьох режимах | NOT_STARTED |
| 2.8 | Мінімальний React-клієнт | `frontend/`, `docs/screenshots/` | 2.8 | Висока: робочий щоденник у браузері | NOT_STARTED |
| 2.9 | Бенчмарк-раннер: метрики й латентність у JSON | команда запуску, `docs/experiments/results/` | 3.4 | Середня: відтворюваність | NOT_STARTED |
| 2.10 | Ask Pastline: grounding, пост-валідація id джерел, відмова | тест з "отруєною" відповіддю, тест відмови, тест ізоляції Ask | 2.7 | Висока: відповідь із джерелами і чесна відмова | NOT_STARTED |

## Етап 5: деплой (розділ 9A плану)

| Крок | Що зроблено | Доказ | Розділ диплома | Цінність для демо/захисту | Статус |
|---|---|---|---|---|---|
| 9A | Vercel (frontend, rewrite `/api/*`) + Render (backend, профіль `cloud`, OpenAI embeddings) + Render Postgres з pgvector, синтетичний корпус, маркування демо | ADR-013, публічний URL, скріншот позначки "демо" | 2.10 | Висока: онлайн-демо для комісії | NOT_STARTED |

## Етап 3: експеримент (розділи 7 і 9.1 плану)

| Крок | Що зроблено | Доказ | Розділ диплома | Цінність для демо/захисту | Статус |
|---|---|---|---|---|---|
| 7.1 | Набір 60–80 запитів, ≥20% без відповіді, запити від 3 людей, категорії | `docs/plan/QUERY_COLLECTION.md`, `docs/experiments/search-query-set-v1.json` | 3.2 | Низька | NOT_STARTED |
| 7.2 | Настанова розмітки, мітки 0/1/2, повторна розмітка 20 запитів | `docs/experiments/LABELING_GUIDE.md`, файл міток, розрахунок узгодженості | 3.3 | Низька | NOT_STARTED |
| 7.3–7.4 | Повний прогін, Recall@10, MRR, nDCG@10, p50/p95, графіки зі сирих даних | `docs/experiments/results/`, скрипт графіків у репозиторії | 3.4, 3.5 | Висока: графіки експерименту | NOT_STARTED |
| 7.5 | Аналіз ≥10 провалів | `docs/experiments/ERROR_ANALYSIS.md` | 3.6 | Середня | NOT_STARTED |
| 9.1 | Оцінка ~20 відповідей Ask Pastline з валідацією і без | `docs/experiments/answer-eval-v1.json`, сирі відповіді в `docs/experiments/results/` | 3.7 | Висока: ефект пост-валідації | NOT_STARTED |
| 7.6 | Загрози валідності | `docs/thesis/VALIDITY_THREATS.md` | 3.8 | Середня | NOT_STARTED |
| 7.7 | Freeze (25.11): SHA, версії, SHA-256 даних, конфігурація | `docs/thesis/THESIS_FREEZE.md` | 3.4 | Низька: відтворюваність | NOT_STARTED |

## Наскрізне

| Крок | Що зроблено | Доказ | Розділ диплома | Цінність для демо/захисту | Статус |
|---|---|---|---|---|---|
| Усі чекпоінти | Рішення, причини, відкинуті альтернативи, питання комісії | `docs/thesis/DEFENSE_NOTES.md` | підготовка до захисту | n/a | NOT_STARTED |
