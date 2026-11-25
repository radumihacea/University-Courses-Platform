import { API_URL, http } from './http'
import { clearSession, loadSession } from './authStorage'
import type { AiStatus, ChatMessage, ChatResult, ChatStreamDone, Id } from './types'

export interface ChatBody {
  message: string
  history: ChatMessage[]
  subgroupId?: Id | null
  professorId?: Id | null
}

/** Un rand din raspunsul cu streaming (NDJSON = cate un obiect JSON pe rand). */
type StreamEvent =
  | { type: 'token'; text: string }
  | ({ type: 'done' } & ChatStreamDone)
  | { type: 'error'; message: string }

export const aiApi = {
  /** Intreaba asistentul si primeste raspunsul intreg. Ca admin, trebuie ales al cui orar se foloseste. */
  chat: async (body: ChatBody) =>
    // modelul ruleaza local, pe procesor: un raspuns poate dura pana la cateva zeci de secunde
    (await http.post<ChatResult>('/api/ai/chat', body, { timeout: 180_000 })).data,

  /**
   * Intreaba asistentul si primeste raspunsul bucata cu bucata: onToken este apelat pentru fiecare
   * bucata de text, imediat ce modelul o genereaza. Se foloseste fetch (nu axios), pentru ca axios
   * nu poate citi raspunsul in browser pe masura ce soseste.
   */
  chatStream: async (body: ChatBody, onToken: (text: string) => void, signal?: AbortSignal): Promise<ChatStreamDone> => {
    const session = loadSession()
    let response: Response
    try {
      response = await fetch(`${API_URL}/api/ai/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(session ? { Authorization: `Bearer ${session.token}` } : {}),
        },
        body: JSON.stringify(body),
        signal,
      })
    } catch (e) {
      if (e instanceof DOMException && e.name === 'AbortError') throw e
      throw new Error('Serverul nu răspunde. Verifică dacă aplicația Spring Boot rulează pe ' + API_URL + '.')
    }

    // erorile dinainte de raspuns vin ca JSON obisnuit (ApiError)
    if (!response.ok) {
      if (response.status === 401) {
        clearSession()
        window.location.assign('/login?expired=1')
      }
      const error = (await response.json().catch(() => null)) as { message?: string } | null
      throw new Error(error?.message ?? `Eroare ${response.status}.`)
    }
    if (!response.body) throw new Error('Browserul nu poate citi răspunsul pe bucăți.')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    const result: { done: ChatStreamDone | null } = { done: null }

    const handle = (line: string) => {
      if (!line.trim()) return
      const event = JSON.parse(line) as StreamEvent
      if (event.type === 'token') onToken(event.text)
      else if (event.type === 'done') result.done = event
      else throw new Error(event.message)
    }

    for (;;) {
      const { value, done: finished } = await reader.read()
      if (finished) break
      buffer += decoder.decode(value, { stream: true })
      // un rand poate sosi in doua bucati: procesam doar randurile complete
      let newline: number
      while ((newline = buffer.indexOf('\n')) >= 0) {
        handle(buffer.slice(0, newline))
        buffer = buffer.slice(newline + 1)
      }
    }
    handle(buffer + decoder.decode())

    if (!result.done) throw new Error('Răspunsul s-a întrerupt înainte de final.')
    return result.done
  },

  status: async () => (await http.get<AiStatus>('/api/ai/status')).data,
}
