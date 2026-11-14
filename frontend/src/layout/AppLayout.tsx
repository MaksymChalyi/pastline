import { Navigate, NavLink, Outlet } from 'react-router'
import { useLogout, useMe } from '../auth/useAuth'

/**
 * Макет для сторінок, що потребують входу. Без сесії перенаправляє на /login.
 * Outlet — місце, куди router підставляє дочірню сторінку (як layout-шаблон).
 */
export default function AppLayout() {
  const me = useMe()
  const logout = useLogout()

  if (me.isPending) {
    return <div className="center muted">Завантаження…</div>
  }
  if (me.isError) {
    return <div className="center error">Бекенд недоступний: {me.error.message}</div>
  }
  if (me.data === null) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">Pastline</span>
        <nav>
          <NavLink to="/" end>
            Записи
          </NavLink>
          <NavLink to="/search">Пошук</NavLink>
          <NavLink to="/ask">Запитати</NavLink>
          <NavLink to="/import">Імпорт</NavLink>
        </nav>
        <span className="spacer" />
        <span className="muted">{me.data.email}</span>
        <button className="link" onClick={() => logout.mutate()} disabled={logout.isPending}>
          Вийти
        </button>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
