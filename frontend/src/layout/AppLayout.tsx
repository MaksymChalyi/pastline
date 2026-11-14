import { Navigate, NavLink, Outlet } from 'react-router'
import { useLogout, useMe } from '../auth/useAuth'
import { IconAsk, IconImport, IconJournal, IconSearch } from './Icons'
import Logo from './Logo'

const NAV = [
  { to: '/', label: 'Записи', icon: IconJournal, end: true },
  { to: '/search', label: 'Пошук', icon: IconSearch, end: false },
  { to: '/ask', label: 'Запитати', icon: IconAsk, end: false },
  { to: '/import', label: 'Імпорт', icon: IconImport, end: false },
]

/** Макет сторінок, що потребують входу: без сесії перенаправляє на /login. */
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

  const email = me.data.email
  return (
    <div className="app">
      <header className="topbar">
        <NavLink to="/" className="brand" aria-label="Pastline — на головну">
          <Logo />
        </NavLink>
        <nav className="topnav" aria-label="Розділи">
          {NAV.map(({ to, label, end }) => (
            <NavLink key={to} to={to} end={end}>
              {label}
            </NavLink>
          ))}
        </nav>
        {/* details/summary: випадне меню без власного стану і обробників кліків */}
        <details className="user-menu">
          <summary aria-label={'Обліковий запис ' + email}>
            <span className="avatar" aria-hidden="true">
              {email.charAt(0).toUpperCase()}
            </span>
            <span className="user-email">{email}</span>
          </summary>
          <div className="user-dropdown">
            <div className="muted small user-dropdown-email">{email}</div>
            <button className="ghost" onClick={() => logout.mutate()} disabled={logout.isPending}>
              Вийти
            </button>
          </div>
        </details>
      </header>

      <main className="content">
        <Outlet />
      </main>

      <nav className="tabbar" aria-label="Розділи">
        {NAV.map(({ to, label, icon: Icon, end }) => (
          <NavLink key={to} to={to} end={end}>
            <Icon />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
