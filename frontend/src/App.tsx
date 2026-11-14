import { Navigate, Route, Routes } from 'react-router'
import AppLayout from './layout/AppLayout'
import PublicOnly from './layout/PublicOnly'
import EntriesPage from './pages/EntriesPage'
import EntryEditorPage from './pages/EntryEditorPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'

/** Маршрути на клієнті: аналог @RequestMapping, тільки сторінку обирає браузер без запиту до сервера. */
export default function App() {
  return (
    <Routes>
      <Route element={<PublicOnly />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>
      <Route element={<AppLayout />}>
        <Route index element={<EntriesPage />} />
        <Route path="/entries/new" element={<EntryEditorPage />} />
        <Route path="/entries/:id" element={<EntryEditorPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
