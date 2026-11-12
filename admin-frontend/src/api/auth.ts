import { http } from './http'
import type { AuthSession } from './types'

export const authApi = {
  login: async (email: string, password: string) =>
    (await http.post<AuthSession>('/api/auth/login', { email, password })).data,

  me: async () => (await http.get<AuthSession>('/api/auth/me')).data,
}
