# AI-провайдери

Pastline використовує дві моделі: **ембедінги** (вектор тексту для семантичного пошуку) і **генеративну**
(відповідь Ask Pastline). Домен знає лише два порти, а конкретний провайдер обирає одна властивість.

Код: `backend/.../ai/` (`port/`, `client/`, `client/gemini/`, `exception/`).

## Порти

```java
public interface EmbeddingModelPort {
    String modelName();                                  // зберігається разом із вектором
    float[] embedQuery(String query);
    List<float[]> embedDocuments(List<EmbeddingDocument> documents);
    record EmbeddingDocument(String title, String content) {}
}

public interface ChatModelPort {
    String modelName();
    String generate(String systemPrompt, String userPrompt);
}
```

Модулі `search` і `ask` залежать тільки від цих інтерфейсів. Тому:

- провайдер замінюється без змін у пошуку й Ask;
- у тестах замість моделей стоять детерміновані фейки (`FakeEmbeddingModel`, `FakeChatModel`), і тести не
  потребують ні Ollama, ні мережі.

Це патерн «порти й адаптери» (hexagonal architecture), застосований лише там, де є реальна потреба в заміні: два
провайдери і фейки в тестах.

## Вибір провайдера

`pastline.ai.provider` = `ollama` (за замовчуванням) або `gemini`. Кожен адаптер позначений
`@ConditionalOnProperty`, тож у контексті Spring завжди рівно одна реалізація кожного порту.

| | `ollama` — локально, експеримент | `gemini` — профіль `cloud`, онлайн-демо |
|---|---|---|
| Ембедінги | `OllamaEmbeddingAdapter`: `embeddinggemma`, 768 вимірів | `GeminiEmbeddingAdapter`: `gemini-embedding-001`, `outputDimensionality = 768` |
| Запит vs документ | префікси з model card: `task: search result \| query: …` / `title: … \| text: …` | `taskType`: `RETRIEVAL_QUERY` / `RETRIEVAL_DOCUMENT` (+ `title`) |
| `modelName()` | `embeddinggemma+prompts` | `gemini-embedding-001@768` |
| Відповіді | `OllamaChatAdapter`: `qwen3:4b-instruct` | `GeminiChatAdapter`: `gemini-3.5-flash-lite` |
| Транспорт | Spring AI (`EmbeddingModel`, `ChatModel`) | власний `GeminiApi` на `RestClient` (`batchEmbedContents`, `generateContent`) |
| Ключ | не потрібен | `GEMINI_API_KEY` (без нього застосунок не стартує) |

## Чому назва моделі зберігається з вектором

Вектори різних моделей (і навіть однієї моделі з префіксами й без) лежать у різних просторах і не порівнюються.
`modelName()` записується в `entry_embeddings.model`, а пошук бере лише вектори з поточною назвою. Після зміни
провайдера чи префіксів усі вектори автоматично стають застарілими, і фоновий індексатор перераховує їх. Тому
назва включає варіант: `+prompts` для Ollama і розмірність `@768` для Gemini.

## Помилки

| Ситуація | Виняток | HTTP |
|---|---|---|
| Ollama / Gemini не відповідає (I/O, тайм-аут) | `EmbeddingUnavailableException` / `ChatUnavailableException` | 503 `EMBEDDING_UNAVAILABLE` / `AI_UNAVAILABLE` |
| Gemini повернув 4xx/5xx (у т.ч. 429 — ліміт безкоштовного рівня) | те саме; у лог — лише статус і повідомлення Gemini | 503 |
| Помилка Spring AI (Ollama: немає моделі тощо) | `ChatUnavailableException` | 503 |
| Модель повернула вектор не тієї розмірності | `IllegalStateException` | запис отримує `FAILED` |

Фоновий індексатор на 503-помилку не позначає записи як `FAILED`: вони лишаються в черзі до наступного циклу.

## Ollama (локально)

```powershell
ollama pull embeddinggemma
ollama pull qwen3:4b-instruct
```

- `qwen3:4b` у реєстрі Ollama — **thinking**-варіант: перед кожною відповіддю він «міркує» сотні токенів, що на CPU
  займає хвилини. `qwen3:4b-instruct` відповідає одразу ([рішення 22](decisions.md#d22)).
- `OllamaChatAdapter` явно передає назву моделі в опціях запиту (інакше Spring AI бере свою модель за
  замовчуванням), `numPredict` = `pastline.ask.max-tokens`, і прибирає `<think>…</think>`, якщо він є.
- Тайм-аут читання `spring.http.clients.read-timeout=300s`, повтори Spring AI вимкнено (`max-attempts=1`).

## Gemini (онлайн-демо)

- Ключ — Google AI Studio, лише у змінних середовища Render.
- Gemini 2.5 недоступні для нових ключів, тому модель відповідей — `gemini-3.5-flash-lite`; змінна
  `GEMINI_CHAT_MODEL` дозволяє замінити її без нової збірки.
- Безкоштовний рівень може використовувати запити для покращення продуктів Google. Тому в онлайн-демо лише
  синтетичні дані ([рішення 25](decisions.md#d25)).
- Числа експерименту отримано **лише** з Ollama: онлайн-демо використовує інші моделі.

## Як додати провайдера

1. Адаптер `implements EmbeddingModelPort` і/або `ChatModelPort` з
   `@ConditionalOnProperty(name = "pastline.ai.provider", havingValue = "<назва>")`.
2. Розмірність ембедінгів — 768 (як `vector(768)` у схемі) або нова міграція.
3. Мережеві помилки → `EmbeddingUnavailableException` / `ChatUnavailableException`.
4. Тест адаптера без мережі (як `GeminiAdaptersTest` з `MockRestServiceServer`).
