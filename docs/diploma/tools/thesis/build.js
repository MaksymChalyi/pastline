// Генератор пояснювальної записки: node build.js → docs/diploma/text/ФКНТ_2026_F3_ЧалийМВ_Диплом.docx
// Текст розділів — у ch1.js, ch2.js, ...; джерела — у sources.js (нумерація за першою згадкою).
const fs = require('fs');
const path = require('path');
const SOURCES = require('./sources');
const {
  Document, Packer, Paragraph, TextRun, AlignmentType, HeadingLevel, TableOfContents, Footer, PageNumber,
  Table, TableRow, TableCell, WidthType, BorderStyle, ShadingType, TabStopType, LineRuleType, ImageRun,
} = require('docx');

const FONT = 'Times New Roman';
const IND = 709;            // абзацний відступ 1,25 см
const LINE = 360;           // 1,5 інтервали
const W = 9638;             // ширина тексту: 210 - 30 - 10 мм
const HL = 'yellow';        // дані, які треба перевірити

// ---------- примітиви ----------
const r = (text, o = {}) => new TextRun({ text, font: FONT, size: o.size ?? 28, bold: o.bold, italics: o.italics,
  underline: o.u ? {} : undefined, highlight: o.hl ? HL : undefined, allCaps: o.caps, break: o.br });
const fill = (text, o = {}) => r(text, { ...o, hl: true });          // жовтим — уточнити
const p = (children, o = {}) => new Paragraph({
  children: typeof children === 'string' ? [r(children)] : children,
  alignment: o.align ?? AlignmentType.JUSTIFIED,
  indent: o.noIndent ? { firstLine: 0, left: o.left ?? 0 } : { firstLine: IND, left: o.left ?? 0 },
  spacing: { lineRule: LineRuleType.AUTO, line: o.line ?? LINE, before: o.before ?? 0, after: o.after ?? 0 },
  keepNext: o.keepNext, pageBreakBefore: o.pageBreak,
  tabStops: o.tabs ? o.tabs.map((position) => ({ type: TabStopType.LEFT, position })) : undefined,
});
const center = (children, o = {}) => p(children, { ...o, align: AlignmentType.CENTER, noIndent: true });
const empty = (n = 1) => Array.from({ length: n }, () => p([r('')], { noIndent: true }));

const h1 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [r(text, { bold: true })] });
const chapter = (n, title) => new Paragraph({ heading: HeadingLevel.HEADING_1,
  children: [r(`РОЗДІЛ ${n}`, { bold: true }), r(title, { bold: true, br: 1 })] });
const h2 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [r(text, { bold: true })] });
const h3 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [r(text)] });
const unlisted = (text) => new Paragraph({ style: 'Unlisted', children: [r(text, { bold: true })] });

// Підказка: що писати і звідки брати матеріал. Після написання підрозділу — видалити.
const hint = (...lines) => lines.map((line, i) => new Paragraph({
  style: 'Hint',
  children: [new TextRun({ text: (i === 0 ? 'Підказка. ' : '') + line, font: FONT, size: 24, italics: true, color: '595959' })],
}));

// ---------- посилання на джерела ----------
const citedOrder = [];
function cite(text) {
  return text.replace(/\[@([^\]]+)\]/g, (_, inner) => {
    const nums = inner.split(';').map((k) => k.trim().replace(/^@/, '')).map((key) => {
      if (!SOURCES[key]) throw new Error('Невідоме джерело: ' + key);
      if (!citedOrder.includes(key)) citedOrder.push(key);
      return citedOrder.indexOf(key) + 1;
    });
    return '[' + nums.join(', ') + ']';
  });
}

// ---------- текст розділу з блоків ----------
function renderTable(t) {
  const out = [
    p([r(`Таблиця ${t.num}`)], { noIndent: true, align: AlignmentType.RIGHT, before: 240, keepNext: true }),
    center([r(t.title)], { keepNext: true, after: 120 }),
  ];
  const size = t.size ?? 24;
  const total = t.widths.reduce((a, b) => a + b, 0);
  const para = (text, bold, align) => new Paragraph({ alignment: align, indent: { firstLine: 0 }, spacing: { lineRule: LineRuleType.AUTO, line: 240 },
    children: [r(cite(text), { size, bold })] });
  out.push(new Table({
    width: { size: total, type: WidthType.DXA },
    columnWidths: t.widths,
    rows: [
      new TableRow({ tableHeader: true, cantSplit: true, children: t.header.map((h, i) =>
        new TableCell({ width: { size: t.widths[i], type: WidthType.DXA }, margins: { top: 40, bottom: 40, left: 70, right: 70 },
          children: [para(h, true, AlignmentType.CENTER)] })) }),
      ...t.rows.map((row) => new TableRow({ cantSplit: true, children: row.map((c, i) =>
        new TableCell({ width: { size: t.widths[i], type: WidthType.DXA }, margins: { top: 40, bottom: 40, left: 70, right: 70 },
          children: [para(c, i === 0, AlignmentType.LEFT)] })) })),
    ],
  }));
  out.push(p([r('')], { noIndent: true, line: 240 }));
  return out;
}
const REPO = path.join(__dirname, '..', '..', '..', '..');
function pngSize(buf) {
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) };
}
function renderFigure(f) {
  const data = fs.readFileSync(path.join(REPO, f.file));
  const { w, h } = pngSize(data);
  const width = Math.round((f.widthCm / 2.54) * 96);
  const height = Math.round((width * h) / w);
  return [
    new Paragraph({ alignment: AlignmentType.CENTER, indent: { firstLine: 0 }, spacing: { lineRule: LineRuleType.AUTO, before: 240, line: 240 }, keepNext: true,
      children: [new ImageRun({ type: 'png', data, transformation: { width, height } })] }),
    center([r(f.caption)], { after: 240 }),
  ];
}
function renderBlocks(blocks) {
  return blocks.flatMap(([kind, value]) => {
    switch (kind) {
      case 'h2': return [h2(value)];
      case 'h3': return [h3(value)];
      case 'p': return [p(cite(value))];
      case 'note': return [p([r(cite(value), { size: 24 })], { line: 276 })];
      case 'list': return value.map((item) => p('– ' + cite(item)));
      case 'olist': return value.map((item, i) => p(`${i + 1}. ` + cite(item)));
      case 'table': return renderTable(value);
      case 'figure': return renderFigure(value);
      case 'formula': return [new Paragraph({
        children: [new TextRun({ text: '\t' + value.text, font: FONT, size: 28 }), new TextRun({ text: `\t(${value.num})`, font: FONT, size: 28 })],
        tabStops: [{ type: TabStopType.CENTER, position: 4819 }, { type: TabStopType.RIGHT, position: 9638 }],
        spacing: { lineRule: LineRuleType.AUTO, line: LINE, before: 120, after: 120 }, keepNext: true,
      })];
      case 'where': return [p(cite(value), { noIndent: true })];
      case 'hint': return hint(value);
      case 'code': return [
        p([r(value.caption)], { noIndent: true, keepNext: true, before: 120 }),
        ...value.lines.map((line, i) => new Paragraph({
          children: [new TextRun({ text: line.length ? line : ' ', font: 'Courier New', size: value.size ?? 22 })],
          indent: { firstLine: 0, left: 284 }, alignment: AlignmentType.LEFT,
          spacing: { lineRule: LineRuleType.AUTO, line: 240, before: 0, after: i === value.lines.length - 1 ? 120 : 0 },
          keepNext: value.lines.length <= 15 ? i < value.lines.length - 1 : i < 2, keepLines: true,
        })),
      ];
      default: throw new Error('Невідомий блок ' + kind);
    }
  });
}
const renderChapter = (n, ch) => [chapter(n, ch.title), ...renderBlocks(ch.blocks)];

const cell = (children, width, o = {}) => new TableCell({
  width: { size: width, type: WidthType.DXA },
  borders: o.noBorders ? noBorders : undefined,
  margins: { top: 40, bottom: 40, left: 80, right: 80 },
  children: (Array.isArray(children) ? children : [children]).map((c) =>
    typeof c === 'string' ? p([r(c, { size: o.size ?? 24, bold: o.bold })], { noIndent: true, line: 240, align: o.align ?? AlignmentType.LEFT }) : c),
});
const none = { style: BorderStyle.NONE, size: 0, color: 'FFFFFF' };
const noBorders = { top: none, bottom: none, left: none, right: none };

// ---------- титульний аркуш (форма Ф 03.01-28) ----------
const rightBlock = (lines) => lines.map((l) => p(l, { noIndent: true, left: 5200, align: AlignmentType.LEFT, line: 276 }));
const titlePage = [
  center([r('МІНІСТЕРСТВО ОСВІТИ І НАУКИ УКРАЇНИ', { bold: true })], { line: 276 }),
  center([r('НАЦІОНАЛЬНИЙ УНІВЕРСИТЕТ «КИЇВСЬКИЙ АВІАЦІЙНИЙ ІНСТИТУТ»', { bold: true })], { line: 276 }),
  center([r('Кафедра комп’ютерних інформаційних технологій')], { line: 276, before: 120 }),
  ...empty(2),
  ...rightBlock([
    [r('ДОПУСТИТИ ДО ЗАХИСТУ')],
    [r('Завідувач кафедри')],
    [r('_________ Аліна САВЧЕНКО')],
    [r('«_____» ____________ 2026 р.')],
  ]),
  ...empty(4),
  center([r('КВАЛІФІКАЦІЙНА РОБОТА', { bold: true, size: 40 })], { line: 276 }),
  center([r('(ПОЯСНЮВАЛЬНА ЗАПИСКА)', { bold: true })], { line: 276 }),
  center([r('ЗДОБУВАЧА ОСВІТНЬОГО СТУПЕНЯ «МАГІСТР»', { bold: true })], { line: 276, before: 240 }),
  ...empty(2),
  p([r('Тема: ', { bold: true }), fill('«Інтелектуальна система обліку та інтелектуального пошуку персональних записів»')],
    { noIndent: true, align: AlignmentType.LEFT }),
  ...empty(2),
  p([r('Виконавець:', { bold: true }), r('\t'), r('Максим ЧАЛИЙ')], { noIndent: true, align: AlignmentType.LEFT, tabs: [3000] }),
  ...empty(1),
  p([r('Керівник:', { bold: true }), r('\t'), fill('[науковий ступінь, вчене звання] Ім’я ПРІЗВИЩЕ')], { noIndent: true, align: AlignmentType.LEFT, tabs: [3000] }),
  ...empty(1),
  p([r('Нормоконтролер:', { bold: true }), r('\t'), fill('[науковий ступінь, вчене звання] Ім’я ПРІЗВИЩЕ')], { noIndent: true, align: AlignmentType.LEFT, tabs: [3000] }),
  ...empty(9),
  center([r('Київ – 2026', { bold: true })]),
];

// ---------- завдання (форма Ф 03.01-19) ----------
const lineItem = (num, label, valueRuns) =>
  p([r(`${num}. `), r(label, { bold: true }), r(' '), ...valueRuns], { noIndent: true, align: AlignmentType.JUSTIFIED });
const calendar = [
  ['Провести аналіз предметної області та існуючих систем ведення персональних записів. Сформувати вступ і перелік джерел. Написати розділ 1, подати керівнику.'],
  ['Провести аналіз методів пошуку та засобів реалізації, обґрунтувати вибір технологій. Написати розділ 2, подати керівнику.'],
  ['Розробити програмну систему, провести експериментальне дослідження. Написати розділ 3, подати керівнику.'],
  ['Сформувати висновки, виконати загальне редагування пояснювальної записки.'],
  ['Пройти нормоконтроль і перевірку роботи на плагіат.'],
  ['Підготувати доповідь, ілюстративний матеріал і презентацію.'],
];
const calTable = new Table({
  width: { size: W, type: WidthType.DXA },
  columnWidths: [700, 5338, 1900, 1700],
  rows: [
    new TableRow({ tableHeader: true, children: [
      cell('№ з/п', 700, { align: AlignmentType.CENTER }), cell('Завдання', 5338, { align: AlignmentType.CENTER }),
      cell('Термін виконання', 1900, { align: AlignmentType.CENTER }), cell('Підпис керівника', 1700, { align: AlignmentType.CENTER })] }),
    ...calendar.map(([task], i) => new TableRow({ children: [
      cell(`${i + 1}.`, 700, { align: AlignmentType.CENTER }), cell(task, 5338),
      cell([p([fill('__.__.2026–__.__.2026', { size: 24 })], { noIndent: true, line: 240, align: AlignmentType.CENTER })], 1900),
      cell('', 1700)] })),
  ],
});
const signRow = (left, nameRuns, caption1, caption2) => [
  p([r(left), r('\t_____________\t'), ...nameRuns], { noIndent: true, align: AlignmentType.LEFT, line: 276, before: 240, tabs: [4700, 7000] }),
  p([r(`\t${caption1}\t${caption2}`, { size: 18 })], { noIndent: true, align: AlignmentType.LEFT, line: 240, tabs: [4700, 7000] }),
];
const assignment = [
  p([r('НАЦІОНАЛЬНИЙ УНІВЕРСИТЕТ «КИЇВСЬКИЙ АВІАЦІЙНИЙ ІНСТИТУТ»')], { noIndent: true, line: 276, pageBreak: true }),
  p([r('Факультет '), r('комп’ютерних наук та технологій', { u: true })], { noIndent: true, line: 276 }),
  p([r('Кафедра '), r('комп’ютерних інформаційних технологій', { u: true })], { noIndent: true, line: 276 }),
  p([r('Галузь знань, спеціальність, освітньо-професійна програма: '),
     fill('12 «Інформаційні технології», 122 «Комп’ютерні науки», «Технології управління комп’ютерними системами»', { u: true })],
    { noIndent: true, line: 276 }),
  ...empty(1),
  ...rightBlock([[r('ЗАТВЕРДЖУЮ')], [r('Завідувач кафедри')], [r('_________ Аліна САВЧЕНКО')], [r('«_____» ____________ 2026 р.')]]),
  ...empty(1),
  center([r('ЗАВДАННЯ', { bold: true })], { line: 276 }),
  center([r('на виконання кваліфікаційної роботи', { bold: true })], { line: 276 }),
  center([r('Чалого Максима Віталійовича', { u: true })], { line: 276, before: 120 }),
  center([r('(прізвище, ім’я, по батькові здобувача вищої освіти в родовому відмінку)', { size: 18 })], { line: 240 }),
  ...empty(1),
  lineItem(1, 'Тема кваліфікаційної роботи:', [fill('«Інтелектуальна система обліку та інтелектуального пошуку персональних записів»', { u: true }),
    r(', затверджена наказом ректора від '), fill('«__» ________ 2026 р. № ____', { u: true }), r('.')]),
  lineItem(2, 'Термін виконання роботи:', [r('з '), fill('__.__.2026', { u: true }), r(' по '), fill('27.11.2026', { u: true }), r('.')]),
  lineItem(3, 'Вихідні дані до роботи:', [fill('синтетичний корпус із 520 записів щоденника; набір із 102 пошукових запитів і 20 питань до записів; ' +
    'засоби реалізації: Java 21, Spring Boot, PostgreSQL з розширенням pgvector, локальні мовні моделі (Ollama), React і TypeScript.', { u: true })]),
  lineItem(4, 'Зміст пояснювальної записки:', [r('1. Аналіз предметної області та постановка задачі інтелектуального пошуку персональних записів. ' +
    '2. Аналіз методів і засобів розробки системи. 3. Розробка та експериментальне дослідження системи пошуку персональних записів.', { u: true })]),
  lineItem(5, 'Перелік обов’язкового графічного (ілюстративного) матеріалу:', [r('1. Архітектура системи. 2. Схема бази даних. ' +
    '3. Послідовність гібридного пошуку. 4. Послідовність Ask Pastline з перевіркою посилань. 5. Результати експериментального дослідження.', { u: true })]),
  p([r('6. Календарний план-графік', { bold: true })], { noIndent: true, before: 120, after: 120, keepNext: true }),
  calTable,
  p([r('7. Консультації з окремих розділів: ', { bold: true }), r('не передбачено.')], { noIndent: true, before: 240 }),
  p([r('8. Дата видачі завдання: ', { bold: true }), fill('«__» ________ 2026 р.', { u: true })], { noIndent: true }),
  ...signRow('Керівник кваліфікаційної роботи', [fill('Ім’я ПРІЗВИЩЕ')], '(підпис керівника)', '(Ім’я, ПРІЗВИЩЕ)'),
  ...signRow('Завдання прийняв до виконання', [r('Максим ЧАЛИЙ')], '(підпис здобувача)', '(Ім’я, ПРІЗВИЩЕ)'),
];
// ---------- реферат ----------
// Кількість сторінок — за рендером; після фінального оформлення в Word перевірити (жовтим)
const PAGES = '__';
const abstract = () => [
  unlisted('РЕФЕРАТ'),
  p([r('Пояснювальна записка до кваліфікаційної роботи «Інтелектуальна система обліку та інтелектуального пошуку персональних записів»: '),
     fill(`${PAGES} с.`), r(`, 12 рис., 22 табл., ${citedOrder.length} джерел, 2 додатки.`)]),
  p([r('ПЕРСОНАЛЬНІ ЗАПИСИ, ЩОДЕННИК, ЛЕКСИЧНИЙ ПОШУК, СЕМАНТИЧНИЙ ПОШУК, ГІБРИДНИЙ ПОШУК, ВЕКТОРНІ ПРЕДСТАВЛЕННЯ, RRF, RAG, ВЕЛИКІ МОВНІ МОДЕЛІ, PGVECTOR')]),
  p('Об’єкт дослідження — процес пошуку інформації в персональних записах. Предмет дослідження — методи лексичного, семантичного і гібридного пошуку персональних записів та генерації відповідей із перевіркою посилань на джерела.'),
  p('Мета роботи — розробити систему обліку персональних записів із трьома режимами пошуку та відповідями на запитання з перевіркою джерел і експериментально порівняти режими пошуку за якістю і затримкою.'),
  p('Методи: аналіз і порівняння, об’єктно-орієнтоване проєктування, автоматизоване тестування, обчислювальний експеримент. Засоби: Java 21, Spring Boot, PostgreSQL з pgvector, Ollama, React, TypeScript.'),
  p('Розроблено систему Pastline з лексичним, семантичним і гібридним (RRF) пошуком, фоновою індексацією векторів і відповідями з детермінованою перевіркою посилань. На корпусі з 520 записів і 102 запитах семантичний пошук досяг MRR@10 = 0,832 проти 0,108 у лексичного; гібридний пошук не перевершив семантичного через відсутність морфологічної нормалізації української мови; медіанна затримка — 2,3 мс для лексичного і 252,7 мс для семантичного пошуку. Відповіді правильні в 13 з 15 випадків, на всіх 5 запитаннях без відповіді система відмовилася відповідати.'),
  p('Результати можна використати під час вибору методу пошуку для невеликих персональних колекцій текстів і як основу приватного застосунку для щоденника. Подальший розвиток — морфологічна нормалізація української мови і зважене злиття рангів.'),
];

// ---------- зміст ----------
const toc = [
  unlisted('ЗМІСТ'),
  new TableOfContents('Зміст', { hyperlink: true, headingStyleRange: '1-3' }),
  ...hint('Зміст заповнює Word: правий клік по змісту → «Оновити поле» → «Оновити повністю» (або F9). Оновлювати після кожної зміни заголовків.'),
];

// ---------- перелік скорочень ----------
const abbr = [
  ['ANN', 'Approximate Nearest Neighbors — наближений пошук найближчих сусідів'],
  ['API', 'Application Programming Interface — програмний інтерфейс застосунку'],
  ['BM25', 'Best Matching 25 — функція ранжування в інформаційному пошуку'],
  ['CSRF', 'Cross-Site Request Forgery — підробка міжсайтового запиту'],
  ['DTO', 'Data Transfer Object — об’єкт передачі даних'],
  ['FTS', 'Full-Text Search — повнотекстовий пошук'],
  ['GIN', 'Generalized Inverted Index — узагальнений інвертований індекс PostgreSQL'],
  ['HNSW', 'Hierarchical Navigable Small World — граф для наближеного векторного пошуку'],
  ['JPA', 'Jakarta Persistence API'],
  ['JSON', 'JavaScript Object Notation'],
  ['JWT', 'JSON Web Token'],
  ['LLM', 'Large Language Model — велика мовна модель'],
  ['MRR', 'Mean Reciprocal Rank — середній обернений ранг'],
  ['RAG', 'Retrieval-Augmented Generation — генерація з доповненням пошуком'],
  ['REST', 'Representational State Transfer'],
  ['RRF', 'Reciprocal Rank Fusion — злиття рангів'],
  ['SPA', 'Single Page Application — односторінковий застосунок'],
  ['SQL', 'Structured Query Language'],
  ['UUID', 'Universally Unique Identifier'],
  ['ПЗ', 'програмне забезпечення'],
  ['СУБД', 'система управління базами даних'],
];
const abbrTable = new Table({
  width: { size: W, type: WidthType.DXA }, columnWidths: [1600, 8038],
  rows: abbr.map(([a, b]) => new TableRow({ children: [
    cell([p([r(a)], { noIndent: true, line: LINE, align: AlignmentType.LEFT })], 1600, { noBorders: true }),
    cell([p([r('– ' + b)], { noIndent: true, line: LINE, align: AlignmentType.LEFT })], 8038, { noBorders: true })] })),
});
const abbreviations = [
  h1('ПЕРЕЛІК УМОВНИХ ПОЗНАЧЕНЬ, СКОРОЧЕНЬ, ТЕРМІНІВ'),
  abbrTable,
  ...hint('Перелік за абеткою, дві колонки. Залишити лише ті скорочення, що вжито в тексті три й більше разів; решту розшифрувати при першій згадці (Положення, п. 4.8). Спершу латиниця, потім кирилиця.'),
];

// ---------- вступ ----------
const intro = [h1('ВСТУП'), ...renderBlocks(require('./intro'))];

// ---------- розділ 1 ----------
const ch1 = renderChapter(1, require('./ch1'));

// ---------- розділ 2 ----------
const ch2 = renderChapter(2, require('./ch2'));

// ---------- розділ 3 ----------
const ch3 = renderChapter(3, require('./ch3'));

// ---------- кінець ----------
const { appendixA, appendixB } = require('./appendices');
const appendixTitle = (letter, title) => [
  new Paragraph({ heading: HeadingLevel.HEADING_1, alignment: AlignmentType.RIGHT, children: [r(`ДОДАТОК ${letter}`, { bold: true })] }),
  center([r(title, { bold: true })], { after: 240 }),
];
const tail = [
  h1('ВИСНОВКИ'),
  ...renderBlocks(require('./conclusions')),
  h1('СПИСОК БІБЛІОГРАФІЧНИХ ПОСИЛАНЬ ВИКОРИСТАНИХ ДЖЕРЕЛ'),
  ...citedOrder.map((key, i) => p(`${i + 1}. ${SOURCES[key]}`)),
  ...appendixTitle('А', 'Фрагменти програмного коду'),
  ...renderBlocks(appendixA),
  ...appendixTitle('Б', 'Набори запитів і запитань експериментального дослідження'),
  ...renderBlocks(appendixB),
];

// ---------- документ ----------
const margins = { top: 1134, bottom: 1134, left: 1701, right: 567 };
const doc = new Document({
  creator: 'Максим Чалий',
  title: 'Інтелектуальна система обліку та інтелектуального пошуку персональних записів',
  features: { updateFields: true },
  styles: {
    default: {
      document: {
        run: { font: FONT, size: 28 },
        paragraph: { spacing: { lineRule: LineRuleType.AUTO, line: LINE }, indent: { firstLine: IND } },
      },
    },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 28, bold: true, allCaps: true },
        paragraph: { alignment: AlignmentType.CENTER, indent: { firstLine: 0 }, spacing: { lineRule: LineRuleType.AUTO, before: 0, after: 840, line: LINE },
          keepNext: true, pageBreakBefore: true, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 28, bold: true },
        paragraph: { alignment: AlignmentType.JUSTIFIED, indent: { firstLine: IND }, spacing: { lineRule: LineRuleType.AUTO, before: 420, after: 420, line: LINE },
          keepNext: true, outlineLevel: 1 } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 28, bold: false },
        paragraph: { alignment: AlignmentType.JUSTIFIED, indent: { firstLine: IND }, spacing: { lineRule: LineRuleType.AUTO, before: 420, after: 420, line: LINE },
          keepNext: true, outlineLevel: 2 } },
      { id: 'Unlisted', name: 'Заголовок поза змістом', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 28, bold: true, allCaps: true },
        paragraph: { alignment: AlignmentType.CENTER, indent: { firstLine: 0 }, spacing: { lineRule: LineRuleType.AUTO, before: 0, after: 840, line: LINE },
          keepNext: true, pageBreakBefore: true } },
      { id: 'Hint', name: 'Підказка (видалити)', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: FONT, size: 24, italics: true, color: '595959' },
        paragraph: { alignment: AlignmentType.LEFT, indent: { firstLine: 0, left: 0 }, spacing: { lineRule: LineRuleType.AUTO, before: 60, after: 60, line: 264 },
          shading: { type: ShadingType.CLEAR, fill: 'F2F2F2', color: 'auto' } } },
      { id: 'TOC1', name: 'toc 1', basedOn: 'Normal', next: 'Normal',
        paragraph: { indent: { firstLine: 0, left: 0 }, alignment: AlignmentType.LEFT, spacing: { lineRule: LineRuleType.AUTO, line: LINE } } },
      { id: 'TOC2', name: 'toc 2', basedOn: 'Normal', next: 'Normal',
        paragraph: { indent: { firstLine: 0, left: 420 }, alignment: AlignmentType.LEFT, spacing: { lineRule: LineRuleType.AUTO, line: LINE } } },
      { id: 'TOC3', name: 'toc 3', basedOn: 'Normal', next: 'Normal',
        paragraph: { indent: { firstLine: 0, left: 840 }, alignment: AlignmentType.LEFT, spacing: { lineRule: LineRuleType.AUTO, line: LINE } } },
    ],
  },
  sections: [
    { // титул, завдання, реферат: сторінки рахуються, номери не друкуються
      properties: { page: { size: { width: 11906, height: 16838 }, margin: margins } },
      children: [...titlePage, ...assignment, ...abstract()],
    },
    {
      properties: { page: { size: { width: 11906, height: 16838 }, margin: margins } },
      footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.RIGHT, indent: { firstLine: 0 },
        children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 28 })] })] }) },
      children: [...toc, ...abbreviations, ...intro, ...ch1, ...ch2, ...ch3, ...tail],
    },
  ],
});

const OUT = process.argv[2] ?? path.join(__dirname, '..', '..', 'text', 'ФКНТ_2026_F3_ЧалийМВ_Диплом.docx');
Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(OUT, buf);
  console.log('ok', buf.length);
});
