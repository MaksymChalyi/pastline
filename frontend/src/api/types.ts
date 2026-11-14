// Типи відповідей бекенду. Це копія Java-record'ів з DTO: TypeScript перевіряє їх лише під час компіляції,
// тож якщо бекенд змінить поле, ці типи треба оновити вручну.

export interface User {
  id: string
  email: string
  timezone: string | null
}

export interface FieldError {
  field: string
  message: string
}

/** ErrorResponse з бекенду: єдиний формат помилок API. */
export interface ApiErrorBody {
  code: string
  message: string
  requestId: string | null
  fieldErrors: FieldError[]
}

/** EntryResponse. Дати приходять рядками: entryDate як "2026-03-14", createdAt/updatedAt як ISO-момент. */
export interface Entry {
  id: string
  title: string | null
  content: string
  entryDate: string
  createdAt: string
  updatedAt: string
}

/** EntryRequest: те, що форма надсилає на створення і редагування. */
export interface EntryInput {
  title: string | null
  content: string
  entryDate: string
}

/** PageResponse<T> з бекенду; page рахується з 0. */
export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type SearchMode = 'lexical' | 'semantic' | 'hybrid'

/** Ранги в кожному режимі (null: запис не потрапив у кандидати режиму) і підсумковий RRF-бал; лише hybrid + debug. */
export interface HitDebug {
  lexicalRank: number | null
  semanticRank: number | null
  rrfScore: number
}

export interface SearchHit {
  id: string
  entryDate: string
  title: string | null
  snippet: string
  score: number
  rank: number
  debug?: HitDebug
}

export interface SearchResponse {
  mode: SearchMode
  query: string
  tookMs: number
  /** Записи без актуального вектора; null для lexical. */
  pendingCount: number | null
  hits: SearchHit[]
}

export interface ImportError {
  index: number
  message: string
}

/** При dryRun=true created означає "було б створено": нічого не збережено. */
export interface ImportReport {
  dryRun: boolean
  total: number
  created: number
  duplicates: number
  failed: number
  firstDate: string | null
  lastDate: string | null
  errors: ImportError[]
}

export type AskStatus = 'ANSWERED' | 'NO_EVIDENCE'

/** ref: позначка в тексті відповіді ("E2"), id: запис, на який вона вказує. */
export interface AskSource {
  ref: string
  id: string
  entryDate: string
  title: string | null
  snippet: string
}

export interface AskResponse {
  status: AskStatus
  answer: string
  sources: AskSource[]
  removedSentences: number
  tookMs: number
}
