import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import type { Entry } from '../api/types'
import { useDeleteEntry, useEntry, useSaveEntry } from '../entries/useEntries'
import { todayIso } from '../lib/dates'

/** /entries/new і /entries/:id: та сама форма, різниця лише в тому, чи є id. */
export default function EntryEditorPage() {
  const { id } = useParams()
  const entry = useEntry(id)

  if (id && entry.isPending) {
    return <p className="muted">Завантаження…</p>
  }
  if (id && entry.isError) {
    return (
      <p className="error">
        {entry.error instanceof ApiError && entry.error.status === 404 ? 'Запис не знайдено.' : entry.error.message}{' '}
        <Link to="/">До стрічки</Link>
      </p>
    )
  }
  // key: форма створюється заново для кожного запису, тож початкові значення беруться з пропсів один раз,
  // без useEffect для "синхронізації" стану з даними сервера
  return <EntryForm key={id ?? 'new'} entry={entry.data} />
}

function EntryForm({ entry }: { entry: Entry | undefined }) {
  const navigate = useNavigate()
  const save = useSaveEntry(entry?.id)
  const remove = useDeleteEntry()

  const [entryDate, setEntryDate] = useState(entry?.entryDate ?? todayIso())
  const [title, setTitle] = useState(entry?.title ?? '')
  const [content, setContent] = useState(entry?.content ?? '')

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    save.mutate(
      { entryDate, title: title.trim() || null, content },
      { onSuccess: () => navigate('/') },
    )
  }

  function handleDelete() {
    if (entry && window.confirm('Видалити цей запис? Дію не можна скасувати.')) {
      remove.mutate(entry.id, { onSuccess: () => navigate('/') })
    }
  }

  const error = save.error ?? remove.error
  const fieldErrors = error instanceof ApiError ? error.fieldErrors : []
  const busy = save.isPending || remove.isPending

  return (
    <section>
      <div className="page-head">
        <h1>{entry ? 'Редагування запису' : 'Новий запис'}</h1>
        <Link to="/" className="back-link">
          ← До стрічки
        </Link>
      </div>
      <form className="editor" onSubmit={handleSubmit}>
        <div className="editor-row">
          <label>
            Дата
            <input type="date" value={entryDate} max={todayIso()} onChange={(e) => setEntryDate(e.target.value)} required />
          </label>
          <label className="grow">
            Заголовок <span className="muted">(необов'язково)</span>
            <input value={title} maxLength={200} onChange={(e) => setTitle(e.target.value)} />
          </label>
        </div>
        <label>
          Текст
          <textarea
            className="prose"
            value={content}
            maxLength={20000}
            rows={14}
            onChange={(e) => setContent(e.target.value)}
            required
            autoFocus={!entry}
          />
          <small className="muted counter">{content.length} / 20000</small>
        </label>

        {error && (
          <div className="error" role="alert">
            {error.message}
            {fieldErrors.length > 0 && (
              <ul>
                {fieldErrors.map((f) => (
                  <li key={f.field}>
                    {f.field}: {f.message}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="editor-actions">
          <button type="submit" disabled={busy || !content.trim()}>
            {save.isPending ? 'Збереження…' : 'Зберегти'}
          </button>
          {entry && (
            <button type="button" className="danger" onClick={handleDelete} disabled={busy}>
              Видалити
            </button>
          )}
        </div>
      </form>
    </section>
  )
}
