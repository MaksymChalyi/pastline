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
