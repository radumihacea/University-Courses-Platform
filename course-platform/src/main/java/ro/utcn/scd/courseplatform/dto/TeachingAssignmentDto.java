package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ro.utcn.scd.courseplatform.model.TeachingAssignment;
import ro.utcn.scd.courseplatform.model.enums.ActivityType;
import ro.utcn.scd.courseplatform.model.enums.Frequency;

/**
 * Publicul activitatii se trimite printr-un singur id, in functie de tip:
 * COURSE -> seriesId, SEMINAR -> groupId, LAB / PROJECT -> subgroupId.
 * Campurile *Code / *Name / targetName sunt doar pentru afisare.
 */
public record TeachingAssignmentDto(
        Long id,
        @NotNull Long disciplineId,
        String disciplineCode,
        @NotNull Long professorId,
        String professorName,
        @NotNull ActivityType type,
        @NotNull @Min(1) @Max(4) Integer hoursPerSession,
        @NotNull Frequency frequency,
        Long preferredRoomId,
        String preferredRoomCode,
        Long seriesId,
        Long groupId,
        Long subgroupId,
        String targetName
) {
    public static TeachingAssignmentDto from(TeachingAssignment a) {
        String target = a.getSeries() != null ? a.getSeries().getName()
                : a.getGroup() != null ? a.getGroup().getCode()
                : a.getSubgroup() != null ? a.getSubgroup().getName()
                : null;
        return new TeachingAssignmentDto(
                a.getId(),
                a.getDiscipline().getId(), a.getDiscipline().getCode(),
                a.getProfessor().getId(), a.getProfessor().getUser().getFullName(),
                a.getType(), a.getHoursPerSession(), a.getFrequency(),
                a.getPreferredRoom() != null ? a.getPreferredRoom().getId() : null,
                a.getPreferredRoom() != null ? a.getPreferredRoom().getCode() : null,
                a.getSeries() != null ? a.getSeries().getId() : null,
                a.getGroup() != null ? a.getGroup().getId() : null,
                a.getSubgroup() != null ? a.getSubgroup().getId() : null,
                target);
    }
}
