import type {
  ActivityType,
  ConstraintType,
  Frequency,
  RequestStatus,
  RoomType,
  TimetableStatus,
  WeekParity,
} from '@/api/types'

/** Etichetele in romana pentru valorile din backend + culorile folosite in interfata. */

export const activityTypeLabel: Record<ActivityType, string> = {
  COURSE: 'Curs',
  SEMINAR: 'Seminar',
  LAB: 'Laborator',
  PROJECT: 'Proiect',
}

export const activityTypeColor: Record<ActivityType, string> = {
  COURSE: 'primary',
  SEMINAR: 'info',
  LAB: 'success',
  PROJECT: 'warning',
}

export const frequencyLabel: Record<Frequency, string> = {
  WEEKLY: 'Săptămânal',
  BIWEEKLY: 'O dată la 2 săptămâni',
}

export const parityLabel: Record<WeekParity, string> = {
  ALL: 'Săptămânal',
  ODD: 'Săpt. impare',
  EVEN: 'Săpt. pare',
}

export const parityShort: Record<WeekParity, string> = {
  ALL: '',
  ODD: 'S.I.',
  EVEN: 'S.P.',
}

export const roomTypeLabel: Record<RoomType, string> = {
  LECTURE_HALL: 'Amfiteatru',
  SEMINAR_ROOM: 'Sală de seminar',
  LAB: 'Laborator',
}

export const constraintTypeLabel: Record<ConstraintType, string> = {
  UNAVAILABLE: 'Indisponibil',
  PREFERRED: 'Preferință',
}

export const timetableStatusLabel: Record<TimetableStatus, string> = {
  DRAFT: 'Ciornă',
  PUBLISHED: 'Publicat',
  ARCHIVED: 'Arhivat',
}

export const timetableStatusColor: Record<TimetableStatus, string> = {
  DRAFT: 'warning',
  PUBLISHED: 'success',
  ARCHIVED: 'secondary',
}

export const requestStatusLabel: Record<RequestStatus, string> = {
  PENDING: 'În așteptare',
  APPROVED: 'Aprobată',
  REJECTED: 'Respinsă',
}

export const requestStatusColor: Record<RequestStatus, string> = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
}

/** Transforma un dictionar de etichete in optiuni pentru v-select. */
export function enumOptions<K extends string>(labels: Record<K, string>) {
  return (Object.keys(labels) as K[]).map((value) => ({ title: labels[value], value }))
}

/** "2026-11-12T10:15:00" -> "12.11.2026, 10:15" */
export function formatDateTime(value?: string | null): string {
  if (!value) return '-'
  const date = new Date(value)
  return date.toLocaleString('ro-RO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** "08:00:00" -> "08:00" */
export function formatTime(value: string): string {
  return value.slice(0, 5)
}
