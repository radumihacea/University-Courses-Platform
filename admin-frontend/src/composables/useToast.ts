import { reactive } from 'vue'

export type ToastColor = 'success' | 'error' | 'info' | 'warning'

interface ToastState {
  visible: boolean
  text: string
  color: ToastColor
}

/** Starea unica a mesajelor scurte (toast) afisate in coltul ecranului. */
const state = reactive<ToastState>({ visible: false, text: '', color: 'success' })

function show(text: string, color: ToastColor) {
  state.visible = false
  // mic delay ca un mesaj nou sa reporneasca animatia chiar daca precedentul e inca vizibil
  setTimeout(() => {
    state.text = text
    state.color = color
    state.visible = true
  }, 50)
}

export function useToast() {
  return {
    state,
    success: (text: string) => show(text, 'success'),
    error: (text: string) => show(text, 'error'),
    info: (text: string) => show(text, 'info'),
    warning: (text: string) => show(text, 'warning'),
  }
}
