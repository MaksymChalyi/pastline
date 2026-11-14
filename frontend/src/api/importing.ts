import { api } from './client'
import type { ImportReport } from './types'

export const importApi = {
  /** dryRun=true: лише перевірка й підрахунок, без збереження. */
  importFile: (file: File, dryRun: boolean) => {
    const form = new FormData()
    form.append('file', file)
    return api<ImportReport>('/import?dryRun=' + dryRun, { method: 'POST', body: form })
  },
}
