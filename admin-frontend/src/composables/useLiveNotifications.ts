import { computed, reactive, readonly } from 'vue'

import { loadSession } from '@/api/authStorage'
import { api } from '@/api/crud'
import { WS_URL } from '@/api/http'
import type { AppNotification, LiveMessage } from '@/api/types'

const RECONNECT_DELAY_MS = 3_000

interface LiveState {
  connected: boolean
  items: AppNotification[]
}

const state = reactive<LiveState>({ connected: false, items: [] })
const listeners = new Set<(n: AppNotification) => void>()
let socket: WebSocket | null = null
let active = false
let reconnectTimer: ReturnType<typeof setTimeout> | undefined

function connect() {
  const session = loadSession()
  if (!active || !session) return

  socket = new WebSocket(`${WS_URL}/ws/notifications?userId=${session.userId}`)

  socket.onopen = () => {
    state.connected = true
  }

  socket.onmessage = (event: MessageEvent<string>) => {
    const data = JSON.parse(event.data) as LiveMessage
    if (data.type !== 'NOTIFICATION' || data.id === undefined) return
    const notification: AppNotification = {
      id: data.id,
      userId: data.userId ?? session.userId,
      message: data.message ?? '',
      read: false,
      createdAt: data.createdAt,
    }
    state.items.unshift(notification)
    listeners.forEach((listener) => listener(notification))
  }

  // Daca serverul este repornit, ne reconectam automat (cat timp utilizatorul e autentificat)
  socket.onclose = () => {
    state.connected = false
    if (active) reconnectTimer = setTimeout(connect, RECONNECT_DELAY_MS)
  }
}

async function loadExisting() {
  const session = loadSession()
  if (!session) return
  try {
    state.items = await api.notifications.list({ userId: session.userId })
  } catch {
    /* serverul poate fi oprit; lista se umple cand vin notificari noi */
  }
}

/** Porneste conexiunea WebSocket (o singura data, dupa autentificare). */
function start() {
  if (active) return
  active = true
  loadExisting()
  connect()
}

/** Inchide conexiunea (la deconectare). */
function stop() {
  active = false
  clearTimeout(reconnectTimer)
  socket?.close()
  socket = null
  state.connected = false
  state.items = []
}

/** Notificarile live ale utilizatorului autentificat (prin WebSocket). */
export function useLiveNotifications() {
  const unreadCount = computed(() => state.items.filter((n) => !n.read).length)

  async function markAllRead() {
    const unread = state.items.filter((n) => !n.read && n.id !== undefined)
    await Promise.all(unread.map((n) => api.notifications.update(n.id as number, { ...n, read: true })))
    unread.forEach((n) => (n.read = true))
  }

  function onNotification(listener: (n: AppNotification) => void) {
    listeners.add(listener)
    return () => {
      listeners.delete(listener)
    }
  }

  return { state: readonly(state), unreadCount, markAllRead, onNotification, start, stop }
}
