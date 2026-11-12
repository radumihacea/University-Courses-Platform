import 'vuetify/styles'
import '@mdi/font/css/materialdesignicons.css'

import { createVuetify, type ThemeDefinition } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { en, ro } from 'vuetify/locale'

import { loadSavedTheme } from '@/composables/useAppTheme'

/**
 * Culorile aplicatiei. Accentul (primary) este rosul UTCN.
 * Pentru a schimba nuanta, modifica doar valorile "primary" de mai jos.
 */
const light: ThemeDefinition = {
  dark: false,
  colors: {
    primary: '#9D1B30',
    secondary: '#475569',
    background: '#F5F6F8',
    surface: '#FFFFFF',
    'surface-variant': '#EEF0F3',
    'on-surface-variant': '#334155',
    success: '#1E8E5A',
    info: '#2563EB',
    warning: '#C27803',
    error: '#C62828',
  },
}

const dark: ThemeDefinition = {
  dark: true,
  colors: {
    primary: '#E2546A',
    secondary: '#94A3B8',
    background: '#0F1115',
    surface: '#171A21',
    'surface-variant': '#222733',
    'on-surface-variant': '#CBD5E1',
    success: '#3DBE85',
    info: '#60A5FA',
    warning: '#F0B429',
    error: '#F26D6D',
  },
}

export default createVuetify({
  components,
  directives,
  locale: {
    locale: 'ro',
    fallback: 'en',
    messages: { ro, en },
  },
  theme: {
    defaultTheme: loadSavedTheme(),
    themes: { light, dark },
  },
  // Stilul "modern minimal": carduri plate cu contur, colturi rotunjite, campuri conturate
  defaults: {
    VCard: { elevation: 0, rounded: 'lg', border: true },
    VBtn: { rounded: 'lg' },
    VTextField: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VSelect: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VAutocomplete: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VTextarea: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VNumberInput: { variant: 'outlined', density: 'comfortable', color: 'primary' },
    VSwitch: { color: 'primary', inset: true, hideDetails: 'auto' },
    VChip: { rounded: 'lg' },
    VDataTable: { hover: true, density: 'comfortable' },
    VDialog: { maxWidth: 640 },
  },
})
