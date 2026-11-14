import { api } from './client'
import type { Entry, EntryInput, Page } from './types'

export interface EntryFilter {
  from?: string
  to?: string
  page: number
  size: number
}

function query(filter: EntryFilter): string {
  const params = new URLSearchParams({ page: String(filter.page), size: String(filter.size) })
  if (filter.from) params.set('from', filter.from)
  if (filter.to) params.set('to', filter.to)
  return params.toString()
}

export const entriesApi = {
  list: (filter: EntryFilter) => api<Page<Entry>>('/entries?' + query(filter)),
  get: (id: string) => api<Entry>('/entries/' + id),
  create: (input: EntryInput) => api<Entry>('/entries', { method: 'POST', body: input }),
  update: (id: string, input: EntryInput) => api<Entry>('/entries/' + id, { method: 'PUT', body: input }),
  remove: (id: string) => api<void>('/entries/' + id, { method: 'DELETE' }),
}
