# Лаба 04: Spring AI

Дата: 11.11.2026 · Час: ~2 год

**Середовище:** Java 21 (Corretto 21.0.10), Maven (wrapper 3.10.0), IntelliJ IDEA 2024.3. Spring Boot 4.1.2, Spring AI 2.0.1 (`spring-ai-starter-model-ollama`), `spring-boot-starter-webmvc`, Tomcat 11.0.27. Ollama 0.40.15, моделі `qwen3:4b` і `embeddinggemma` на CPU.

## Мета

Хотів побачити, що саме Spring AI ховає порівняно з лабами 02–03, де HTTP до Ollama, DTO, обробку помилок і cosine я писав сам. Тут те саме (чат і семантичний пошук по 30 записах) зроблено через `ChatClient` і `EmbeddingModel`, а cosine лишився мій.

## Як запустити

```powershell
cd labs/04-spring-ai-demo
.\mvnw.cmd test              # контекст піднімається без Ollama: індексація на старті вимкнена в тесті
.\mvnw.cmd spring-boot:run   # порт 8081
```

Запити лежать у `requests.http` (▶ в IDEA): чат і три запити з лаби 3 (Q02, Q07, Q10) з очікуваними результатами.

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=qwen3:4b
spring.ai.ollama.embedding.model=embeddinggemma
server.port=8081
```

## Як це працює

```text
старт ──▶ ApplicationReadyEvent ──▶ SearchService
             entries.json (classpath) ──▶ EmbeddingModel.embed(List<String>) ──▶ Map<id, float[]>

POST /demo/search {query} ──▶ EmbeddingModel.embed(query) ──▶ мій Cosine з кожним записом ──▶ top-5 {id, text, score}
GET  /demo/chat?q=...     ──▶ ChatClient.prompt().user(q).options(OllamaChatOptions...disableThinking()).call().content()
```

- `chat/ChatController`: `ChatClient` будується з автоконфігурованого `ChatClient.Builder`;
- `search/SearchService`: читає записи, рахує вектори один раз після старту і пише в лог час. Пошук: embedding запиту, cosine, сортування, top-5. Індексацію можна вимкнути властивістю `demo.index-on-startup=false`, як у `ApplicationTests`;
- `search/SearchController`: `POST /demo/search`, у відповіді `tookMs` і `hits`;
- `search/Cosine`: перенесений з лаби 03 без змін.

## Результати

| Що | Значення |
|---|---|
| Результати збіглися з Лабою 03? | **так, повністю**: для Q02, Q07, Q10 збіглися всі 5 позицій і cosine до 3-го знака (E03 0.414, E20 0.401, E12 0.537 / E19 0.489) |
| Час старту застосунку | 6,1 с до `Started Application` + **10,3 с** індексація 30 записів (`Проіндексовано 30 записів, розмірність 768, за 10334 мс`) |
| Час `/demo/search`, мс | **100–189** у звичайному режимі; перший запит після чату 3 897 мс |
| Час `/demo/chat` | **256 с** (чистий запуск); 447 с, коли запит стояв у черзі Ollama за попереднім |

| Q | Лаба 03 top-5 | Лаба 04 top-5 |
|---|---|---|
| Q02 | E03 .414, E13 .368, E25 .306, E14 .298, E19 .280 | те саме |
| Q07 | E20 .401, E15 .378, E25 .354, E29 .342, E13 .319 | те саме |
| Q10 | E12 .537, E19 .489, E29 .419, E25 .374, E13 .371 | те саме |

### Що побачив у чаті

- **`disableThinking()` не прибрав роздуми.** В обох запусках `answer` починається з кількох кілобайтів міркувань англійською і закінчується `</think>` та однією фразою-відповіддю. Те саме було в лабах 01–02 з `"think": false` у сирому HTTP, тож Spring AI тут нічого не змінює.
- **Мова відповіді випадкова.** На те саме питання українською один раз модель відповіла англійською, другий — українською.
- **Час визначає модель.** 256 с через Spring AI проти 283 с через мій `HttpClient` у лабі 02: накладні витрати фреймворку на тлі генерації не видно.

## Що Spring AI робить за мене і чого не робить

| Spring AI робить за мене | Spring AI НЕ робить |
|---|---|
| 1. HTTP до Ollama, JSON, DTO запиту й відповіді: замість `EmbeddingClient`, `EmbedRequest`, `EmbedResponse` з лаби 03 один виклик `embeddingModel.embed(...)` | 1. Не шукає: схожість, сортування, top-k і метрики пишу сам. Без vector store вектори живуть у `Map` у пам'яті й перераховуються на кожному старті |
| 2. Конфігурацію: URL і моделі задаються в `application.properties`, а біни `EmbeddingModel` і `ChatClient.Builder` створює автоконфігурація | 2. Не гарантує поведінку моделі: `disableThinking()` передається, але `qwen3:4b` однаково пише роздуми в `content`, і `</think>` ніхто не вирізає |
| 3. Єдиний API над провайдером: код контролерів не знає, що це Ollama. Перехід на OpenAI — інший стартер і властивості | 3. Не керує мовою й форматом відповіді: це робиться системним промптом і перевіркою результату в моєму коді |
| 4. Опції моделі через типізований builder (`OllamaChatOptions`), а не вручну в JSON | |
| 5. Пакетний виклик `embed(List<String>)`: вектори повертаються в тому ж порядку, що й тексти | |

## Що пішло не так і як виправив

1. **Майстер Spring Boot в IntelliJ IDEA 2024.3 не створив проєкт.** Папка лишалась порожньою. Згенерував проєкт напряму на start.spring.io (zip з Boot 4.1.2, Web, Ollama) і відкрив `pom.xml` як проєкт.
2. **Artifact `04-spring-ai-demo` не приймається:** Maven artifact не може починатися з цифри. Назва папки лишилась `04-spring-ai-demo`, artifact — `lab04-spring-ai-demo`.
3. **IDEA вважала модуль Java 1.5** (`release version 5 not supported`), хоча Maven збирав проєкт нормально. IDEA не до кінця імпортувала налаштування з `pom.xml` (`java.version` 21 з parent Spring Boot 4). Запускав через Maven (`mvnw spring-boot:run`, у IDEA — делегування build/run у Maven).
4. **Порт 8080 зайнятий** системною службою `ApplicationWebServer.exe` (сесія Services). Не чіпав її, переніс застосунок на `server.port=8081`.
5. **`ReadTimeoutException` у HTTP-клієнті IDEA** на чаті: клієнт чекає ~хвилину, а відповідь qwen3 на CPU йде хвилини. Додав `# @timeout 600` у `requests.http`. Запит, на якому клієнт від'єднався, сервер і Ollama все одно догенерували, тому наступний чекав у черзі (447 с замість 256 с).
6. **`contextLoads` ходив би в Ollama**, бо індексація стартує після запуску застосунку. Зробив її вимикною властивістю, у тесті вона вимкнена.

## Висновки

1. Spring AI прибирає шар «HTTP до моделі»: клієнт, DTO, серіалізацію, конфігурацію. У лабі 03 це було ~100 рядків, тут один виклик.
2. Результати пошуку збіглися з лабою 03 до третього знака: фреймворк викликає ту саму модель, тому вектори ті самі. Мій cosine перенісся без змін.
3. Пошук, ранжування й оцінку якості Spring AI за мене не робить (без vector store). Це залишається моїм кодом і моїм експериментом.
4. Опція `disableThinking()` не гарантує результату: `qwen3:4b` однаково пише роздуми у відповідь. Чистити `content` і задавати мову доведеться самому.
5. Повільність визначає модель на CPU (~4 хв на коротку відповідь), а не фреймворк. Для UI потрібні великі таймаути, потокова відповідь і розуміння, що запити до Ollama стоять у черзі.
6. Embedding запиту займає 100–190 мс, але перший виклик після чату — ~3,9 с. Імовірно, модель перезавантажувалась у пам'ять після qwen3, цього я не перевіряв.

## Що це дає Pastline і диплому

- **Порт `EmbeddingModelPort` з адаптером на Spring AI.** Домен не залежить від Spring AI, а адаптер — це кілька рядків навколо `EmbeddingModel`. Профіль `cloud` з OpenAI міняє стартер і властивості, а не доменний код.
- **Пошук свій.** Фільтр за користувачем у SQL, ранжування й гібрид Spring AI не дасть, і за правилами проєкту в Pastline власна таблиця embeddings замість `PgVectorStore`.
- **Ask Pastline:** системний промпт з мовою відповіді, вирізання роздумів до `</think>`, потокова відповідь, таймаути в хвилинах.
- **Розділ 2 диплома (вибір технологій):** порівняння «власний клієнт (лаби 02–03) проти Spring AI (лаба 04)» з таблицею «робить / не робить» і тим, що результати однакові.

## Питання, на які відповідаю без нотаток

**Що саме ховає `EmbeddingModel.embed(...)`?**
Формування JSON-запиту до `/api/embed`, HTTP-виклик, розбір відповіді й перетворення її на `float[]`. URL і модель беруться з `application.properties`. У лабі 03 усе це було в `EmbeddingClient` і двох DTO.

**Чому cosine і top-5 лишилися моїми?**
`EmbeddingModel` лише перетворює текст на вектор, порівнювати вектори він не вміє. Пошук у Spring AI дає `VectorStore`, але без нього (і в Pastline, де таблиця embeddings власна) схожість і ранжування пишу сам.

**Що зміниться, якщо перейти з Ollama на OpenAI?**
Код контролерів і сервісу — ні: вони працюють з інтерфейсами `ChatClient` і `EmbeddingModel`. Змінюються стартер, властивості й модель. Але вектори іншої моделі несумісні з уже збереженими, тому всі записи треба переіндексувати (див. лабу 03).

**Чому `disableThinking()` не допоміг?**
Spring AI лише передає опцію в Ollama. Чи виконає її модель, залежить від Ollama і самої моделі: `qwen3:4b` в обох випадках, і через сирий HTTP, і через Spring AI, писала роздуми в `content`. Тому захист має бути в моєму коді, який обробляє відповідь.

## Матеріали

- Spring AI, Ollama chat: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
- Spring AI, Ollama embeddings: https://docs.spring.io/spring-ai/reference/api/embeddings/ollama-embeddings.html
- Spring AI, ChatClient: https://docs.spring.io/spring-ai/reference/api/chatclient.html
- Spring Initializr: https://start.spring.io
