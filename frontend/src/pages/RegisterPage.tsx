import { Link } from 'react-router'
import { useRegister } from '../auth/useAuth'
import AuthForm from './AuthForm'

export default function RegisterPage() {
  const register = useRegister()
  return (
    <AuthForm
      title="Реєстрація"
      submitLabel="Створити акаунт"
      passwordHint="Від 8 до 128 символів"
      pending={register.isPending}
      error={register.error}
      onSubmit={(email, password) => register.mutate({ email, password })}
      footer={
        <>
          Уже є акаунт? <Link to="/login">Увійти</Link>
        </>
      }
    />
  )
}
