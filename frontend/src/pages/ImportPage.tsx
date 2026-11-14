import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { importApi } from '../api/importing'
import type { ImportReport } from '../api/types'
import { entryKeys } from '../entries/useEntries'
import { formatEntryDate } from '../lib/dates'

/** Імпорт у два кроки: перевірка (dryRun=true), потім збереження. */
export default function ImportPage() {
  const queryClient = useQueryClient()
  const [file, setFile] = useState<File | null>(null)

  const run = useMutation({
    mutationFn: ({ f, dryRun }: { f: File; dryRun: boolean }) => importApi.importFile(f, dryRun),
    onSuccess: (report) => {
      if (!report.dryRun) {
        return queryClient.invalidateQueries({ queryKey: entryKeys.all })
      }
    },
  })

  const report = run.data
  return (
    <section>
      <h1>Імпорт записів</h1>
      <div className="editor">
        <label className="file-field">
          Файл JSON або Markdown (до 5 МБ, до 5000 записів)
          <input
            type="file"
            accept=".json,.md,.markdown,application/json,text/markdown"
            onChange={(e) => {
              setFile(e.target.files?.[0] ?? null)
              run.reset()
            }}
          />
        </label>

        <details className="muted">
          <summary>Формат файлу</summary>
          <p>JSON: масив об'єктів</p>
          <pre>{`[{"entryDate": "2026-03-14", "title": "Спорт", "content": "Пробіг 5 км у парку."}]`}</pre>
          <p>Markdown: кожен запис починається із заголовка з датою</p>
          <pre>{`## 2026-03-14 Спорт\nПробіг 5 км у парку.`}</pre>
        </details>

        <div className="editor-actions">
          <button
            className="secondary"
            disabled={!file || run.isPending}
            onClick={() => file && run.mutate({ f: file, dryRun: true })}
          >
            {run.isPending && run.variables?.dryRun ? 'Перевіряю…' : 'Перевірити'}
          </button>
          <button
            disabled={!file || run.isPending || !report?.dryRun || report.created === 0}
            onClick={() => file && run.mutate({ f: file, dryRun: false })}
          >
            {run.isPending && !run.variables?.dryRun ? 'Імпортую…' : 'Імпортувати'}
          </button>
        </div>

        {run.isError && <p className="error">{run.error.message}</p>}
        {report && <ReportView report={report} />}
      </div>
    </section>
  )
}

function ReportView({ report }: { report: ImportReport }) {
  return (
    <div className="report">
      <h2>{report.dryRun ? 'Результат перевірки (нічого не збережено)' : 'Імпорт завершено'}</h2>
      <dl>
        <dt>Записів у файлі</dt>
        <dd>{report.total}</dd>
        <dt>{report.dryRun ? 'Буде створено' : 'Створено'}</dt>
        <dd>{report.created}</dd>
        <dt>Вже були (пропущено)</dt>
        <dd>{report.duplicates}</dd>
        <dt>З помилками</dt>
        <dd className={report.failed > 0 ? 'error' : ''}>{report.failed}</dd>
        {report.firstDate && report.lastDate && (
          <>
            <dt>Період</dt>
            <dd>
              {formatEntryDate(report.firstDate)} — {formatEntryDate(report.lastDate)}
            </dd>
          </>
        )}
      </dl>
      {report.errors.length > 0 && (
        <ul className="error">
          {report.errors.map((e) => (
            <li key={e.index}>
              Запис №{e.index + 1}: {e.message}
            </li>
          ))}
        </ul>
      )}
      {!report.dryRun && report.created > 0 && (
        <p className="muted">
          Записи вже у <Link to="/">стрічці</Link>. Пошук за змістом побачить їх через кілька хвилин, коли
          фоновий індексатор порахує вектори.
        </p>
      )}
    </div>
  )
}
