import { Link, useSearchParams } from 'react-router'
import { useEntries } from '../entries/useEntries'
import { formatEntryDate } from '../lib/dates'

const PAGE_SIZE = 20
const PREVIEW_LENGTH = 280

/**
 * Стрічка записів, новіші вгорі. Фільтр і номер сторінки живуть в адресі (?from=&to=&page=), а не в useState:
 * F5, "назад" у браузері і посилання на конкретну сторінку працюють без додаткового коду.
 */
export default function EntriesPage() {
  const [params, setParams] = useSearchParams()
  const from = params.get('from') ?? ''
  const to = params.get('to') ?? ''
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)

  const entries = useEntries({ from: from || undefined, to: to || undefined, page, size: PAGE_SIZE })

  function update(changes: Record<string, string>) {
    const next = new URLSearchParams(params)
    for (const [key, value] of Object.entries(changes)) {
      if (value) next.set(key, value)
      else next.delete(key)
    }
    setParams(next)
  }

  const data = entries.data
  return (
    <section>
      <div className="page-head">
        <h1>Мої записи</h1>
        <Link className="button" to="/entries/new">
          Новий запис
        </Link>
      </div>

      <div className="filters">
        <label>
          Від
          <input type="date" value={from} onChange={(e) => update({ from: e.target.value, page: '' })} />
        </label>
        <label>
          До
          <input type="date" value={to} onChange={(e) => update({ to: e.target.value, page: '' })} />
        </label>
        {(from || to) && (
          <button className="link" onClick={() => update({ from: '', to: '', page: '' })}>
            Скинути
          </button>
        )}
        {data && <span className="muted spacer-left">Записів: {data.totalItems}</span>}
      </div>

      {entries.isPending && <p className="muted">Завантаження…</p>}
      {entries.isError && <p className="error">{entries.error.message}</p>}

      {data && data.items.length === 0 && (
        <div className="empty">
          {from || to ? 'За цей період записів немає.' : 'Записів поки немає. Створіть перший або імпортуйте файл.'}
        </div>
      )}

      {data && data.items.length > 0 && (
        <ul className={'entry-list' + (entries.isPlaceholderData ? ' stale' : '')}>
          {data.items.map((entry) => (
            <li key={entry.id}>
              <Link to={'/entries/' + entry.id} className="entry-card">
                <div className="entry-meta">{formatEntryDate(entry.entryDate)}</div>
                {entry.title && <div className="entry-title">{entry.title}</div>}
                <p className="entry-preview">
                  {entry.content.length > PREVIEW_LENGTH
                    ? entry.content.slice(0, PREVIEW_LENGTH) + '…'
                    : entry.content}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 && (
        <div className="pager">
          <button disabled={page === 0} onClick={() => update({ page: String(page - 1) })}>
            ← Новіші
          </button>
          <span className="muted">
            Сторінка {page + 1} з {data.totalPages}
          </span>
          <button
            disabled={page + 1 >= data.totalPages}
            onClick={() => update({ page: String(page + 1) })}
          >
            Старіші →
          </button>
        </div>
      )}
    </section>
  )
}
