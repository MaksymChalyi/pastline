import { useMutation } from '@tanstack/react-query'
import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link } from 'react-router'
import { askApi } from '../api/ask'
import type { AskResponse, AskSource } from '../api/types'
import { formatEntryDate } from '../lib/dates'

/** Локальна модель на CPU відповідає десятки секунд, тому показуємо час очікування. */
export default function AskPage() {
  const [question, setQuestion] = useState('')
  const ask = useMutation({ mutationFn: (q: string) => askApi.ask(q) })

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (question.trim()) {
      ask.mutate(question.trim())
    }
  }

  return (
    <section>
      <h1>Запитати щоденник</h1>
      <form className="editor" onSubmit={handleSubmit}>
        <label>
          Питання
          <textarea
            rows={3}
            maxLength={500}
            value={question}
            placeholder="Наприклад: що мені порадив лікар?"
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={(e) => {
              // Enter надсилає, Shift+Enter переносить рядок
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault()
                e.currentTarget.form?.requestSubmit()
              }
            }}
          />
        </label>
        <div className="editor-actions">
          <button type="submit" disabled={ask.isPending || !question.trim()}>
            {ask.isPending ? 'Думаю…' : 'Запитати'}
          </button>
          {ask.isPending && <Elapsed />}
        </div>
        <p className="muted hint">
          Відповідь складається лише з ваших записів. Речення без підтвердженого джерела не показуються.
        </p>
      </form>

      {ask.isError && <p className="error">{ask.error.message}</p>}
      {ask.data && <Answer response={ask.data} question={ask.variables ?? ''} />}
    </section>
  )
}

function Elapsed() {
  const [seconds, setSeconds] = useState(0)
  useEffect(() => {
    const timer = setInterval(() => setSeconds((s) => s + 1), 1000)
    return () => clearInterval(timer)
  }, [])
  return <span className="muted">Шукаю в записах і складаю відповідь… {seconds} с (зазвичай 20–60 с)</span>
}

const CITATION = /\[(E\d+(?:\s*,\s*E\d+)*)\]/g

/** Текст відповіді з [E1, E3] → надрядкові номери джерел, що ведуть до списку нижче. */
function renderAnswer(answer: string, numberByRef: Map<string, number>): ReactNode[] {
  const parts: ReactNode[] = []
  let last = 0
  for (const match of answer.matchAll(CITATION)) {
    // Пробіл перед [E1] прибираємо: номер джерела стоїть впритул до слова, як виноска
    parts.push(answer.slice(last, match.index).trimEnd())
    const refs = match[1].split(',').map((r) => r.trim())
    parts.push(
      <sup key={match.index} className="cite">
        {refs.map((ref) => (
          <a key={ref} href={'#src-' + ref} aria-label={'Джерело ' + (numberByRef.get(ref) ?? '?')}>
            {numberByRef.get(ref) ?? '?'}
          </a>
        ))}
      </sup>,
    )
    last = match.index + match[0].length
  }
  parts.push(answer.slice(last))
  return parts
}

function Answer({ response, question }: { response: AskResponse; question: string }) {
  if (response.status === 'NO_EVIDENCE') {
    return (
      <div className="answer">
        <p className="question">{question}</p>
        <p className="no-evidence">{response.answer}</p>
        <p className="muted small">Спробуйте переформулювати питання або знайдіть записи через пошук.</p>
      </div>
    )
  }
  const numberByRef = new Map(response.sources.map((s, i) => [s.ref, i + 1]))
  return (
    <div className="answer">
      <p className="question">{question}</p>
      <blockquote className="answer-text">{renderAnswer(response.answer, numberByRef)}</blockquote>
      <div className="muted small">
        {(response.tookMs / 1000).toFixed(1)} с
        {response.removedSentences > 0 &&
          ` · приховано речень без підтвердженого джерела: ${response.removedSentences}`}
      </div>
      <h2 className="sources-title">Джерела</h2>
      <ol className="sources">
        {response.sources.map((s) => (
          <SourceItem key={s.ref} source={s} />
        ))}
      </ol>
    </div>
  )
}

function SourceItem({ source }: { source: AskSource }) {
  return (
    <li id={'src-' + source.ref}>
      <Link to={'/entries/' + source.id} className="source-link">
        {formatEntryDate(source.entryDate)}
        {source.title ? ` · ${source.title}` : ''}
      </Link>
      <p className="source-snippet">{source.snippet}</p>
    </li>
  )
}
