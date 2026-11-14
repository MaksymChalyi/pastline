import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { searchApi } from '../api/search'
import type { SearchMode } from '../api/types'
import { IconSearch } from '../layout/Icons'
import { formatEntryDate } from '../lib/dates'

const MODES: { value: SearchMode; label: string; hint: string }[] = [
  { value: 'lexical', label: 'Точні слова', hint: 'Повнотекстовий пошук PostgreSQL: знаходить записи з усіма словами запиту, у тій самій формі.' },
  { value: 'semantic', label: 'За змістом', hint: 'Векторний пошук: знаходить записи, близькі за змістом, навіть іншими словами чи іншою мовою.' },
  { value: 'hybrid', label: 'Гібридний', hint: 'Обидва списки зливаються через Reciprocal Rank Fusion (RRF).' },
]

function parseMode(value: string | null): SearchMode {
  return value === 'lexical' || value === 'hybrid' ? value : 'semantic'
}

/**
 * Пошук з перемикачем режиму. Запит, режим і debug живуть в адресі: однаковий URL дає однаковий результат,
 * тож режими зручно порівнювати, перемикаючи лише mode.
 */
export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const mode = parseMode(params.get('mode'))
  const debug = params.get('debug') === '1'

  // Чернетка поля вводу окремо від q в адресі: пошук іде лише після Enter, а не на кожну літеру
  const [draft, setDraft] = useState(q)

  const search = useQuery({
    queryKey: ['search', q, mode, debug],
    queryFn: () => searchApi.search(q, mode, debug),
    enabled: q.trim().length > 0,
  })

  function setParam(changes: Record<string, string>) {
    const next = new URLSearchParams(params)
    for (const [k, v] of Object.entries(changes)) {
      if (v) next.set(k, v)
      else next.delete(k)
    }
    setParams(next)
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setParam({ q: draft.trim() })
  }

  const data = search.data
  return (
    <section>
      <h1>Пошук</h1>
      <form className="search-bar" onSubmit={handleSubmit}>
        <input
          type="search"
          aria-label="Пошуковий запит"
          placeholder="Наприклад: коли я бігав напівмарафон"
          value={draft}
          maxLength={500}
          onChange={(e) => setDraft(e.target.value)}
          autoFocus
        />
        <button type="submit" disabled={!draft.trim()} aria-label="Шукати">
          <IconSearch size={20} />
          <span className="button-text">Шукати</span>
        </button>
      </form>

      <div className="mode-row">
        <div className="segmented" role="radiogroup" aria-label="Режим пошуку">
          {MODES.map((m) => (
            <button
              key={m.value}
              type="button"
              role="radio"
              aria-checked={mode === m.value}
              className={mode === m.value ? 'active' : ''}
              onClick={() => setParam({ mode: m.value })}
            >
              {m.label}
            </button>
          ))}
        </div>
        {mode === 'hybrid' && (
          <label className="check">
            <input type="checkbox" checked={debug} onChange={(e) => setParam({ debug: e.target.checked ? '1' : '' })} />
            Показати ранги
          </label>
        )}
      </div>
      <p className="muted hint">{MODES.find((m) => m.value === mode)!.hint}</p>

      {search.isFetching && !data && <p className="muted">Шукаю…</p>}
      {search.isError && <p className="error">{search.error.message}</p>}

      {data && (
        <>
          <div className="result-meta muted">
            Знайдено: {data.hits.length} · {data.tookMs} мс
            {data.pendingCount !== null && data.pendingCount > 0 && (
              <span className="warn">
                {' '}
                · {data.pendingCount} записів ще індексуються, семантичний пошук бачить не всі
              </span>
            )}
          </div>
          {data.hits.length === 0 ? (
            <div className="empty">Нічого не знайдено. Спробуйте інший режим або інші слова.</div>
          ) : (
            <ol className={'entry-list' + (search.isFetching ? ' stale' : '')}>
              {data.hits.map((hit) => (
                <li key={hit.id}>
                  <Link to={'/entries/' + hit.id} className="entry-card">
                    <div className="entry-meta">
                      <span className="rank">#{hit.rank}</span> {formatEntryDate(hit.entryDate)}
                    </div>
                    {hit.debug && (
                      <div className="debug">
                        lexical {hit.debug.lexicalRank ?? '—'} · semantic {hit.debug.semanticRank ?? '—'} · RRF{' '}
                        {hit.debug.rrfScore.toFixed(4)}
                      </div>
                    )}
                    {hit.title && <div className="entry-title">{hit.title}</div>}
                    <p className="entry-preview">{hit.snippet}</p>
                  </Link>
                </li>
              ))}
            </ol>
          )}
        </>
      )}
    </section>
  )
}
