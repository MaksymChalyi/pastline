# REST API

Усі ендпоінти під `/api/v1`, формат JSON, ідентифікатори — UUID. Усе, крім позначеного «публічний», вимагає
сесії. Змінні запити (POST/PUT/DELETE) вимагають заголовок `X-XSRF-TOKEN` зі значенням cookie `XSRF-TOKEN`.

Деталі, приклади і flow кожної групи ендпоінтів: [auth](features/auth.md), [записи](features/journal.md),
[імпорт](features/import.md), [пошук](features/search.md), [Ask Pastline](features/ask.md).

## Ендпоінти

| Метод і шлях | Призначення | Відповідь |
|---|---|---|
| `GET /auth/csrf` | видати CSRF-токен (публічний) | 200 `{headerName, token}` |
| `POST /auth/register` | реєстрація `{email, password (8–128), timezone?}` (публічний) | 201 користувач |
| `POST /auth/login` | вхід `{email, password}` (публічний) | 200 користувач, нова сесія |
| `POST /auth/logout` | вихід | 204 |
| `GET /me` | поточний користувач | 200 `{id, email, timezone}` |
| `POST /entries` | створити запис `{title?, content, entryDate}` | 201 запис |
| `GET /entries?from&to&page&size` | стрічка, новіші вгорі; `size` 1–100 | 200 `{items, page, size, totalItems, totalPages}` |
| `GET /entries/{id}` | запис | 200 / 404 |
| `PUT /entries/{id}` | змінити запис | 200 / 404 / 409 |
| `DELETE /entries/{id}` | видалити запис | 204 / 404 |
| `POST /import?dryRun=true` | імпорт файлу (multipart, поле `file`, JSON або Markdown, до 5 МБ, до 5000 записів); `dryRun=true` — лише перевірка | 200 звіт `{dryRun, total, created, duplicates, failed, firstDate, lastDate, errors}` |
| `GET /search?q&mode&limit&debug` | пошук; `mode` = lexical / semantic / hybrid, `limit` ≤ 50, `debug` — ранги й RRF-бал (hybrid) | 200 `{mode, query, tookMs, pendingCount, hits}` |
| `POST /search/reindex` | видалити вектори користувача, щоб їх перерахував індексатор | 202 `{removed}` |
| `POST /ai/ask` | Ask Pastline `{question}` (до 500 символів) | 200 `{status: ANSWERED / NO_EVIDENCE, answer, sources, removedSentences, tookMs}` |
| `GET /actuator/health` | стан застосунку (публічний) | 200 `{status: UP}` |

## Формат помилки

```json
{ "code": "ENTRY_NOT_FOUND", "message": "…", "requestId": "…", "fieldErrors": [ { "field": "content", "message": "…" } ] }
```

`requestId` збігається із заголовком відповіді `X-Request-Id` і рядком у лозі сервера.

| HTTP | `code` | Коли |
|---|---|---|
| 400 | `VALIDATION_ERROR` | некоректні поля або параметри (`fieldErrors` — по полях) |
| 400 | `MALFORMED_JSON` | тіло запиту не JSON |
| 400 | `IMPORT_INVALID_FILE`, `IMPORT_UNSUPPORTED_FORMAT` | файл імпорту не розібрано / не JSON і не Markdown |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS` | немає сесії / неправильний email чи пароль |
| 403 | `CSRF_INVALID`, `ACCESS_DENIED` | немає або застарів CSRF-токен / доступ заборонено |
| 404 | `ENTRY_NOT_FOUND`, `NOT_FOUND` | запису немає або він чужий / невідомий шлях |
| 405 | `METHOD_NOT_ALLOWED` | метод не підтримується |
| 409 | `EMAIL_TAKEN`, `ENTRY_DUPLICATE`, `CONFLICT` | email зайнятий / такий самий запис за ту саму дату вже є |
| 413 | `PAYLOAD_TOO_LARGE` | файл більший за 5 МБ |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | неправильний Content-Type |
| 503 | `EMBEDDING_UNAVAILABLE`, `AI_UNAVAILABLE` | модель ембедінгів / відповідей недоступна |
| 500 | `INTERNAL_ERROR` | неочікувана помилка (деталі лише в лозі за `requestId`) |

Ручні запити для перевірки: колекція Postman `backend/http/pastline.postman_collection.json` (CSRF ставить її
скрипт) і `backend/http/gate1.http` (IntelliJ HTTP Client).

Кожна відповідь має заголовок `X-Request-Id` (`RequestIdFilter`); той самий id потрапляє в MDC логів, тож помилку
з UI можна знайти в лозі сервера.
