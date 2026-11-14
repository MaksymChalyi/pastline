import { useState, type FormEvent, type ReactNode } from 'react'
import { ApiError } from '../api/client'

interface Props {
  title: string
  submitLabel: string
  passwordHint?: string
  pending: boolean
  error: Error | null
  onSubmit: (email: string, password: string) => void
  footer: ReactNode
}

/**
 * Спільна форма для входу і реєстрації. useState тримає значення полів між рендерами: кожна зміна поля
 * викликає новий рендер компонента з новим значенням (контрольований input).
 */
export default function AuthForm({ title, submitLabel, passwordHint, pending, error, onSubmit, footer }: Props) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  function handleSubmit(e: FormEvent) {
    e.preventDefault() // інакше браузер перезавантажить сторінку, як у звичайної HTML-форми
    onSubmit(email.trim(), password)
  }

  const fieldErrors = error instanceof ApiError ? error.fieldErrors : []

  return (
    <div className="auth">
      <div className="auth-brand">
        <div className="brand-mark">Pastline</div>
        <p className="muted">Щоденник, який пам'ятає за вас</p>
      </div>
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h1>{title}</h1>
        <label>
          Email
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <label>
          Пароль
          <input
            type="password"
            autoComplete={passwordHint ? 'new-password' : 'current-password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          {passwordHint && <small className="muted">{passwordHint}</small>}
        </label>
        {error && (
          <div className="error" role="alert">
            {error.message}
            {fieldErrors.length > 0 && (
              <ul>
                {fieldErrors.map((f) => (
                  <li key={f.field}>
                    {f.field}: {f.message}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
        <button type="submit" className="block" disabled={pending}>
          {pending ? 'Зачекайте…' : submitLabel}
        </button>
        <div className="muted footer">{footer}</div>
      </form>
    </div>
  )
}
