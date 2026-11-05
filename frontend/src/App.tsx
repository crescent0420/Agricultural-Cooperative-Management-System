import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import type { ApiError } from './api/errors'
import { AuthProvider } from './auth/AuthProvider'
import { ToastProvider } from './components/toast/ToastProvider'
import { AppRouter } from './routes/AppRouter'

/** Retry a failed read once, and only when the server was unreachable or failed (never 4xx answers). */
function shouldRetry(failureCount: number, error: unknown): boolean {
  const status = (error as ApiError | undefined)?.status ?? 0
  return failureCount < 1 && (status === 0 || status >= 500)
}

function App() {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { retry: shouldRetry, refetchOnWindowFocus: false },
          mutations: { retry: false },
        },
      }),
  )
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <AppRouter />
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>
  )
}

export default App
