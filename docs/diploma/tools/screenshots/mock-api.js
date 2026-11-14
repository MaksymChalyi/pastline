// Тестовий API для скріншотів записки: віддає збережені результати експерименту (запит «напівмарафон»
// у гібридному режимі і запитання q05 Ask Pastline) з тим самим форматом, що й справжній бекенд.
// Ранги й RRF-бали обчислюються з top-10 лексичного і семантичного списків звіту (усі 10 гібридних результатів
// є в обох списках, тож ранги точні). Запуск: node mock-api.js, далі npm run dev у frontend/ і make.py.
const http = require('http');
const fs = require('fs');
const path = require('path');

const EXP = path.join(__dirname, '..', '..', '..', 'experiments');
const readJson = (rel) => JSON.parse(fs.readFileSync(path.join(EXP, rel), 'utf-8'));
const tables = fs.readFileSync(path.join(EXP, 'results-tables.md'), 'utf-8');
const andRun = tables.match(/results\/(run-[\d-]+-and\.json)/)[1];
const askRun = tables.match(/results\/(run-ask-[\d-]+\.json)/)[1];

const corpus = new Map(readJson('corpus-v1/corpus.json').map((e) => [e.id, e]));
const query = readJson('results/' + andRun).queries.find((q) => q.id === 'claude-01');
const { lexical, semantic, hybrid } = query.modes;
const hits = hybrid.top.map((id, i) => {
  const lr = lexical.top.indexOf(id) + 1;
  const sr = semantic.top.indexOf(id) + 1;
  if (!lr || !sr) throw new Error('ранг поза top-10: ' + id);
  const score = 1 / (60 + lr) + 1 / (60 + sr);
  const e = corpus.get(id);
  return { id, entryDate: e.entryDate, title: e.title, snippet: e.content.slice(0, 200), score, rank: i + 1,
    debug: { lexicalRank: lr, semanticRank: sr, rrfScore: score } };
});
const search = { mode: 'hybrid', query: query.text, tookMs: Math.round(hybrid.latencyMedianMs), pendingCount: 0, hits };

const q05 = readJson('results/' + askRun).questions.find((q) => q.id === 'q05');
const ask = { status: q05.status, answer: q05.answer, removedSentences: q05.removedSentences, tookMs: q05.tookMs,
  sources: q05.sources.map((s) => { const [ref, id] = s.split(' '); const e = corpus.get(id);
    return { ref, id, entryDate: e.entryDate, title: e.title, snippet: e.content.slice(0, 200) }; }) };

http.createServer((req, res) => {
  const send = (code, body, headers = {}) => { res.writeHead(code, { 'Content-Type': 'application/json', ...headers }); res.end(JSON.stringify(body)); };
  req.resume();
  req.on('end', () => {
    if (req.url.startsWith('/api/v1/me')) return send(200, { id: 'demo', email: 'demo@pastline.local', timezone: 'Europe/Kyiv' });
    if (req.url.startsWith('/api/v1/auth/csrf')) return send(200, { headerName: 'X-XSRF-TOKEN', token: 't' }, { 'Set-Cookie': 'XSRF-TOKEN=t; Path=/' });
    if (req.url.startsWith('/api/v1/search')) return send(200, search);
    if (req.url.startsWith('/api/v1/ai/ask')) return send(200, ask);
    send(404, { code: 'NOT_FOUND', message: 'not found', requestId: null, fieldErrors: [] });
  });
}).listen(8090, '127.0.0.1', () => console.log('mock API :8090, питання для Ask:', q05.question));
