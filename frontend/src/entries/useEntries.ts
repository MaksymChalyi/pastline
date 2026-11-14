import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { entriesApi, type EntryFilter } from '../api/entries'
import type { EntryInput } from '../api/types'

/**
 * Ключі кешу записів. Усі починаються з 'entries': після будь-якої зміни достатньо скинути цей префікс,
 * і всі списки та картки перезавантажаться (аналог @CacheEvict(allEntries = true)).
 */
export const entryKeys = {
  all: ['entries'] as const,
  list: (filter: EntryFilter) => ['entries', 'list', filter] as const,
  detail: (id: string) => ['entries', 'detail', id] as const,
}

export function useEntries(filter: EntryFilter) {
  return useQuery({
    queryKey: entryKeys.list(filter),
    queryFn: () => entriesApi.list(filter),
    // Під час переходу на іншу сторінку показуємо попередню, а не порожній екран
    placeholderData: keepPreviousData,
  })
}

export function useEntry(id: string | undefined) {
  return useQuery({
    queryKey: entryKeys.detail(id ?? ''),
    queryFn: () => entriesApi.get(id!),
    enabled: id !== undefined, // для нового запису завантажувати нічого
  })
}

export function useSaveEntry(id: string | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: EntryInput) => (id ? entriesApi.update(id, input) : entriesApi.create(input)),
    onSuccess: (saved) => {
      queryClient.setQueryData(entryKeys.detail(saved.id), saved)
      return queryClient.invalidateQueries({ queryKey: entryKeys.all })
    },
  })
}

export function useDeleteEntry() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => entriesApi.remove(id),
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: entryKeys.detail(id) })
      return queryClient.invalidateQueries({ queryKey: entryKeys.all })
    },
  })
}
