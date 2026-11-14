import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { ApiError } from './api/client'
import App from './App'
import { meKey } from './auth/useAuth'
import './styles.css'

// Будь-який запит отримав 401 → сесія скінчилась: "хто я" = null, і захищений макет сам переведе на /login
function onError(error: Error) {
  if (error instanceof ApiError && error.status === 401) {
    queryClient.setQueryData(meKey, null)
  }
}

const queryClient = new QueryClient({
  queryCache: new QueryCache({ onError }),
  mutationCache: new MutationCache({ onError }),
  defaultOptions: {
    queries: {
      // Повторювати має сенс лише збої мережі і 5xx; 4xx від повтору не зміниться
      retry: (failureCount, error) => failureCount < 2 && !(error instanceof ApiError && error.status < 500),
      refetchOnWindowFocus: false,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
