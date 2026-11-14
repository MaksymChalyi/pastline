import { Navigate, Outlet } from 'react-router'
import { useMe } from '../auth/useAuth'

/** Сторінки входу й реєстрації: якщо сесія вже є, на них немає сенсу лишатись. */
export default function PublicOnly() {
  const me = useMe()
  if (me.isPending) {
    return <div className="center muted">Завантаження…</div>
  }
  if (me.data) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
