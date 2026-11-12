/**
 * Tipurile datelor primite / trimise catre backend.
 * Corespund DTO-urilor din pachetul ro.utcn.scd.courseplatform.dto.
 */

export type Id = number

export type Role = 'ADMIN' | 'PROFESSOR' | 'STUDENT'
export type ActivityType = 'COURSE' | 'SEMINAR' | 'LAB' | 'PROJECT'
export type Frequency = 'WEEKLY' | 'BIWEEKLY'
export type WeekParity = 'ALL' | 'ODD' | 'EVEN'
export type RoomType = 'LECTURE_HALL' | 'SEMINAR_ROOM' | 'LAB'
export type ConstraintType = 'UNAVAILABLE' | 'PREFERRED'
export type TimetableStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface Building {
  id?: Id
  name: string
  address: string
}

export interface Room {
  id?: Id
  code: string
  buildingId: Id | null
  buildingName?: string
  capacity: number | null
  type: RoomType | null
}

export interface Discipline {
  id?: Id
  code: string
  name: string
  credits: number | null
  studyYear: number | null
  optional: boolean
}

export interface Series {
  id?: Id
  name: string
  studyYear: number | null
  specialization: string
}

export interface StudentGroup {
  id?: Id
  code: string
  seriesId: Id | null
  seriesName?: string
}

export interface Subgroup {
  id?: Id
  name: string
  groupId: Id | null
  groupCode?: string
  studentCount: number | null
}

export interface Professor {
  id?: Id
  userId?: Id
  fullName: string
  email: string
  password?: string | null
  title: string | null
  department: string | null
}

export interface Student {
  id?: Id
  userId?: Id
  fullName: string
  email: string
  password?: string | null
  registrationNumber: string
  subgroupId: Id | null
  subgroupName?: string
}

export interface TeachingAssignment {
  id?: Id
  disciplineId: Id | null
  disciplineCode?: string
  professorId: Id | null
  professorName?: string
  type: ActivityType | null
  hoursPerSession: number | null
  frequency: Frequency | null
  preferredRoomId: Id | null
  preferredRoomCode?: string | null
  seriesId: Id | null
  groupId: Id | null
  subgroupId: Id | null
  targetName?: string | null
}

export interface ProfessorConstraint {
  id?: Id
  professorId: Id | null
  professorName?: string
  timeSlotId: Id | null
  timeSlotLabel?: string
  type: ConstraintType | null
  mandatory: boolean
  originalText: string | null
}

export interface ChangeRequest {
  id?: Id
  scheduleEntryId: Id
  professorId: Id
  professorName?: string
  proposedTimeSlotId: Id
  proposedTimeSlotLabel?: string
  proposedRoomId: Id | null
  proposedRoomCode?: string | null
  reason: string
  status?: RequestStatus
  adminComment?: string | null
  createdAt?: string
  resolvedAt?: string | null
  disciplineCode?: string
  activityType?: ActivityType
  targetName?: string | null
  currentTimeSlotLabel?: string
  currentRoomCode?: string
  parity?: WeekParity
}

export interface AppNotification {
  id?: Id
  userId: Id | null
  message: string
  read: boolean
  createdAt?: string
}

export interface TimeSlot {
  id: Id
  dayOfWeek: DayOfWeek
  startTime: string
  endTime: string
  label: string
}

export interface SemesterWeek {
  id: Id
  weekNumber: number | null
  startDate: string
  endDate: string
  parity: WeekParity
  holiday: boolean
}

export interface Timetable {
  id: Id
  name: string
  academicYear: string
  semester: number
  status: TimetableStatus
  createdAt: string
  publishedAt: string | null
  entryCount: number
}

export interface ScheduleEntry {
  id: Id
  timetableId: Id
  assignmentId: Id
  disciplineCode: string
  disciplineName: string
  type: ActivityType
  professorId: Id
  professorName: string
  roomId: Id
  roomCode: string
  timeSlotId: Id
  dayOfWeek: DayOfWeek
  dayName: string
  startTime: string
  endTime: string
  parity: WeekParity
  targetName: string | null
}

export interface GenerateTimetableRequest {
  name: string
  academicYear: string
  semester: number
  attempts?: number | null
  seed?: number | null
}

export interface GenerationResult {
  timetable: Timetable
  totalActivities: number
  placed: number
  unplaced: string[]
  score: number
  attempts: number
  durationMs: number
}

export interface Conflict {
  type: 'ROOM' | 'PROFESSOR' | 'STUDENTS' | 'CAPACITY'
  description: string
  firstEntryId: Id | null
  secondEntryId: Id | null
}

export interface ProfessorLoad {
  professorId: Id
  professorName: string
  activities: number
  weeklyHours: number
}

export interface RoomOccupancy {
  roomId: Id
  roomCode: string
  capacity: number
  activities: number
  occupiedSlotsPerWeek: number
  availableSlotsPerWeek: number
  occupancyPercent: number
}

/** Raspunsul la login (AuthResponse) - salvat in browser ca sesiune. */
export interface AuthSession {
  token: string
  expiresAt: string
  userId: Id
  fullName: string
  email: string
  role: Role
  professorId: Id | null
  studentId: Id | null
}

/** Mesajul primit prin WebSocket. */
export interface LiveMessage {
  type: 'CONNECTED' | 'NOTIFICATION' | 'PONG'
  id?: Id
  userId?: Id
  message?: string
  read?: boolean
  createdAt?: string
}
