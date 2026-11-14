const longFormat = new Intl.DateTimeFormat('uk-UA', { day: 'numeric', month: 'long', year: 'numeric' })
const weekdayFormat = new Intl.DateTimeFormat('uk-UA', { weekday: 'short' })

/** "2026-03-14" → дата без часового поясу: інакше new Date() зсуне її на день для UTC-мінус. */
function parseLocalDate(isoDate: string): Date {
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** "2026-03-14" → "14 березня 2026 р., сб" */
export function formatEntryDate(isoDate: string): string {
  const date = parseLocalDate(isoDate)
  return `${longFormat.format(date)}, ${weekdayFormat.format(date)}`
}

/** Сьогоднішня дата в часовому поясі браузера у форматі yyyy-MM-dd (sv-SE дає саме його). */
export function todayIso(): string {
  return new Date().toLocaleDateString('sv-SE')
}
