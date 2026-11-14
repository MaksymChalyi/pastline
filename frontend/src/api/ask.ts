import { api } from './client'
import type { AskResponse } from './types'

export const askApi = {
  ask: (question: string) => api<AskResponse>('/ai/ask', { method: 'POST', body: { question } }),
}
