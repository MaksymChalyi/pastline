# План вивчення коду

Як за 9 сесій по 1–3 години розібратися в Pastline так, ніби пишеш його сам. План розрахований на
Java/Spring-розробника: звичні речі (контролер → сервіс → репозиторій, JPA, валідація) проходимо швидко, а час
витрачаємо на те, що в проєкті нестандартне: сесія + CSRF без form login, повнотекстовий пошук, вектори, RRF, RAG
з перевіркою посилань, бенчмарк і React.

Шляхи бекенду — відносно `backend/src/main/java/com/pastline/`, тестів — `backend/src/test/java/com/pastline/`.

## Як працювати з планом

Кожна сесія: **прочитати → запустити → подивитися зсередини → відповісти без нотаток.**

- Читати код в IntelliJ: `Ctrl+B` — перехід до оголошення, `Alt+F7` — хто викликає, `Ctrl+F12` — структура файлу.
- Запускати бекенд у режимі Debug (`PastlineBackendApplication`) і ставити точки зупинки, вказані в сесіях.
- Запити — з Postman (колекція нижче) або через сторінки клієнта з відкритими DevTools → Network.
- Експерименти робити в окремій гілці (`git switch -c study`) і не переносити в `main`.
- Сесія завершена, коли відповідаєш на її питання вголос, не дивлячись у код.

## Підготовка (30 хв)

1. Запустити все за [getting-started.md](../getting-started.md): БД, Ollama, бекенд, клієнт.
2. Postman → **Import** → `backend/http/pastline.postman_collection.json`. Колекція сама бере CSRF-токен перед
   кожним POST/PUT/DELETE, cookie сесії Postman зберігає сам. Порядок: `Health` → `Auth/Register` → `Auth/Login`
   → решта. Для `Import` у Body → form-data → `file` обрати `docs/experiments/corpus-v1/corpus.json`.
3. Щоб бачити кожен SQL, додати в конфігурацію запуску IntelliJ (Program arguments), а не в
   `application.properties`:
   ```
   --logging.level.org.springframework.jdbc.core.JdbcTemplate=DEBUG --logging.level.org.hibernate.SQL=DEBUG
   ```
4. Консоль БД: `docker compose exec postgres psql -U pastline -d pastline`.

---

## Сесія 1. Загальна картина (1,5 год)

**Прочитати:** [README](../../README.md) → [architecture.md](../architecture.md) →
[decisions.md](../decisions.md), рішення 1–8.

**Зробити:** пройти колекцію Postman зверху вниз, потім те саме в браузері: запис, імпорт, три режими пошуку,
«Запитати».

**Питання:**
- Які 8 модулів у бекенді і хто від кого залежить?
- Чому моноліт, PostgreSQL + pgvector і сесія, а не мікросервіси, окрема векторна БД і JWT?
- Що працює без Ollama, а що повертає 503?

## Сесія 2. Дані і шлях запиту (2 год)

**Прочитати по порядку:**
1. `backend/src/main/resources/db/migration/V1…V5` і [database.md](../database.md).
2. `journal/entity/JournalEntry` (зверни увагу на `Persistable`).
3. `shared/web/RequestIdFilter` → `journal/controller/JournalController` → `journal/service/JournalService` →
   `journal/repository/JournalEntryRepository`.
4. `shared/exception/BaseException`, `GlobalExceptionHandler`, `ErrorResponse`.
5. [features/journal.md](../features/journal.md).

**Зробити:**
- Точка зупинки в `JournalService.create` → `Entries/Create entry`. Подивитися `normalize`, хеш, перевірку дати.
- `Create duplicate (409)` і `Create invalid (400)`: знайти, який клас сформував кожну відповідь.
- Ізоляція: ще раз `Auth/Register` + `Auth/Login` (новий користувач), потім `Entries/Get` з `entryId` першого →
  404.
- У `psql`: `\d journal_entries` і `SELECT entry_date, title, content_hash FROM journal_entries LIMIT 5;`.
- Тест: `.\mvnw test -Dtest=JournalApiTest`.

**Питання:**
- Чому чужий запис дає 404, а не 403, і де саме це вирішується?
- Що гарантує `UNIQUE (user_id, entry_date, content_hash)` і навіщо нормалізація перед хешем?
- Навіщо `Persistable` і що було б без нього?

## Сесія 3. Безпека (2 год)

**Прочитати:** `auth/security/SecurityConfig` → `auth/controller/AuthController` → `auth/service/AuthService` →
`auth/security/AuthenticatedUser` → тест `auth/CsrfFlowTest` → [features/auth.md](../features/auth.md) і
[concepts.md, розділ 8](concepts.md#8-сесія-cookie-csrf).

**Зробити:**
- У Postman вимкнути скрипт колекції (колекція → Scripts → Pre-request) і повторити `Create entry` → 403
  `CSRF_INVALID`. Увімкнути назад.
- У браузері DevTools → Application → Cookies: `JSESSIONID` (HttpOnly) і `XSRF-TOKEN` (без HttpOnly); у Network —
  заголовок `X-XSRF-TOKEN` на POST.
- Точка зупинки в `AuthController.login`: id сесії до і після `changeSessionId()`.
- Тести: `.\mvnw test "-Dtest=AuthApiTest,CsrfFlowTest"`.

**Питання:**
- Чому `formLogin` вимкнено, а вхід — звичайний контролер?
- Як працює double submit cookie і чому чужий сайт не підробить заголовок?
- Що таке session fixation і який рядок від неї захищає?
- Чому невідомий email і неправильний пароль дають однакову відповідь?

## Сесія 4. Імпорт (1,5 год)

**Прочитати:** `importing/parser/ImportFormat` → `JsonEntryParser` → `MarkdownEntryParser` →
`importing/service/ImportService` → `JournalService.existingKeys` і `createAll` →
[features/import.md](../features/import.md).

**Зробити:**
- Створити маленький `.md`: текст перед першим заголовком, один нормальний запис, один із датою в майбутньому,
  один без дати в заголовку. `Import — dry run` → подивитися `errors[].index`.
- `Import — save` з корпусом двічі: другий раз `created = 0`. За SQL-логами видно, скільки запитів іде на файл.
- Тест: `.\mvnw test -Dtest=ImportApiTest`.

**Питання:**
- Чому немає однієї транзакції на весь файл і що станеться при падінні посередині?
- Як дублікати з БД перевіряються одним запитом?
- Що робить `catch (DataIntegrityViolationException)` в `ImportService`?

## Сесія 5. Пошук — ядро диплома (3 год)

**Спочатку теорія:** [concepts.md, розділи 1–5](concepts.md).

**Прочитати:**
1. `search/controller/SearchController` → `search/service/SearchService`.
2. `search/repository/LexicalSearchRepository`, `LexicalOperator`.
3. `ai/port/EmbeddingModelPort` → `ai/client/OllamaEmbeddingAdapter`, `EmbeddingPrompts`.
4. `search/embedding/EmbeddingScheduler` → `EmbeddingIndexer` → `search/repository/EmbeddingRepository`.
5. `search/service/RrfFusion`.
6. [features/search.md](../features/search.md).

**Зробити в `psql`:**
```sql
SELECT to_tsvector('simple', 'Пробіг 5 км у парку');
SELECT websearch_to_tsquery('simple', 'біг -ранок "у парку"');
EXPLAIN ANALYZE SELECT id FROM journal_entries WHERE search_vector @@ websearch_to_tsquery('simple', 'ОВДП');
SELECT status, model, count(*) FROM entry_embeddings GROUP BY 1, 2;
```

**Зробити в застосунку:**
- `Search/Lexical`, `Semantic`, `Hybrid + debug` з тим самим запитом, потім `Cross-lingual`. Порівняти ранги.
- Точка зупинки в `RrfFusion.fuse`: порахувати бал першого результату вручну і звірити з `rrfScore`.
- `Entries/Update` і в `psql` подивитися, як вектор запису стає застарілим і за кілька секунд перераховується.
- Тести: `.\mvnw test "-Dtest=RrfFusionTest,SearchApiTest,SemanticSearchApiTest,HybridSearchApiTest"`.

**Питання:**
- Чому «пробіжку» не знаходить запит «пробіжка»? Що змінив би OR?
- Як індексатор знає, які записи рахувати, якщо статусу `PENDING` немає?
- Чому точний пошук, а не HNSW?
- Чому RRF працює з рангами, а не з балами? Що робить `k`?
- Чому гібрид при недоступній моделі не переходить на лексичний?

## Сесія 6. Ask Pastline (2 год)

**Спочатку теорія:** [concepts.md, розділи 6–7](concepts.md#6-rag-і-галюцинації).

**Прочитати:** `ask/controller/AskController` → `ask/service/AskService` → `AskPrompt` →
`ai/client/OllamaChatAdapter` → `CitationValidator` → тести `ask/AskApiTest`, `ask/CitationValidatorTest` →
[features/ask.md](../features/ask.md).

**Зробити:**
- Точка зупинки в `AskService.ask` на виклику `chatModel.generate`: скопіювати промпт, який бачить модель.
- Наступний рядок: порівняти сиру відповідь `raw` з тим, що лишилось після `CitationValidator.validate`.
- `Ask — no answer in entries` → `NO_EVIDENCE`: на якому з трьох кроків сталася відмова?
- У гілці `study` дописати свій випадок у `CitationValidatorTest` (наприклад, `[E1, E9]` в одному реченні) і
  запустити `.\mvnw test -Dtest=CitationValidatorTest` (Docker не потрібен).

**Питання:**
- Що саме гарантує перевірка посилань і чого не гарантує?
- Чому `E1…E8`, а не UUID? Навіщо екранування `<` і `>`?
- Чому `qwen3:4b-instruct`, а не `qwen3:4b`? Чому вимкнено повтори Spring AI?

## Сесія 7. Експеримент (2 год)

**Прочитати:** [experiments/README.md](../experiments/README.md) → `benchmark/BenchmarkRunner` →
`CorpusPreparer` → `QuerySetLoader` → `BenchmarkService` → `BenchmarkMetrics` → `AskBenchmarkService` →
[decisions.md](../decisions.md), рішення 16–24.

**Зробити:**
- Відкрити `docs/experiments/results/run-20261114-155311-and.json`: знайти конфігурацію, SHA-256 корпусу і
  метрики `semantic`, `type:paraphrase`. Звірити з `experiments/results-tables.md`.
- Порахувати MRR@10 вручну для трьох запитів зі звіту.
- Тест: `.\mvnw test -Dtest=BenchmarkMetricsTest` (Docker не потрібен).
- За бажанням повторити прогін пошуку (~10 хв) за командою з `experiments/README.md` і переконатися, що якість
  збігається.

**Питання:**
- Як отримано еталон релевантності без ручних міток?
- Чому Recall@10 нормований на min(10, кількість релевантних)?
- Чому якість рахується з одного прогону, а затримка — з п'яти?
- Що показала абляція AND/OR і чому в застосунку лишився AND?

## Сесія 8. Клієнт (2–3 год)

Тут більше нового, тож спершу [frontend.md](../frontend.md) і
[concepts.md, React](concepts.md#react-для-java-розробника).

**Прочитати по порядку:**
1. `frontend/src/main.tsx` → `App.tsx` → `layout/AppLayout.tsx`, `layout/PublicOnly.tsx`.
2. `api/client.ts` → `api/types.ts` → `api/entries.ts`.
3. `auth/useAuth.ts` → `pages/AuthForm.tsx`.
4. `entries/useEntries.ts` → `pages/EntriesPage.tsx` → `pages/EntryEditorPage.tsx`.
5. `pages/SearchPage.tsx` → `pages/AskPage.tsx` → `pages/ImportPage.tsx`.

**Зробити:**
- Встановити розширення браузера React DevTools і подивитися дерево компонентів і їхній стан.
- У вкладці Network простежити, які запити йдуть при відкритті стрічки, збереженні запису і поверненні на стрічку.
- Перемкнути режим пошуку і подивитися, як змінюється URL і які запити при цьому йдуть.
- У гілці `study` додати під полем тексту в редакторі лічильник символів (`content.length` / 20 000), потім
  `npm run check`.

**Питання:**
- Що відбувається з кешем після збереження запису і чому стрічка оновлюється сама?
- Чому фільтр і пошуковий запит зберігаються в URL?
- Що робить клієнт на 401 і на `403 CSRF_INVALID`?
- Що зупиняє таймер на сторінці «Запитати», коли приходить відповідь?

## Сесія 9. Конфігурація і деплой (1 год)

**Прочитати:** `application.properties` → `application-benchmark.properties` → `application-cloud.properties` →
`backend/Dockerfile` → `frontend/vite.config.ts`, `frontend/vercel.json` →
[configuration.md](../configuration.md), [deployment.md](../deployment.md), [ai-providers.md](../ai-providers.md).

**Питання:**
- Як той самий код працює з Ollama локально і з Gemini онлайн?
- Чому Vercel пересилає `/api/*`, а не браузер іде напряму на Render?
- Чим онлайн-демо відрізняється від експерименту і чому його числа не йдуть у диплом?

---

## Фінальна перевірка

1. Відповісти без нотаток на всі питання з [diploma/defense-qa.md](../diploma/defense-qa.md).
2. За 5 хвилин розповісти вголос, що відбувається від натискання «Запитати» до появи відповіді з джерелами:
   клієнт → фільтри Spring Security → контролер → пошук → модель → перевірка → відповідь.
3. Намалювати з пам'яті схему БД і схему модулів.

## Що почитати додатково

Лише те, що прямо стосується рішень у проєкті:

| Тема | Джерело |
|---|---|
| CSRF у Spring Security | https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html |
| Повнотекстовий пошук | https://www.postgresql.org/docs/current/textsearch.html (розділи 12.1–12.3) |
| pgvector | https://github.com/pgvector/pgvector (README: querying, indexing) |
| RRF | Cormack, Clarke, Büttcher. Reciprocal Rank Fusion outperforms Condorcet and individual rank learning methods. SIGIR 2009 |
| RAG | Lewis et al. Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks, 2020: https://arxiv.org/abs/2005.11401 |
| React | https://react.dev/learn/thinking-in-react, https://react.dev/learn/you-might-not-need-an-effect |
| TanStack Query | https://tanstack.com/query/latest/docs/framework/react/guides/important-defaults |
