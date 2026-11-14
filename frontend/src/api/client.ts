import type { ApiErrorBody, FieldError } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: FieldError[]

  constructor(status: number, code: string, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'

function readCookie(name: string): string | null {
  const prefix = name + '='
  const found = document.cookie.split('; ').find((c) => c.startsWith(prefix))
  return found ? decodeURIComponent(found.slice(prefix.length)) : null
}

/** Бекенд кладе CSRF-токен у cookie XSRF-TOKEN; якщо її ще немає, просимо видати. */
async function csrfToken(forceNew: boolean): Promise<string> {
  let token = forceNew ? null : readCookie(CSRF_COOKIE)
  if (!token) {
    await fetch('/api/v1/auth/csrf', { credentials: 'include' })
    token = readCookie(CSRF_COOKIE)
  }
  return token ?? ''
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

interface RequestOptions {
  method?: Method
  body?: unknown
}

/**
 * Єдиний HTTP-клієнт: cookie сесії, CSRF для змінних запитів, помилка бекенду → ApiError.
 * api('/me') → GET /api/v1/me.
 */
export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  return send<T>(path, options, false)
}

async function send<T>(path: string, { method = 'GET', body }: RequestOptions, retried: boolean): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const isForm = body instanceof FormData
  // Для файлу Content-Type (multipart з boundary) браузер ставить сам
  if (body !== undefined && !isForm) {
    headers['Content-Type'] = 'application/json'
  }
  if (method !== 'GET') {
    // Після входу сесія нова, і старий токен недійсний: при CSRF_INVALID беремо свіжий і повторюємо один раз
    headers[CSRF_HEADER] = await csrfToken(retried)
  }

  const res = await fetch('/api/v1' + path, {
    method,
    credentials: 'include',
    headers,
    body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
  })

  if (res.status === 204) {
    return undefined as T
  }
  const data: unknown = await res.json().catch(() => null)
  if (res.ok) {
    return data as T
  }

  const err = (data ?? {}) as Partial<ApiErrorBody>
  if (res.status === 403 && err.code === 'CSRF_INVALID' && !retried) {
    return send<T>(path, { method, body }, true)
  }
  throw new ApiError(
    res.status,
    err.code ?? 'HTTP_' + res.status,
    err.message ?? 'Помилка ' + res.status,
    err.fieldErrors ?? [],
  )
}
