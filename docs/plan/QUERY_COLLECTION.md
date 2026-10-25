# External query collection

Purpose: get 30 search queries from people who have never seen the corpus, so that part of the evaluation query set is not biased toward what the author already knows is retrievable (master prompt Section 20.12, query formulation bias).

## Protocol

1. Before checkpoint 1.6, send the message below to 3 people (P1, P2, P3). They receive only the topic description. Never send them corpus entries, generator code or example queries from the author.
2. Each person writes 10 queries. Deadline: target 2026-10-29, at the latest before checkpoint 1.6 starts.
3. Save the replies verbatim, without correcting typos or wording, under labels P1-P3. Store no names or contact details in the repository.
4. The generator in checkpoint 1.6 must cover the topics below, and it is committed with its corpus checksum before the external queries are labeled. Never change the generator afterwards to make an external query answerable. A query with no matching entry becomes a no-answer query.
5. In `docs/experiments/search-query-set-v1.json` the queries carry `source: external-p1 | external-p2 | external-p3`. Their category and relevance labels are assigned by the author using the labeling guideline.

## Topic description (send exactly this, draft)

> Це вигаданий особистий щоденник людини на ім'я Андрій: Java-розробник із Києва, який паралельно навчається в магістратурі. Записи охоплюють приблизно 2024-2026 роки, написані переважно українською, іноді англійською або змішано.
>
> Про що він пише:
> - робота: задачі, релізи, інциденти на продакшні, команда, мітинги, зміна проєкту;
> - навчання: магістратура, дипломна робота, науковий керівник, сесія та екзамени;
> - самоосвіта: Java, багатопотоковість, Spring, бази даних, AI-інструменти, курси та книжки;
> - поїздки: інші міста України, закордонні поїздки, конференції;
> - здоров'я і спорт: біг, спортзал, сон, хвороби, лікарі;
> - родина та друзі: зустрічі, свята, дні народження, сварки та примирення;
> - побут і гроші: переїзд, ремонт, великі покупки, бюджет;
> - хобі: книжки, фільми, музика, готування;
> - настрій і роздуми: втома, мотивація, плани, підсумки.

The final topic list in this section is the generator's topic specification for checkpoint 1.6. If a topic is changed here after the message is sent, record the change and the reason.

## Message to send (draft)

> Привіт! Мені потрібна твоя допомога з дипломом, це займе 10-15 хвилин.
>
> Я роблю систему пошуку по особистому щоденнику. Щоб чесно її перевірити, мені потрібні запити від людей, які не бачили самих записів.
>
> Нижче короткий опис вигаданого щоденника. Уяви, що це твій щоденник за кілька років і ти хочеш щось у ньому знайти. Напиши 10 запитів так, як ти б реально шукав:
> - можна коротко ("коли я хворів") або повним питанням ("коли я востаннє був на конференції за кордоном?");
> - можна точні слова або розмито, як пам'ятаєш ("той вечір, коли ми посварились через переїзд");
> - українською, англійською або змішано, як тобі природно;
> - не перевіряй, чи таке справді є в щоденнику: запити про те, чого там може не бути, теж корисні;
> - не виправляй друкарські помилки, якщо вони з'являться природно.
>
> Надішли просто нумерованим списком 1-10. Дякую!
>
> [вставити опис тем вище]

## Received queries

Save the verbatim replies in `docs/experiments/external-queries-raw.md` (P1-P3 labels only). Track receipt here.

| Person | Sent | Received | Count |
|---|---|---|---|
| P1 | | | |
| P2 | | | |
| P3 | | | |
