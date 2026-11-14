# Конфігурація

Усі налаштування бекенду — у `backend/src/main/resources/application*.properties`. Значення, що відрізняються між
машинами або є секретами, читаються зі змінних середовища у формі `${ЗМІННА:значення_за_замовчуванням}`.

## Профілі

| Профіль | Файл | Коли | Що змінює |
|---|---|---|---|
| (без профілю) | `application.properties` | локальна розробка, тести | Ollama, локальна БД на `5434`, порт `8090` |
| `benchmark` | `application-benchmark.properties` | експеримент | без веб-сервера і фонового індексатора, шляхи до корпусу й запитів, `temperature = 0` |
| `cloud` | `application-cloud.properties` | онлайн-демо на Render | Gemini замість Ollama, БД Neon із змінних, `Secure` cookie, порт `PORT` |

Профіль вмикається `-Dspring-boot.run.profiles=benchmark` (Maven) або змінною `SPRING_PROFILES_ACTIVE=cloud`
(так робить `backend/Dockerfile`).

## Змінні середовища

| Змінна | Обов'язкова | За замовчуванням | Де | Призначення |
|---|---|---|---|---|
| `PASTLINE_DB_PORT` | ні | `5434` | локально | порт PostgreSQL (і в `compose.yaml`, і в бекенді) |
| `PASTLINE_DB_USER` | ні | `pastline` | локально | користувач БД (Compose і бекенд) |
| `PASTLINE_DB_PASSWORD` | ні | `pastline` | локально | пароль БД (Compose і бекенд); для Compose діє лише на новому томі |
| `PASTLINE_PORT` | ні | `8090` | локально | порт бекенду |
| `OLLAMA_URL` | ні | `http://localhost:11434` | локально | адреса Ollama |
| `SPRING_PROFILES_ACTIVE` | ні | — | Render | `cloud` (задано в Dockerfile) |
| `PORT` | ні | `8090` | Render | порт, який дає Render |
| `SPRING_DATASOURCE_URL` | так, у `cloud` | — | Render | `jdbc:postgresql://<host>/neondb?sslmode=require&reWriteBatchedInserts=true` |
| `SPRING_DATASOURCE_USERNAME` | так, у `cloud` | — | Render | користувач Neon |
| `SPRING_DATASOURCE_PASSWORD` | так, у `cloud` | — | Render | пароль Neon (**секрет**) |
| `GEMINI_API_KEY` | так, у `cloud` | — | Render | ключ Google AI Studio (**секрет**) |
| `GEMINI_CHAT_MODEL` | ні | `gemini-3.5-flash-lite` | Render | модель відповідей |
| `GEMINI_EMBEDDING_MODEL` | ні | `gemini-embedding-001` | Render | модель ембедінгів |

### `.env` і `.env.example`

`.env.example` — шаблон у Git: перелік змінних без секретів. Для локальної роботи він **не обов'язковий**: усі
локальні значення мають робочі значення за замовчуванням. Файл потрібен, лише якщо треба щось змінити (наприклад,
порт `5434` зайнятий):

```powershell
Copy-Item .env.example .env      # потім змінити потрібні рядки
```

`.env` у корені читають:

- Docker Compose — автоматично (`${PASTLINE_DB_PORT:-5434}`, `${PASTLINE_DB_USER:-pastline}` у `compose.yaml`);
- бекенд — через `spring.config.import=optional:file:../.env[.properties]` (шлях відносно `backend/`, звідки
  запускається `mvnw`). Файлу немає — нічого не змінюється. Якщо запускаєте з IntelliJ, робоча папка конфігурації
  запуску має бути `backend`.

`.env` ігнорується Git (`.gitignore`). Секрети онлайн-демо в `.env` не кладуться: вони лише в панелі Render.

## Властивості застосунку

### Сервер, БД, сесія

| Властивість | Значення | Чому |
|---|---|---|
| `server.port` | `${PASTLINE_PORT:8090}` | 8080 на ноутбуці автора зайнятий |
| `spring.datasource.url` | `…localhost:${PASTLINE_DB_PORT:5434}/pastline?reWriteBatchedInserts=true` | 5432 зайнятий локальним PostgreSQL; `reWriteBatchedInserts` зливає пакет INSERT в один |
| `spring.jpa.hibernate.ddl-auto` | `validate` | схема лише через Flyway |
| `spring.jpa.open-in-view` | `false` | транзакція й ліниве завантаження не тягнуться у view-шар |
| `spring.jpa.properties.hibernate.jdbc.batch_size` | `100` | пакетна вставка під час імпорту |
| `server.servlet.session.cookie.http-only` / `same-site` | `true` / `lax` | див. [auth](features/auth.md) |
| `spring.servlet.multipart.max-file-size` | `5MB` | межа файлу імпорту |
| `management.endpoints.web.exposure.include` | `health` | назовні лише `/actuator/health` |

### AI

| Властивість | За замовчуванням | Що змінює |
|---|---|---|
| `pastline.ai.provider` | `ollama` (`gemini` у `cloud`) | який адаптер моделей створюється |
| `spring.ai.ollama.base-url` | `${OLLAMA_URL:http://localhost:11434}` | адреса Ollama |
| `spring.ai.ollama.embedding.model` | `embeddinggemma` | модель ембедінгів |
| `spring.ai.ollama.chat.model` | `qwen3:4b-instruct` | модель відповідей |
| `pastline.embedding.use-prompts` | `true` | префікси EmbeddingGemma |
| `pastline.embedding.worker.enabled` | `true` (`false` у `benchmark`) | фоновий індексатор |
| `pastline.embedding.worker.delay-ms` | `2000` (`3000` у `cloud`) | пауза між пачками індексації |
| `pastline.gemini.api-key` | `${GEMINI_API_KEY}` | ключ Gemini |
| `pastline.gemini.base-url` | `https://generativelanguage.googleapis.com/v1beta` | адреса Gemini API |
| `pastline.gemini.timeout` | `60s` | тайм-аут читання Gemini |
| `spring.http.clients.read-timeout` | `300s` | тайм-аут HTTP-клієнтів Spring (зокрема Ollama) |
| `spring.ai.retry.max-attempts` | `1` | без повторів запиту до моделі |

### Пошук і Ask

| Властивість | За замовчуванням | Що змінює |
|---|---|---|
| `pastline.search.rrf-k` | `60` | `k` у RRF |
| `pastline.search.lexical-operator` | `and` | `and` / `or`; невідоме значення зупиняє запуск |
| `pastline.ask.top-k` | `8` | записів у контексті |
| `pastline.ask.max-entry-chars` | `1500` | символів з кожного запису |
| `pastline.ask.temperature` | `0.2` (`0` у `benchmark`) | температура моделі |
| `pastline.ask.max-tokens` | `400` | межа відповіді |

### Бенчмарк (профіль `benchmark`)

| Властивість | Значення | Що змінює |
|---|---|---|
| `pastline.benchmark.task` | `search` | `search` або `ask` |
| `pastline.benchmark.corpus` | `../docs/experiments/corpus-v1/corpus.json` | корпус |
| `pastline.benchmark.queries` | `../docs/experiments/queries-v1` | папка з наборами запитів |
| `pastline.benchmark.ask-questions` | `../docs/experiments/ask-v1/questions.json` | питання Ask |
| `pastline.benchmark.output` | `../docs/experiments/results` | куди писати звіти |
| `pastline.benchmark.user` | `benchmark@pastline.local` | окремий користувач бенчмарку |
| `pastline.benchmark.warmup` / `repeats` | `5` / `5` | прогрівні запити й повтори для затримки |

Будь-яку властивість можна перевизначити при запуску: `"-Dspring-boot.run.arguments=--pastline.search.rrf-k=30"`.

## Клієнт

Клієнт не має власних змінних середовища: він завжди звертається до `/api` на своєму origin.

| Середовище | Хто пересилає `/api` | Куди |
|---|---|---|
| `npm run dev` | проксі Vite (`vite.config.ts`) | `http://127.0.0.1:8090` |
| Vercel | rewrite у `frontend/vercel.json` | `https://pastline-api.onrender.com` |

Для браузера бекенд і клієнт — один сайт, тому cookie сесії й CSRF працюють без CORS.
