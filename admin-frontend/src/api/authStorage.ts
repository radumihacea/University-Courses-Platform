import type { AuthSession } from './types'

/**
 * Sesiunea salvata in browser (localStorage), ca utilizatorul sa ramana autentificat
 * dupa refresh. Modul separat, fara dependinte, ca sa poata fi folosit si de clientul HTTP.
 */
const STORAGE_KEY = 'ucp-session'

export function loadSession(): AuthSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as AuthSession
    // sesiunile expirate se sterg automat
    if (!session.token || new Date(session.expiresAt).getTime() <= Date.now()) {
      localStorage.removeItem(STORAGE_KEY)
      return null
    }
    return session
  } catch {
    return null
  }
}

export function saveSession(session: AuthSession) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY)
}
