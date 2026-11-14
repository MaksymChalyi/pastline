# Pastline: правила для Claude Code

Персональний щоденник із lexical, semantic і hybrid пошуком та Ask Pastline. Самодостатній продукт і практична
частина магістерської роботи.

## Джерела правди

- `README.md` — опис продукту і карта документації.
- `docs/` — документація продукту: `architecture.md`, `configuration.md`, `api.md`, `database.md`, `features/*`,
  `ai-providers.md`, `frontend.md`, `testing.md`, `deployment.md`.
- `docs/decisions.md` — журнал рішень (рішення, чому, відкинуті альтернативи, питання → відповідь).
- `docs/experiments/` — корпус, запити, сирі результати, методика.
- `docs/diploma/plan.md` — план диплома і прогрес (розділ 15); `docs/learning/labs.md` — лабораторні 01–05.
- Перед роботою прочитати розділ 15 `docs/diploma/plan.md` і документ тієї частини, яку змінюємо.

## Режими роботи

- **Лабораторні (`labs/`): весь код пише користувач.** Claude лише пояснює теорію, рецензує (`review, do not rewrite`, один мінімальний фікс на баг) і опитує. Код лаби не редагує, якщо прямо не попросили.
- **Застосунок: код пише Claude.** Спершу план, файли не редагувати до погодження. Готово лише з зеленим `mvnw verify` і `npm run check` (показати вивід). Після рішення, що змінює архітектуру чи поведінку, — новий розділ у `docs/decisions.md` і оновлення відповідного документа в `docs/`.
- **Код заморожено для диплома** (тег `thesis-freeze`): лише виправлення багів, доки користувач не скаже інакше. Тег переноситься тільки за командою користувача.
- **Експеримент:** запити, мітки, аналіз і висновки робить користувач; Claude пише раннер і графіки.
- **Текст диплома:** пише користувач; Claude допомагає зі структурою, перевіряє факти й логіку, рецензує. Усе, що стосується диплома, — лише в `docs/diploma/`.

## Архітектура (обов'язково)

- Модульний моноліт: один Spring Boot застосунок у `backend/`.
- Пакети за фічами `com.pastline.{auth,journal,importing,search,ask,ai,benchmark,shared}`, усередині звичні шари: `controller`, `service`, `repository`, `entity`, `dto`, `exception`, за потреби `security`, `config`, `client`, `parser`, `embedding`. Без інтерфейсу `Service` + `ServiceImpl`, коли реалізація одна.
- API: JSON під `/api/v1/...`, DTO (records) замість JPA-сутностей, UUID назовні, помилки `{ code, message, requestId, fieldErrors }`.
- Auth: Spring Security, серверна сесія з HttpOnly cookie, CSRF. Без JWT.
- Ізоляція: кожен запит до записів і векторів фільтрується за користувачем у SQL; `userId` береться лише із сесії.
- БД: PostgreSQL + pgvector у Docker Compose, схема лише через Flyway (зміна — новий `V<N>__*.sql`), `ddl-auto=validate`.
- AI за портами `EmbeddingModelPort` і `ChatModelPort`: домен не знає про Ollama, Gemini чи Spring AI. Провайдер обирає `pastline.ai.provider`: `ollama` локально (експеримент), `gemini` у профілі `cloud` (онлайн-демо).
- Власна таблиця `entry_embeddings`, не `PgVectorStore`: фільтр користувача в SQL, `content_hash`, модель, статус.
- Ask Pastline: показуються лише речення, усі посилання яких є серед переданих записів (`CitationValidator`).
- Тести: Testcontainers із pgvector-образом, не H2; моделі — фейки (`FakeEmbeddingModel`, `FakeChatModel`).
- Логи: ніколи не логувати текст записів, питань, відповідей, паролі, cookies.
- Lombok дозволено: `@Getter`, `@RequiredArgsConstructor`, `@NoArgsConstructor(access = PROTECTED)` для JPA, `@Slf4j`. Не використовувати `@Data`, `@EqualsAndHashCode`, `@ToString` на JPA-сутностях.
- Без мікросервісів, Kafka, черг, CQRS, JWT.
- Версії Spring Boot, Spring AI, образу pgvector, npm-пакетів звіряти з офіційною документацією.

## Стиль коду

- **Java:** Spotless (palantir-java-format). Перед комітом користувач запускає `mvnw spotless:apply`; Claude пише код у тому ж стилі.
- **Клієнт:** React + TypeScript (`strict`), Prettier (`.prettierrc`) і ESLint (`eslint.config.js`); перед комітом `npm run format` і `npm run check`. Серверні дані — TanStack Query, HTTP — лише через `src/api/client.ts`, стан фільтрів — в URL. Залежності — точні версії без `^`.
- **Коментарі й Javadoc:** коротко і лише «чому», а не «що»; без переказу коду і розділювачів `// ---`. Коментарі й документація — українською; назви в коді — англійською.
- Документацію в `docs/` оновлювати в тому самому коміті, що й код, який вона описує.

## Git

- Повідомлення комітів англійською: `type(scope): summary` (`feat`, `fix`, `test`, `docs`, `chore`, `refactor`).
- Без `Co-Authored-By` чи інших AI-трейлерів.

## Чого Claude не робить ніколи

- не комітить і не пушить без прямого дозволу;
- не додає `Co-Authored-By` чи інші AI-трейлери;
- не змінює `docs/diploma/plan.md` без прямого прохання; без прохання можна лише розділ 15;
- **не створює нових документаційних файлів** поза погодженою структурою `docs/` без прохання;
- не вигадує результатів, версій, API;
- не пише код лаб; у відповіді лаб не вписує того, чого користувач не робив;
- не ставить мітки релевантності й не пише висновки експерименту;
- не пише розділи диплома замість користувача;
- не кладе реальні особисті записи в репозиторій; не комітить `.env` і ключі; в онлайн-демо лише синтетичні дані.
