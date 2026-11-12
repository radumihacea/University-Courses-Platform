import axios from 'axios'

import { clearSession, loadSession } from './authStorage'

/** Adresa backend-ului (se poate schimba din .env.local cu VITE_API_URL). */
export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

/** Adresa WebSocket derivata din API_URL: http -> ws, https -> wss. */
export const WS_URL: string = API_URL.replace(/^http/, 'ws')

export const http = axios.create({
  baseURL: API_URL,
  timeout: 60_000,
})

/** Fiecare cerere trimite tokenul primit la login. */
http.interceptors.request.use((config) => {
  const session = loadSession()
  if (session) {
    config.headers.Authorization = `Bearer ${session.token}`
  }
  return config
})

/** Daca serverul raspunde 401 (token expirat / invalid), utilizatorul este trimis la login. */
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginRequest = axios.isAxiosError(error) && error.config?.url?.includes('/api/auth/login')
    if (axios.isAxiosError(error) && error.response?.status === 401 && !isLoginRequest) {
      clearSession()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login?expired=1')
      }
    }
    return Promise.reject(error)
  },
)

/** Formatul erorilor trimise de backend (ApiError). */
interface ApiErrorBody {
  status?: number
  message?: string
  details?: string[]
}

/** Transforma orice eroare intr-un mesaj clar, in romana, pentru utilizator. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      return 'Serverul nu răspunde. Verifică dacă aplicația Spring Boot rulează pe ' + API_URL + '.'
    }
    const body = error.response.data as ApiErrorBody | undefined
    if (body?.message) {
      return body.details && body.details.length > 0
        ? `${body.message} ${body.details.join('; ')}`
        : body.message
    }
    return `Eroare ${error.response.status}.`
  }
  return error instanceof Error ? error.message : String(error)
}
