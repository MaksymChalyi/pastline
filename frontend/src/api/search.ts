import { api } from './client'
import type { SearchMode, SearchResponse } from './types'

export const searchApi = {
  search: (q: string, mode: SearchMode, debug: boolean, limit = 20) => {
    const params = new URLSearchParams({ q, mode, limit: String(limit), debug: String(debug) })
    return api<SearchResponse>('/search?' + params.toString())
  },
}
