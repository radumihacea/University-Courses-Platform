import { http } from './http'
import type {
  AppNotification,
  Building,
  ChangeRequest,
  Discipline,
  Id,
  Professor,
  ProfessorConstraint,
  Room,
  Series,
  Student,
  StudentGroup,
  Subgroup,
  TeachingAssignment,
} from './types'

export type QueryParams = Record<string, string | number | boolean | null | undefined>

/** Operatiile CRUD standard expuse de backend pentru o resursa. */
export interface CrudApi<T> {
  list(params?: QueryParams): Promise<T[]>
  get(id: Id): Promise<T>
  create(body: T): Promise<T>
  update(id: Id, body: T): Promise<T>
  remove(id: Id): Promise<void>
}

/** Parametrii null / goi nu se trimit (backend-ul ii trateaza ca "fara filtru"). */
function clean(params?: QueryParams): QueryParams | undefined {
  if (!params) return undefined
  const result: QueryParams = {}
  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined && value !== '') result[key] = value
  }
  return result
}

export function crudApi<T>(path: string): CrudApi<T> {
  return {
    list: async (params) => (await http.get<T[]>(path, { params: clean(params) })).data,
    get: async (id) => (await http.get<T>(`${path}/${id}`)).data,
    create: async (body) => (await http.post<T>(path, body)).data,
    update: async (id, body) => (await http.put<T>(`${path}/${id}`, body)).data,
    remove: async (id) => {
      await http.delete(`${path}/${id}`)
    },
  }
}

/** Toate resursele CRUD ale aplicatiei, intr-un singur loc. */
export const api = {
  buildings: crudApi<Building>('/api/buildings'),
  rooms: crudApi<Room>('/api/rooms'),
  disciplines: crudApi<Discipline>('/api/disciplines'),
  series: crudApi<Series>('/api/series'),
  groups: crudApi<StudentGroup>('/api/groups'),
  subgroups: crudApi<Subgroup>('/api/subgroups'),
  professors: crudApi<Professor>('/api/professors'),
  students: crudApi<Student>('/api/students'),
  assignments: crudApi<TeachingAssignment>('/api/teaching-assignments'),
  constraints: crudApi<ProfessorConstraint>('/api/professor-constraints'),
  changeRequests: crudApi<ChangeRequest>('/api/change-requests'),
  notifications: crudApi<AppNotification>('/api/notifications'),
}
