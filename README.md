# Pastline

Персональний щоденник, у якому можна знайти запис за змістом, а не лише за точними словами. Три режими пошуку
по власних записах — **лексичний**, **семантичний** і **гібридний** — і **Ask Pastline**: відповідь на питання
лише з ваших записів, де кожне речення має посилання на запис-джерело.

**Онлайн-демо:** https://pastline.vercel.app (лише синтетичні дані; перший запит після простою — до хвилини).

## Можливості

- Записи щоденника: створення, редагування, стрічка з фільтром дат.
- Імпорт JSON і Markdown з попередньою перевіркою; повторний імпорт не створює дублікатів.
- Пошук трьома режимами через один API:
  - лексичний — повнотекстовий пошук PostgreSQL;
  - семантичний — ембедінги і косинусна відстань у pgvector;
  - гібридний — злиття обох списків через Reciprocal Rank Fusion.
- Ask Pastline: гібридний пошук → локальна LLM → перевірка посилань; речення з посиланням на запис, якого модель
  не отримувала, не показується.
- Строга ізоляція користувачів: кожен запит фільтрується за власником у SQL.
- Відтворюваний бенчмарк пошуку й Ask Pastline на синтетичному корпусі.

## Стек

| Частина | Технології |
|---|---|
| Бекенд | Java 21, Spring Boot 4.1 (модульний моноліт), Spring Security (серверна сесія + CSRF), Spring Data JPA, JDBC |
| AI | Spring AI 2.0 + Ollama (`embeddinggemma`, `qwen3:4b-instruct`) локально; Gemini API в онлайн-демо |
| БД | PostgreSQL 17 + pgvector 0.8.2, Flyway |
| Клієнт | React 19, TypeScript, Vite 8, react-router 7, TanStack Query 5 |
| Тести і якість | JUnit 5, Testcontainers, Spotless; Prettier, ESLint |
| Інфраструктура | Docker Compose; Vercel + Render + Neon для демо |

## Швидкий старт

Потрібно: JDK 21, Docker Desktop, Node.js ≥ 22.12, Ollama.

```powershell
docker compose up -d
ollama pull embeddinggemma
ollama pull qwen3:4b-instruct

cd backend
.\mvnw spring-boot:run                   # http://localhost:8090
```

В іншому терміналі, з кореня репозиторію:

```powershell
cd frontend
npm install
npm run dev                              # http://localhost:5173
```

Далі: зареєструватися, імпортувати `docs/experiments/corpus-v1/corpus.json` і спробувати пошук. Детально —
[docs/getting-started.md](docs/getting-started.md).

## Документація

| Документ | Про що |
|---|---|
| [Початок роботи](docs/getting-started.md) | встановлення, перший запуск, типові проблеми |
| [Архітектура](docs/architecture.md) | модулі, залежності, ключові рішення коротко |
| [Конфігурація](docs/configuration.md) | профілі, змінні середовища, `.env`, властивості |
| [REST API](docs/api.md) | ендпоінти і коди помилок |
| [База даних](docs/database.md) | схема, обмеження, міграції |
| Функції | [автентифікація](docs/features/auth.md) · [записи](docs/features/journal.md) · [імпорт](docs/features/import.md) · [пошук](docs/features/search.md) · [Ask Pastline](docs/features/ask.md) |
| [AI-провайдери](docs/ai-providers.md) | порти, Ollama, Gemini |
| [Клієнт](docs/frontend.md) | структура React-застосунку, кеш, маршрути |
| [Тестування](docs/testing.md) | Testcontainers, фейки моделей, що покрито |
| [Деплой](docs/deployment.md) | онлайн-демо на безкоштовних планах |
| [Журнал рішень](docs/decisions.md) | що вирішили, чому і що відкинули |
| [Експеримент](docs/experiments/README.md) | методика, дані, як відтворити, [таблиці результатів](docs/experiments/results-tables.md) |
| [Навчання](docs/learning/) | [ключові поняття](docs/learning/concepts.md), [план вивчення коду](docs/learning/code-tour.md), [лабораторні](docs/learning/labs.md) |
| [Диплом](docs/diploma/README.md) | матеріали магістерської роботи: план, питання захисту, рисунки |

## Структура репозиторію

```
backend/    Spring Boot: com.pastline.{auth, journal, importing, search, ask, ai, benchmark, shared}
frontend/   React + TypeScript
docs/       документація; docs/diploma/ — матеріали диплома
labs/       навчальні лабораторні 01–05
scripts/    генератор синтетичного корпусу
```

## Приватність

Справжні записи щоденника ніколи не потрапляють у репозиторій: ні в код, ні в тести, ні в скриншоти чи додатки
диплома. Усі дані в репозиторії синтетичні. Секрети — лише в змінних середовища; `.env` ігнорується Git,
`.env.example` містить тільки назви змінних і локальні значення за замовчуванням. Логи не містять тексту записів,
питань, паролів і cookie.

## Контекст

Pastline створено як практичну частину магістерської роботи «Інтелектуальна система обліку та інтелектуального
пошуку персональних записів» (робоча назва). Продукт самодостатній: матеріали диплома винесено в `docs/diploma/`.
