import { computed, ref, type Ref } from 'vue'

import { api } from '@/api/crud'
import { scheduleApi } from '@/api/schedule'
import type {
  Building,
  Discipline,
  Professor,
  Room,
  Series,
  Student,
  StudentGroup,
  Subgroup,
  TimeSlot,
} from '@/api/types'

/** O optiune dintr-un camp de selectie (v-select / v-autocomplete). */
export interface Option {
  title: string
  value: number
  subtitle?: string
}

/**
 * Liste folosite in campurile de selectie (cladiri, sali, profesori etc.).
 * Se incarca o singura data si se pastreaza in memorie; dupa o modificare
 * se reincarca doar lista afectata (reload).
 */
const buildings = ref<Building[]>([])
const rooms = ref<Room[]>([])
const series = ref<Series[]>([])
const groups = ref<StudentGroup[]>([])
const subgroups = ref<Subgroup[]>([])
const disciplines = ref<Discipline[]>([])
const professors = ref<Professor[]>([])
const students = ref<Student[]>([])
const timeSlots = ref<TimeSlot[]>([])

const loaders = {
  buildings: async () => (buildings.value = await api.buildings.list()),
  rooms: async () => (rooms.value = await api.rooms.list()),
  series: async () => (series.value = await api.series.list()),
  groups: async () => (groups.value = await api.groups.list()),
  subgroups: async () => (subgroups.value = await api.subgroups.list()),
  disciplines: async () => (disciplines.value = await api.disciplines.list()),
  professors: async () => (professors.value = await api.professors.list()),
  students: async () => (students.value = await api.students.list()),
  timeSlots: async () => (timeSlots.value = await scheduleApi.timeSlots()),
}

export type LookupName = keyof typeof loaders

const loaded = new Set<LookupName>()

async function ensure(...names: LookupName[]) {
  await Promise.all(
    names
      .filter((name) => !loaded.has(name))
      .map(async (name) => {
        await loaders[name]()
        loaded.add(name)
      }),
  )
}

async function reload(...names: LookupName[]) {
  await Promise.all(names.map((name) => loaders[name]()))
  names.forEach((name) => loaded.add(name))
}

function options<T extends { id?: number }>(
  list: Ref<T[]>,
  title: (item: T) => string,
  subtitle?: (item: T) => string,
) {
  return computed<Option[]>(() =>
    list.value
      .filter((item) => item.id !== undefined)
      .map((item) => ({ title: title(item), value: item.id as number, subtitle: subtitle?.(item) })),
  )
}

export function useLookups() {
  return {
    ensure,
    reload,
    buildings,
    rooms,
    series,
    groups,
    subgroups,
    disciplines,
    professors,
    students,
    timeSlots,
    buildingOptions: options(buildings, (b) => b.name),
    roomOptions: options(rooms, (r) => r.code, (r) => `${r.capacity} locuri`),
    seriesOptions: options(series, (s) => s.name, (s) => s.specialization),
    groupOptions: options(groups, (g) => g.code),
    subgroupOptions: options(subgroups, (s) => s.name),
    disciplineOptions: options(disciplines, (d) => `${d.code} - ${d.name}`),
    professorOptions: options(professors, (p) => p.fullName),
    timeSlotOptions: computed<Option[]>(() => timeSlots.value.map((t) => ({ title: t.label, value: t.id }))),
    /** Destinatarii posibili ai unei notificari: profesori + studenti (dupa id-ul contului). */
    userOptions: computed<Option[]>(() => [
      ...professors.value
        .filter((p) => p.userId !== undefined)
        .map((p) => ({ title: p.fullName, value: p.userId as number, subtitle: 'Profesor' })),
      ...students.value
        .filter((s) => s.userId !== undefined)
        .map((s) => ({ title: s.fullName, value: s.userId as number, subtitle: `Student · ${s.subgroupName ?? ''}` })),
    ]),
  }
}
