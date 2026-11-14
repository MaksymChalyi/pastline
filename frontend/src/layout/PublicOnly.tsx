import { Navigate, Outlet } from 'react-router'
import { useMe } from '../auth/useAuth'

/** Вхід і реєстрація: з активною сесією перенаправляє на головну. */
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
