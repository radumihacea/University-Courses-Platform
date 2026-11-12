/** Configurarea unei coloane din tabel. */
export interface ColumnDef {
  title: string
  key: string
  sortable?: boolean
  align?: 'start' | 'center' | 'end'
  width?: string | number
}

/** Configurarea unui camp din formularul de adaugare / editare. */
export interface FieldDef {
  key: string
  label: string
  type?: 'text' | 'email' | 'password' | 'number' | 'textarea' | 'select' | 'autocomplete' | 'switch'
  /** Optiunile pentru select / autocomplete. */
  options?: ReadonlyArray<{ title: string; value: unknown; subtitle?: string }>
  /** Camp obligatoriu (la creare si la editare). */
  required?: boolean
  /** Camp obligatoriu doar la creare (ex: parola). */
  requiredOnCreate?: boolean
  /** Latimea pe un rand de 12 coloane (implicit 12). */
  cols?: number
  hint?: string
  min?: number
  max?: number
  clearable?: boolean
  /** Afiseaza campul doar in anumite situatii (ex: seriesId doar pentru cursuri). */
  visible?: (item: Record<string, unknown>, isNew: boolean) => boolean
}

export type FormItem = Record<string, unknown>
