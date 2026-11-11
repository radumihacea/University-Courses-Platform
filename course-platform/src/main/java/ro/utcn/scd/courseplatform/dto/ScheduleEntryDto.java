package ro.utcn.scd.courseplatform.dto;

import ro.utcn.scd.courseplatform.model.ScheduleEntry;
import ro.utcn.scd.courseplatform.model.TeachingAssignment;
import ro.utcn.scd.courseplatform.model.enums.ActivityType;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;

/** O ora din orar, cu toate informatiile necesare afisarii (grila web, lista pe mobil). */
public record ScheduleEntryDto(
        Long id,
        Long timetableId,
        Long assignmentId,
        String disciplineCode,
        String disciplineName,
        ActivityType type,
        Long professorId,
        String professorName,
        Long roomId,
        String roomCode,
        Long timeSlotId,
        DayOfWeek dayOfWeek,
        String dayName,
        LocalTime startTime,
        LocalTime endTime,
        WeekParity parity,
        String targetName
) {
    /** Ordinea fireasca: luni -> vineri, apoi dupa ora. */
    public static final Comparator<ScheduleEntryDto> CHRONOLOGICAL =
            Comparator.comparing(ScheduleEntryDto::dayOfWeek).thenComparing(ScheduleEntryDto::startTime);

    public static ScheduleEntryDto from(ScheduleEntry e) {
        TeachingAssignment a = e.getAssignment();
        String target = a.getSeries() != null ? a.getSeries().getName()
                : a.getGroup() != null ? a.getGroup().getCode()
                : a.getSubgroup() != null ? a.getSubgroup().getName()
                : null;
        return new ScheduleEntryDto(
                e.getId(),
                e.getTimetable().getId(),
                a.getId(),
                a.getDiscipline().getCode(),
                a.getDiscipline().getName(),
                a.getType(),
                a.getProfessor().getId(),
                a.getProfessor().getUser().getFullName(),
                e.getRoom().getId(),
                e.getRoom().getCode(),
                e.getTimeSlot().getId(),
                e.getTimeSlot().getDayOfWeek(),
                TimeSlotDto.dayName(e.getTimeSlot().getDayOfWeek()),
                e.getTimeSlot().getStartTime(),
                e.getTimeSlot().getEndTime(),
                e.getParity(),
                target);
    }
}
