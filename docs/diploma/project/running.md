# Запуск і відтворення

Команди для Windows PowerShell з кореня репозиторію `pastline`. Потрібно: JDK 21, Docker Desktop, Node.js ≥ 22.12,
Ollama.

## 1. Локальне середовище

```powershell
docker compose up -d                 # PostgreSQL 17 + pgvector на порту 5434
ollama pull embeddinggemma           # ембедінги, 768 вимірів
ollama pull qwen3:4b-instruct        # відповіді Ask Pastline (не qwen3:4b — це thinking-варіант)
```

## 2. Бекенд

```powershell
cd backend
.\mvnw spring-boot:run               # http://localhost:8090
```

Перевірка: `http://localhost:8090/actuator/health` → `{"status":"UP"}`.

## 3. Клієнт

```powershell
cd frontend
npm install
npm run dev                          # http://localhost:5173, /api проксується на 127.0.0.1:8090
npm run dev -- --host                # те саме, але доступно з телефона в тій самій Wi-Fi
```

## 4. Тести

```powershell
cd backend
.\mvnw spotless:apply
.\mvnw verify                        # Testcontainers сам піднімає PostgreSQL + pgvector у Docker
```

## 5. Експеримент

Ноутбук на зарядці, сон вимкнено (`powercfg /change standby-timeout-ac 0`), інші важкі процеси закрито.

```powershell
cd backend
# пошук, основний прогін (оператор AND)
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=benchmark"
# пошук, абляція (оператор OR)
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=benchmark" "-Dspring-boot.run.arguments=--pastline.search.lexical-operator=or"
# Ask Pastline (H5), ~10 хв
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=benchmark" "-Dspring-boot.run.arguments=--pastline.benchmark.task=ask"
```

Звіти з'являються в `docs/experiments/results/`. Рисунки й таблиці: `python docs/diploma/tools/make_figures.py`.

## 6. Онлайн-демо

| Частина | Сервіс | Налаштування |
|---|---|---|
| Клієнт | Vercel | Root Directory `frontend`; `frontend/vercel.json` пересилає `/api/*` на бекенд |
| Бекенд | Render (Free, Docker) | Root Directory `backend`, `backend/Dockerfile`, Health Check `/actuator/health`, профіль `cloud` вмикається в Dockerfile |
| БД | Neon (Free) | пряме підключення (без `-pooler`), `sslmode=require` |
| AI | Gemini API | ключ з Google AI Studio |

Змінні середовища бекенду (лише в Render, ніколи в Git):

| Змінна | Приклад |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>/neondb?sslmode=require&reWriteBatchedInserts=true` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | з панелі Neon |
| `GEMINI_API_KEY` | з Google AI Studio |
| `GEMINI_CHAT_MODEL`, `GEMINI_EMBEDDING_MODEL` | необов'язково; за замовчуванням `gemini-3.5-flash-lite`, `gemini-embedding-001` |

Обмеження безкоштовних планів: Render засинає після 15 хв без запитів (перший запит ~1 хв), сесії губляться при
перезапуску. Безкоштовний рівень Gemini може використовувати запити для покращення продуктів Google — в онлайн-демо
лише синтетичний корпус.
