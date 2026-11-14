# Екскурсія кодом

Порядок читання коду, щоб за кілька годин зрозуміти весь Pastline. Кожен крок — кілька файлів і питання, на які
після них треба вміти відповісти. Шляхи бекенду — відносно `backend/src/main/java/com/pastline/`.

## Крок 0. Запустити

Пройти [getting-started.md](../getting-started.md) до кінця, імпортувати корпус і спробувати всі сторінки. Код
читається набагато легше, коли бачив, що він робить.

## Крок 1. Дані (30 хв)

- `backend/src/main/resources/db/migration/V1…V5` — уся схема.
- `journal/entity/JournalEntry.java`, `auth/entity/User.java`.
- [database.md](../database.md).

Питання: чому `UNIQUE (user_id, entry_date, content_hash)`? Чому `search_vector` — generated column? Як зрозуміти,
що вектор застарів, якщо статусу `PENDING` немає?

## Крок 2. Запит від браузера до БД (45 хв)

Пройти один запит `GET /api/v1/entries` наскрізь:

1. `shared/web/RequestIdFilter` — перший фільтр: `X-Request-Id` і MDC.
2. `auth/security/SecurityConfig` — ланцюжок Spring Security, CSRF, JSON-відповіді на 401/403.
3. `journal/controller/JournalController` — звідки береться `user.id()`.
4. `journal/service/JournalService.list` — валідація параметрів, межі дат.
5. `journal/repository/JournalEntryRepository` — derived query з `userId`.
6. `journal/dto/PageResponse`, `EntryResponse` — що йде назовні, а що ні.
7. `shared/exception/GlobalExceptionHandler`, `BaseException`, `ErrorResponse` — як будь-яка помилка стає
   `{code, message, requestId, fieldErrors}`.

Питання: що станеться, якщо користувач B запитає `GET /entries/{id}` запису A? Де саме це вирішується?

## Крок 3. Автентифікація (45 хв)

`auth/controller/AuthController`, `auth/service/AuthService`, `auth/security/AuthenticatedUser`, потім
`frontend/src/api/client.ts` і `frontend/src/auth/useAuth.ts`. Тест, що описує цикл як у браузері:
`backend/src/test/.../auth/CsrfFlowTest`.

Питання: навіщо `changeSessionId()`? Чому cookie `XSRF-TOKEN` не `HttpOnly`, а сесійна — так? Що робить клієнт
на `403 CSRF_INVALID`?

[features/auth.md](../features/auth.md)

## Крок 4. Імпорт (30 хв)

`importing/parser/*`, `importing/service/ImportService`, `journal/service/JournalService.createAll` і
`existingKeys`.

Питання: чому немає однієї транзакції на весь файл? Як імпорт пропускає дублікати, не роблячи запит на кожен
запис? Що робить `Persistable` у `JournalEntry`?

[features/import.md](../features/import.md)

## Крок 5. Лексичний пошук (30 хв)

`search/controller/SearchController`, `search/service/SearchService.search`,
`search/repository/LexicalSearchRepository`, `LexicalOperator`. Виконати в `psql` запити з
[concepts.md, розділ 1](concepts.md#1-повнотекстовий-пошук-postgresql).

Питання: чим `websearch_to_tsquery` кращий за `to_tsquery` для вводу користувача? Чому «пробіжку» не знаходить
запит «пробіжка»?

## Крок 6. Вектори (1 год)

1. `ai/port/EmbeddingModelPort` → `ai/client/OllamaEmbeddingAdapter`, `EmbeddingPrompts`.
2. `search/embedding/EmbeddingScheduler` → `EmbeddingIndexer`.
3. `search/repository/EmbeddingRepository`: `findStale`, `saveReady`, `saveFailed`, `search`.
4. Тест: `backend/src/test/.../search/SemanticSearchApiTest` і фейк `support/FakeEmbeddingModel`.

Питання: що відбувається з вектором після редагування запису? Чому недоступна модель не робить записи `FAILED`?
Навіщо `+prompts` у назві моделі?

[features/search.md](../features/search.md)

## Крок 7. Гібрид (20 хв)

`search/service/RrfFusion` і `RrfFusionTest`. Порахувати RRF вручну для двох коротких списків.

Питання: чому ранги, а не бали? Чому при недоступній моделі гібрид повертає 503, а не результати лексичного?

## Крок 8. Ask Pastline (1 год)

`ask/service/AskService` → `AskPrompt` → `ai/client/OllamaChatAdapter` → `CitationValidator`. Тести `AskApiTest`
(особливо `poisonedAnswerWithFabricatedIdIsCut`) і `CitationValidatorTest`.

Питання: чому `E1…E8`, а не UUID? Що саме гарантує перевірка посилань і чого не гарантує? Що бачить модель, якщо
в тексті запису є `</entry>`?

[features/ask.md](../features/ask.md)

## Крок 9. Експеримент (45 хв)

`benchmark/BenchmarkRunner` → `CorpusPreparer` → `BenchmarkService` / `AskBenchmarkService` → `BenchmarkMetrics`.
Відкрити один звіт `docs/experiments/results/run-*-and.json` і знайти в ньому конфігурацію, SHA-256 і метрики.

Питання: як отримано еталон релевантності? Чому якість рахується з одного прогону, а затримка — з п'яти?

[experiments/README.md](../experiments/README.md)

## Крок 10. Клієнт (1 год)

`frontend/src/main.tsx` → `App.tsx` → `layout/AppLayout.tsx` → `entries/useEntries.ts` → `pages/EntriesPage.tsx`
→ `pages/SearchPage.tsx` → `pages/AskPage.tsx`.

Питання: що відбувається в кеші після збереження запису? Чому фільтр живе в URL, а не в `useState`? Що зупиняє
таймер на сторінці Ask, коли відповідь прийшла?

[frontend.md](../frontend.md), [concepts.md, React](concepts.md#react-для-java-розробника)

## Самоперевірка

Відповісти без нотаток на питання з [diploma/defense-qa.md](../diploma/defense-qa.md). Якщо відповідь не
виходить — повернутися до відповідного кроку.
