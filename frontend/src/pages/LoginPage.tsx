import { Link } from 'react-router'
import { useLogin } from '../auth/useAuth'
import AuthForm from './AuthForm'

export default function LoginPage() {
  const login = useLogin()
  return (
    <AuthForm
      title="Вхід"
      submitLabel="Увійти"
      pending={login.isPending}
      error={login.error}
      onSubmit={(email, password) => login.mutate({ email, password })}
      footer={
        <>
          Немає акаунта? <Link to="/register">Зареєструватися</Link>
        </>
      }
    />
  )
}
