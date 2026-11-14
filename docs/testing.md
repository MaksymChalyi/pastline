# Тестування

112 автоматичних тестів бекенду (JUnit 5). Більшість — інтеграційні: справжній Spring-контекст, справжній
PostgreSQL з pgvector у Docker (Testcontainers) і HTTP-запити через MockMvc. Моделі AI замінено детермінованими
фейками.

## Запуск

```powershell
cd backend
.\mvnw spotless:apply     # форматування (palantir-java-format)
.\mvnw verify             # усі тести; потрібен запущений Docker
```

Клієнт:

```powershell
cd frontend
npm run check             # prettier --check + eslint + tsc + vite build
```

Автоматичних тестів клієнта немає: перевірка — типи TypeScript, ESLint і ручний прохід сценаріїв у браузері.

## Чому Testcontainers, а не H2

Пошук тримається на можливостях PostgreSQL, яких немає в H2: `tsvector`, GIN, `websearch_to_tsquery`, тип
`vector` і оператор `<=>`. Тест на H2 перевіряв би іншу базу. `TestcontainersConfiguration` піднімає той самий
образ `pgvector/pgvector:0.8.2-pg17`, що й `compose.yaml`, а `@ServiceConnection` сам підставляє URL, логін і
пароль. Flyway накочує ті самі міграції `V1`–`V5`.

Один контейнер на весь прогін: Spring кешує контекст між тестовими класами з однаковою конфігурацією.

## Фейки замість моделей

| Фейк | Що робить | Навіщо |
|---|---|---|
| `FakeEmbeddingModel` | «мішок слів»: кожне слово додає одиницю в позицію `hash(слово) mod 767` вектора 768 | спільні слова → близькі вектори; результат детермінований, без Ollama |
| `FakeChatModel` | повертає заготовлену відповідь (`answerWith`), запам'ятовує промпти, вміє бути «недоступним» | перевірка Ask без моделі: вигадані посилання, 503, вміст промпту |

Обидва — `@Primary` біни в `TestcontainersConfiguration`, тож замінюють справжні адаптери в усіх тестах.
`TestSessions.newUser(mvc)` реєструє нового користувача і повертає його сесію — кожен тест працює зі своїм
користувачем і не залежить від інших.

## Що покрито

| Область | Класи | Тестів | Ключові сценарії |
|---|---|---|---|
| Контекст і БД | `PastlineBackendApplicationTests`, `NonWebContextTest`, `DatabaseSetupTest` | 4 | контекст стартує; профіль без веб-сервера; міграції Flyway, розширення `vector`, GIN-індекс використовується |
| Auth | `AuthApiTest`, `CsrfFlowTest` | 12 | реєстрація, пароль лише як хеш, дублікат email без урахування регістру, однакова відповідь на невідомий email і неправильний пароль, сесія після входу, вихід, CSRF-цикл як у браузері |
| Записи | `JournalApiTest` | 11 | CRUD, валідація, дата в майбутньому, дублікат, **ізоляція: користувач B отримує 404 на GET/PUT/DELETE запису A** |
| Імпорт | `ImportApiTest` | 12 | JSON і Markdown, `dryRun` за замовчуванням, дублікати у файлі й з БД, помилки окремих записів, зламані файли, ізоляція, відтворюваний корпус (SHA-256) імпортується повністю |
| Лексичний пошук | `SearchApiTest`, `LexicalOperatorTest`, `LexicalOrSearchApiTest` | 17 | ранжування, синтаксис websearch, ізоляція, валідація параметрів, оператор OR |
| Семантичний пошук | `SemanticSearchApiTest`, `EmbeddingPromptsTest` | 11 | індексація, застарілий вектор після редагування, `pendingCount`, 503 без моделі, `reindex`, префікси |
| Гібридний пошук | `HybridSearchApiTest`, `RrfFusionTest` | 12 | формула RRF, рівні бали, `debug`, 503 без тихого відкату на lexical |
| Ask Pastline | `AskApiTest`, `CitationValidatorTest` | 16 | `NO_EVIDENCE` без виклику моделі, **«отруєна» відповідь з вигаданим `[E9]` прибирається**, екранування тегів, чужі записи не в контексті, 503 |
| Gemini | `GeminiAdaptersTest` | 6 | формат REST-запитів і відповідей через `MockRestServiceServer`, без мережі |
| Бенчмарк | `BenchmarkMetricsTest`, `BenchmarkServiceTest`, `AskBenchmarkServiceTest` | 11 | Hit@k, MRR, нормований Recall, звіт, оцінка Ask |

## Як писати новий тест

```java
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SomethingApiTest {
    @Autowired MockMvc mvc;

    @Test
    void ownerOnly() throws Exception {
        MockHttpSession alice = TestSessions.newUser(mvc);
        // ... дії від імені alice, перевірки jsonPath(...)
    }
}
```

- Змінні запити в MockMvc — з `.with(csrf())`, інакше 403.
- Кожен тест створює власного користувача: тести не залежать від порядку.
- Не логувати й не класти в тести реальні записи — лише синтетичний текст.
