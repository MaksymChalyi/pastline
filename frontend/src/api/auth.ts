import { api } from './client'
import type { User } from './types'

export const authApi = {
  me: () => api<User>('/me'),
  login: (email: string, password: string) =>
    api<User>('/auth/login', { method: 'POST', body: { email, password } }),
  register: (email: string, password: string, timezone: string) =>
    api<User>('/auth/register', { method: 'POST', body: { email, password, timezone } }),
  logout: () => api<void>('/auth/logout', { method: 'POST' }),
}
