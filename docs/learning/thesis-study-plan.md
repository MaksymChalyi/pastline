# План вивчення: читати зверху вниз

Один маршрут, без прив'язки до дат: іду по кроках по черзі, у власному темпі. Крок закритий, коли відповідаю на
його питання вголос, не дивлячись у код. Біля кожного кроку — орієнтовна кількість занять і годин; кроки 1–8 разом —
близько 30 годин. Якщо часу мало, можна брати два кроки за заняття, але порядок не змінювати.

Правило: **розділ записки читаю лише після того, як розібрав відповідний код**, і правки вношу одразу в
`docs/diploma/tools/thesis/*.js` (потім `npm run build` у цій папці).

Постійно відкриті під рукою:
- [`code-tour.md`](code-tour.md) — сесії з кодом, точками зупинки й питаннями;
- [`concepts.md`](concepts.md) — теорія коротко;
- записка — [`../diploma/text/ФКНТ_2026_F3_ЧалийМВ_Диплом.docx`](../diploma/text/);
- журнал рішень — [`../decisions.md`](../decisions.md).

Відео без прямого посилання відкриваються пошуком YouTube за назвою. Українською чи російською коротких перевірених
відео з цих тем немає — в англійських вмикати автоматичні субтитри.

---

## Крок 1. Загальна картина (1 заняття, 3–4 год)

1. Записка: **Вступ**, потім підрозділи **1.4** (дослідницьке питання, H1–H5) і **1.5** (мета, задачі).
2. [`../architecture.md`](../architecture.md) і рис. 3.1 у записці.
3. [`../getting-started.md`](../getting-started.md): запустити БД, Ollama, бекенд, клієнт.
4. `code-tour.md` → **Підготовка** і **Сесія 1** (Postman-колекція зверху вниз).
5. Відео: «Vector Databases Simply Explained! (Embeddings and Indexes)», AssemblyAI, 5 хв —
   https://www.youtube.com/watch?v=dN0lsF2cvm4

**Питання:** що робить система одним абзацом? які три режими пошуку? що перевіряє кожна з H1–H5?

## Крок 2. Дані й безпека (1 заняття, 2 год)

1. `code-tour.md` → **Сесія 2** (міграції, шлях запиту, помилки) і **Сесія 3** (безпека).
2. [`../database.md`](../database.md), [`../features/journal.md`](../features/journal.md),
   [`../features/auth.md`](../features/auth.md).
3. `concepts.md` → розділ **8** (сесія, cookie, CSRF) і **10** (Flyway, Testcontainers).
4. Статті:
   - Baeldung, «A Guide to CSRF Protection in Spring Security» — https://www.baeldung.com/spring-security-csrf
   - Baeldung, «CSRF With Stateless REST API» — https://www.baeldung.com/csrf-stateless-rest-api
5. Записка: **3.3** (модель даних), **3.4** (автентифікація, ізоляція).

**Питання:** чому сесія, а не JWT? як працює XSRF-TOKEN між React і Spring? чому чужий запис дає 404, а не 403?

## Крок 3. Імпорт і лексичний пошук (1 заняття, 2 год)

1. `code-tour.md` → **Сесія 4** (імпорт) і перша половина **Сесії 5** (лексичний пошук).
2. [`../features/import.md`](../features/import.md), [`../features/search.md`](../features/search.md), розділ 1.
3. `concepts.md` → розділ **1** (повнотекстовий пошук PostgreSQL).
4. Відео: Computerphile, «How Search Engines Treat Data», 10 хв (TF-IDF, стемінг) —
   https://www.youtube.com/results?search_query=Computerphile+How+Search+Engines+Treat+Data
5. Стаття: Baeldung, статті з тегом Testcontainers («DB Integration Tests with Spring Boot and Testcontainers») —
   https://www.baeldung.com/category/testing/tag/testcontainers
6. Записка: **2.1.1** (лексичний пошук, формули 2.1–2.3), **3.5.1**.

**Питання:** що таке tsvector і GIN? чому «пробіжка» і «пробіжку» не збігаються? чим AND відрізняється від OR?

## Крок 4. Семантичний і гібридний пошук (1–2 заняття, 2–3 год)

1. Друга половина `code-tour.md` → **Сесія 5**.
2. [`../features/search.md`](../features/search.md), розділи 2–4.
3. `concepts.md` → розділи **2** (ембедінги), **3** (pgvector), **4** (RRF).
4. Відео: James Briggs, «3 Vector-based Methods for Similarity Search (TF-IDF, BM25, SBERT)» —
   https://www.youtube.com/results?search_query=James+Briggs+3+Vector-based+Methods+for+Similarity+Search
5. Статті:
   - Habr, Selectel, «Векторный поиск внутри PostgreSQL: что умеет и где может пригодиться pgvector» —
     https://habr.com/ru/post/920824
   - ParadeDB, «Hybrid Search in PostgreSQL: The Missing Manual» —
     https://paradedb.com/blog/hybrid-search-in-postgresql-the-missing-manual
6. Записка: **2.1.2–2.1.3** (формули 2.4–2.5, таблиця 2.2), **3.5.2–3.5.3**, лістинги 3.1–3.2, додаток А.1 і А.4.

**Питання:** чому немає HNSW? коли вектор вважається застарілим? порахувати RRF для рангів 1 і 3 на папері.

## Крок 5. Ask Pastline (1 заняття, 2 год)

1. `code-tour.md` → **Сесія 6**.
2. [`../features/ask.md`](../features/ask.md), [`../ai-providers.md`](../ai-providers.md).
3. `concepts.md` → розділи **6** (RAG) і **7** (prompt injection).
4. Відео:
   - IBM Technology, «What is Retrieval-Augmented Generation (RAG)?», ~6 хв —
     https://www.youtube.com/watch?v=T-D1OfcDW1M
   - Computerphile, «Generative AI's Greatest Flaw» (непряма ін'єкція інструкцій) —
     https://www.youtube.com/watch?v=rAEqP9VEhe8
5. Статті:
   - DOU, «Як вибрати embedding-модель для української, коли жоден бенчмарк вам не підходить» —
     https://dou.ua/goto/5aON
   - Baeldung, «Create a ChatGPT Like Chatbot With Ollama and Spring AI» —
     https://www.baeldung.com/spring-ai-ollama-chatgpt-like-chatbot (версія Spring AI у статті старіша — звіряти з кодом)
6. Записка: **2.1.4**, **2.5**, **3.6**, лістинг 3.3, додаток А.2–А.3, рис. 3.5.

**Питання:** навіщо E1…E8 замість UUID? що гарантує CitationValidator і чого не гарантує? чому qwen3:4b-instruct?

## Крок 6. Експеримент, клієнт, деплой (1–2 заняття, 2–3 год)

1. `code-tour.md` → **Сесії 7, 8, 9**.
2. [`../experiments/README.md`](../experiments/README.md), [`../experiments/results-tables.md`](../experiments/results-tables.md),
   [`../frontend.md`](../frontend.md), [`../deployment.md`](../deployment.md), [`../testing.md`](../testing.md).
3. `concepts.md` → розділи **5** (метрики), **9** (архітектура), «React для Java-розробника».
4. Статті:
   - DOU, «Як оцінити RAG-систему: від тестового датасету до порівняння пошуку. Частина 1» —
     https://dou.ua/goto/Ige0
   - Habr, Java + PostgreSQL/pgvector + Ollama + Testcontainers — https://habr.com/ru/post/915348
5. Записка: **2.1.5** (формули 2.6–2.8), **3.7**, **3.8**, **3.9**.

**Питання:** чим Hit@1, MRR@10 і Recall@10 відрізняються? як обчислено p95? як повторити прогін і довести, що дані ті самі?

## Крок 7. Записка повністю, з правками (2 заняття по 4–5 год)

Читаю всю записку від титулу до додатків. На кожен абзац: розумію? можу показати в коді? є джерело або число?

| Розділ | Звіряти з |
|---|---|
| 1. Аналоги, вимоги, гіпотези | [`../decisions.md`](../decisions.md); таблиця 1.2 — з функціями, які реально є в коді |
| 2. Методи і засоби | `concepts.md`, `decisions.md`; на кожну таблицю порівняння — «чому не альтернатива» |
| 3.1. Пілотні дослідження | `labs/0X-…/ANSWERS.md` — числа звідти |
| 3.10. Результати | [`../experiments/results-tables.md`](../experiments/results-tables.md) — кожне число знайти в таблиці |
| Висновки щодо H1–H5 | перевірити, що згоден з кожним, і переписати своїми словами, де формулювання не моє |

## Крок 8. Оформлення і захист (3–4 заняття по 2 год)

1. Жовті поля: керівник, нормоконтролер, дати, наказ, апробація, моделі й дати ChatGPT/DeepSeek, сторінки в рефераті.
2. Word: оновити зміст, перевірити розриви таблиці Б.1, нумерацію рисунків і таблиць.
3. [`../diploma/defense-qa.md`](../diploma/defense-qa.md) — відповідати вголос.
4. Найімовірніші питання комісії — «від зубів»:
   - Чому гібридний пошук не дав приросту і що б ви змінили? (стемінг, зважений RRF, окремий набір для налаштування)
   - Чому немає індексу HNSW? (точний пошук, фільтр за власником, сотні–тисячі записів)
   - Що гарантує перевірка посилань і чого не гарантує?
   - Чи можна довіряти результатам на синтетичному корпусі і запитах від мовних моделей? (3.10.5)
   - Як система захищає дані користувача? (user_id у кожному SQL, 404 замість 403, локальні моделі)

**Після кроку 8 — записка готова до здачі.**

## Крок 9. Поглиблення після здачі (2–3 заняття по 2 год)

1. Відтворити прогін експерименту за `experiments/README.md` («Як відтворити») і звірити SHA-256 корпусу й запитів
   зі звітом.
2. Пройти розділ 3 записки без коду: на кожен рисунок і таблицю — пояснення вголос за 1 хвилину.
3. Почитати, як роблять BM25 усередині PostgreSQL розширеннями (ParadeDB `pg_search`, `pg_textsearch`) — це напрям
   удосконалення з висновків.
4. DOU, «Архітектура рішень, заснованих на LLM, для великих ентерпрайзів» — https://dou.ua/forums/topic/50191/
5. Почати презентацію до захисту 15.12: 10–12 слайдів — проблема, гіпотези, архітектура, експеримент,
   результати H1–H5, обмеження.
