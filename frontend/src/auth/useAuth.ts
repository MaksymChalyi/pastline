import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { authApi } from '../api/auth'
import { ApiError } from '../api/client'
import type { User } from '../api/types'

/** Ключ кешу "хто я". Аналог SecurityContextHolder, тільки на клієнті: одне джерело правди для всіх сторінок. */
export const meKey = ['me'] as const

/**
 * Поточний користувач: User, null (не увійшов) або undefined (ще завантажується).
 * 401 тут не помилка, а відповідь "сесії немає".
 */
export function useMe() {
  return useQuery<User | null>({
    queryKey: meKey,
    queryFn: async () => {
      try {
        return await authApi.me()
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) {
          return null
        }
        throw e
      }
    },
    staleTime: Infinity, // змінюється лише при вході/виході, і тоді ми оновлюємо кеш самі
  })
}

export function useLogin() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) => authApi.login(email, password),
    onSuccess: (user) => {
      queryClient.setQueryData(meKey, user)
      navigate('/', { replace: true })
    },
  })
}

/** Реєстрація не створює сесію (так на бекенді), тому одразу після неї входимо тими ж даними. */
export function useRegister() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: async ({ email, password }: { email: string; password: string }) => {
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
      await authApi.register(email, password, timezone)
      return authApi.login(email, password)
    },
    onSuccess: (user) => {
      queryClient.setQueryData(meKey, user)
      navigate('/', { replace: true })
    },
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: authApi.logout,
    onSettled: () => {
      // Дані попереднього користувача не мають лишитись у кеші
      queryClient.clear()
      queryClient.setQueryData(meKey, null)
      navigate('/login', { replace: true })
    },
  })
}
