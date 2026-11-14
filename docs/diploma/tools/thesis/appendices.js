// Додатки А і Б. Вміст читається з репозиторію під час збірки, тож завжди відповідає коду і даним.
const fs = require('fs');
const path = require('path');

const REPO = path.join(__dirname, '..', '..', '..', '..');
const read = (rel) => fs.readFileSync(path.join(REPO, rel), 'utf-8').replace(/\r\n/g, '\n');

// Java-файл без package та import: з першого рядка, що не є package/import/порожнім
function javaBody(rel) {
  const lines = read(rel).split('\n');
  const start = lines.findIndex((l) => l.trim() && !l.startsWith('package ') && !l.startsWith('import '));
  const body = lines.slice(start);
  while (body.length && !body[body.length - 1].trim()) body.pop();
  return body.map((l) => l.replace(/\t/g, '    '));
}

const JAVA = 'backend/src/main/java/com/pastline/';
const listings = [
  ['А.1', 'Злиття рангів RRF (клас RrfFusion)', JAVA + 'search/service/RrfFusion.java'],
  ['А.2', 'Перевірка посилань на джерела (клас CitationValidator)', JAVA + 'ask/service/CitationValidator.java'],
  ['А.3', 'Промпт Ask Pastline (клас AskPrompt)', JAVA + 'ask/service/AskPrompt.java'],
  ['А.4', 'Фонова індексація векторів (клас EmbeddingIndexer)', JAVA + 'search/embedding/EmbeddingIndexer.java'],
];

const appendixA = [
  ['p', 'У додатку наведено вихідний код ключових класів системи Pastline (версія з тегом thesis-freeze) без рядків package та import.'],
  ...listings.map(([num, title, file]) => ['code', { caption: `Лістинг ${num}. ${title}`, lines: javaBody(file), size: 18 }]),
];

// ---------- додаток Б ----------
// Запити — зі звіту основного прогону, вказаного в results-tables.md (там уже є тип і джерело кожного запиту)
const tablesMd = read('docs/experiments/results-tables.md');
const andRun = tablesMd.match(/docs\/experiments\/results\/(run-[\d-]+-and\.json)/)[1];
const queries = JSON.parse(read('docs/experiments/results/' + andRun)).queries;
const TYPE = { exact: 'точний термін', wordform: 'словоформа', paraphrase: 'перефразування', crosslingual: 'інша мова' };
const SOURCE = { author: 'автор', claude: 'Claude', chatgpt: 'ChatGPT', deepseek: 'DeepSeek' };

const questions = JSON.parse(read('docs/experiments/ask-v1/questions.json'));

const promptMd = read('docs/experiments/queries-v1/PROMPT.md');
const promptText = promptMd.slice(promptMd.indexOf('---') + 3).trim().split('\n');

// Розбиття Б.1 на сторінки: оцінка висоти рядка в рядках тексту (≈ 40 символів у стовпці запиту),
// бюджет першої сторінки менший через заголовок додатка
function pageChunks(texts, first, other, perLine = 40) {
  const chunks = []; let n = 0; let used = 0; let budget = first;
  for (const t of texts) {
    const h = Math.max(1, Math.ceil(t.length / perLine));
    if (used + h > budget && n > 0) { chunks.push(n); n = 0; used = 0; budget = other; }
    n += 1; used += h;
  }
  if (n) chunks.push(n);
  return chunks;
}

const appendixB = [
  ['p', `Повний набір із ${queries.length} пошукових запитів наведено в таблиці Б.1, запитання для оцінки Ask Pastline — у таблиці Б.2, промпт, за яким запити згенеровано моделями ChatGPT і DeepSeek, — у лістингу Б.1. Цільові речення корпусу і сам корпус зберігаються в репозиторії в каталозі docs/experiments.`],
  ['table', {
    num: 'Б.1',
    title: 'Набір пошукових запитів',
    widths: [700, 1500, 1300, 1700, 700, 3738],
    size: 20,
    header: ['№', 'Ідентифікатор', 'Джерело', 'Тип', 'Мова', 'Текст запиту'],
    rows: queries.map((q, i) => [String(i + 1), q.id, SOURCE[q.source] ?? q.source, TYPE[q.type] ?? q.type, q.lang, q.text]),
    chunks: pageChunks(queries.map((q) => q.text), 29, 43, 36),
  }],
  ['table', {
    num: 'Б.2',
    title: 'Запитання для оцінки Ask Pastline',
    widths: [700, 1100, 2300, 5538],
    size: 20,
    header: ['№', 'Ідентифікатор', 'Відповідь у корпусі', 'Запитання'],
    rows: questions.map((q, i) => [String(i + 1), q.id, q.expect === 'answer' ? 'є' : 'немає', q.question]),
  }],
  ['code', { caption: 'Лістинг Б.1. Промпт для генерації запитів моделями ChatGPT і DeepSeek', lines: promptText, size: 18 }],
  ['todo', 'Запити згенеровано: ChatGPT — модель [___], дата [__.__.2026]; DeepSeek — модель [___], дата [__.__.2026].'],
];

module.exports = { appendixA, appendixB };
