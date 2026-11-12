import { computed, ref } from 'vue'

import { authApi } from '@/api/auth'
import { clearSession, loadSession, saveSession } from '@/api/authStorage'
import type { AuthSession } from '@/api/types'

const session = ref<AuthSession | null>(loadSession())

/** Autentificarea in panoul de admin. Doar conturile cu rolul ADMIN au acces. */
export function useAuth() {
  const user = computed(() => session.value)
  const isAuthenticated = computed(() => session.value !== null && session.value.role === 'ADMIN')

  async function login(email: string, password: string) {
    const result = await authApi.login(email, password)
    if (result.role !== 'ADMIN') {
      throw new Error('Acest panou este doar pentru administratori. Profesorii și studenții folosesc aplicația mobilă.')
    }
    saveSession(result)
    session.value = result
  }

  function logout() {
    clearSession()
    session.value = null
  }

  /** Reciteste sesiunea (ex: dupa ce a expirat intre timp). */
  function refresh() {
    session.value = loadSession()
  }

  return { user, isAuthenticated, login, logout, refresh }
}
