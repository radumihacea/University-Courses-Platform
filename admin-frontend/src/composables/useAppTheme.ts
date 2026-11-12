import { computed } from 'vue'
import { useTheme } from 'vuetify'

const STORAGE_KEY = 'ucp-theme'

/** Tema salvata in browser; daca nu exista, urmeaza setarea sistemului de operare. */
export function loadSavedTheme(): 'light' | 'dark' | 'system' {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved === 'light' || saved === 'dark' ? saved : 'system'
  } catch {
    return 'system'
  }
}

/** Comutarea intre tema luminoasa si cea intunecata, cu memorarea alegerii. */
export function useAppTheme() {
  const theme = useTheme()
  const isDark = computed(() => theme.current.value.dark)

  function toggle() {
    const next = isDark.value ? 'light' : 'dark'
    theme.change(next)
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      /* browserul nu permite stocarea - tema se pastreaza doar pana la refresh */
    }
  }

  return { isDark, toggle }
}
