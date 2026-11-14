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
