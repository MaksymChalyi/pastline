# Початок роботи

Як запустити Pastline локально з нуля. Команди для Windows PowerShell з кореня репозиторію; на macOS/Linux
замість `.\mvnw` — `./mvnw`.

## Що потрібно

| Інструмент | Версія | Навіщо |
|---|---|---|
| JDK | 21 | бекенд |
| Docker Desktop | актуальна | PostgreSQL + pgvector; Testcontainers у тестах |
| Node.js | ≥ 22.12 | клієнт (Vite 8) |
| Ollama | актуальна | ембедінги й відповіді локально |

Maven окремо не потрібен: у `backend/` є Maven Wrapper (`mvnw`).

## 1. База даних і моделі

```powershell
docker compose up -d                 # PostgreSQL 17 + pgvector на порту 5434
ollama pull embeddinggemma           # ембедінги, 768 вимірів
ollama pull qwen3:4b-instruct        # відповіді Ask Pastline (не qwen3:4b — це thinking-варіант)
```

Порт 5434, а не стандартний 5432, щоб не конфліктувати з локально встановленим PostgreSQL. Змінити порт можна в
`.env` (див. [configuration.md](configuration.md#env-і-envexample)).

## 2. Бекенд

```powershell
cd backend
.\mvnw spring-boot:run               # http://localhost:8090
```

Під час старту Flyway створює схему. Перевірка: `http://localhost:8090/actuator/health` → `{"status":"UP"}`.

Без Ollama застосунок теж стартує: записи, імпорт і лексичний пошук працюють, а семантичний / гібридний пошук і
Ask повертають 503, доки модель недоступна.

## 3. Клієнт

```powershell
cd frontend
npm install
npm run dev                          # http://localhost:5173
```

Vite пересилає `/api` на `127.0.0.1:8090`. Щоб відкрити з телефона в тій самій Wi-Fi: `npm run dev -- --host` і
адреса з виводу (`http://192.168.…:5173`).

## 4. Перший сценарій

1. Зареєструватися на `/register` (дані лишаються лише в локальній БД).
2. «Імпорт» → файл `docs/experiments/corpus-v1/corpus.json` (520 синтетичних записів) → перевірка → імпорт.
3. Зачекати, поки фоновий індексатор порахує вектори: сторінка пошуку показує, скільки записів ще в черзі.
4. «Пошук»: той самий запит у трьох режимах, наприклад `купував ОВДП` або `my brokerage index fund purchase`.
5. «Запитати»: «Як пройшов мій перший напівмарафон?» — відповідь із посиланнями на записи.

## 5. Тести і форматування

```powershell
cd backend
.\mvnw spotless:apply                # форматування Java
.\mvnw verify                        # 112 тестів; Testcontainers сам піднімає PostgreSQL у Docker

cd ..\frontend
npm run check                        # Prettier, ESLint, TypeScript, збірка
```

Детальніше — [testing.md](testing.md).

## 6. Ручні HTTP-запити

- **Postman:** Import → `backend/http/pastline.postman_collection.json`. Скрипт колекції перед кожним
  POST/PUT/DELETE бере свіжий CSRF-токен; cookie сесії Postman зберігає сам. Порядок: `Health` → `Auth/Register`
  → `Auth/Login` → решта. Для імпорту обрати файл у Body → form-data → `file`. Для онлайн-демо змінна колекції
  `baseUrl` = `https://pastline.vercel.app` (там працює все під `/api`, але не `/actuator/health`).
- **IntelliJ HTTP Client:** `backend/http/gate1.http`.

## Що далі

- Як влаштовано застосунок — [architecture.md](architecture.md).
- Як відтворити експеримент — [experiments/README.md](experiments/README.md#як-відтворити).
- Онлайн-демо — [deployment.md](deployment.md).

## Типові проблеми

| Симптом | Причина | Що зробити |
|---|---|---|
| Бекенд не стартує: `Connection refused` до `5434` | БД не запущена | `docker compose up -d`, перевірити `docker compose ps` |
| Порт `8090` зайнятий | інший процес | `PASTLINE_PORT=8091` у `.env` і той самий порт у проксі `vite.config.ts` |
| 502 у клієнті на `/api/...` | бекенд не запущений | запустити бекенд |
| 503 `EMBEDDING_UNAVAILABLE` | Ollama не запущена або немає моделі | `ollama list`, `ollama pull embeddinggemma` |
| Ask відповідає кілька хвилин | використовується `qwen3:4b` (thinking) | `ollama pull qwen3:4b-instruct`, перевірити `spring.ai.ollama.chat.model` |
| Тести: `Could not find a valid Docker environment` | Docker Desktop не запущений | запустити Docker |
