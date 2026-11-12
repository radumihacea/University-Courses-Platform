import { http } from './http'
import type {
  ChangeRequest,
  Conflict,
  GenerateTimetableRequest,
  GenerationResult,
  Id,
  ProfessorLoad,
  Room,
  RoomOccupancy,
  RoomType,
  ScheduleEntry,
  SemesterWeek,
  TimeSlot,
  Timetable,
  WeekParity,
} from './types'

/** Operatiile pe orar si operatiile complexe (generare, publicare, conflicte, statistici). */
export const scheduleApi = {
  timetables: async () => (await http.get<Timetable[]>('/api/timetables')).data,

  timetable: async (id: Id) => (await http.get<Timetable>(`/api/timetables/${id}`)).data,

  generate: async (request: GenerateTimetableRequest) =>
    (await http.post<GenerationResult>('/api/timetables/generate', request)).data,

  publish: async (id: Id) => (await http.post<Timetable>(`/api/timetables/${id}/publish`)).data,

  deleteTimetable: async (id: Id) => {
    await http.delete(`/api/timetables/${id}`)
  },

  entries: async (id: Id, filter: { subgroupId?: Id | null; professorId?: Id | null; roomId?: Id | null }) =>
    (
      await http.get<ScheduleEntry[]>(`/api/timetables/${id}/entries`, {
        params: {
          subgroupId: filter.subgroupId ?? undefined,
          professorId: filter.professorId ?? undefined,
          roomId: filter.roomId ?? undefined,
        },
      })
    ).data,

  conflicts: async (id: Id) => (await http.get<Conflict[]>(`/api/timetables/${id}/conflicts`)).data,

  availableRooms: async (params: {
    timetableId: Id
    timeSlotId: Id
    parity: WeekParity
    minCapacity?: number
    type?: RoomType | null
  }) =>
    (
      await http.get<Room[]>('/api/rooms/available', {
        params: { ...params, type: params.type ?? undefined },
      })
    ).data,

  professorLoad: async (timetableId: Id) =>
    (await http.get<ProfessorLoad[]>('/api/statistics/professor-load', { params: { timetableId } })).data,

  roomOccupancy: async (timetableId: Id) =>
    (await http.get<RoomOccupancy[]>('/api/statistics/room-occupancy', { params: { timetableId } })).data,

  approveRequest: async (id: Id, adminComment: string | null) =>
    (await http.post<ChangeRequest>(`/api/change-requests/${id}/approve`, { adminComment })).data,

  rejectRequest: async (id: Id, adminComment: string | null) =>
    (await http.post<ChangeRequest>(`/api/change-requests/${id}/reject`, { adminComment })).data,

  timeSlots: async () => (await http.get<TimeSlot[]>('/api/time-slots')).data,

  semesterWeeks: async () => (await http.get<SemesterWeek[]>('/api/semester-weeks')).data,

  online: async () =>
    (await http.get<{ onlineUsers: number; openConnections: number }>('/api/notifications/online')).data,
}
