package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.ChangeRequest;
import ro.utcn.scd.courseplatform.model.ScheduleEntry;
import ro.utcn.scd.courseplatform.model.TeachingAssignment;
import ro.utcn.scd.courseplatform.model.enums.ActivityType;
import ro.utcn.scd.courseplatform.model.enums.RequestStatus;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;

import java.time.LocalDateTime;

/**
 * Cerere de mutare a unei ore.
 * La creare / modificare se trimit doar: scheduleEntryId, professorId, proposedTimeSlotId,
 * proposedRoomId (optional) si reason. Restul campurilor sunt doar pentru afisare:
 * ce activitate se muta (disciplina, tip, public), de unde (slotul si sala actuale)
 * si starea cererii.
 */
public record ChangeRequestDto(
        Long id,
        @NotNull Long scheduleEntryId,
        @NotNull Long professorId,
        String professorName,
        @NotNull Long proposedTimeSlotId,
        String proposedTimeSlotLabel,
        Long proposedRoomId,
        String proposedRoomCode,
        @NotBlank @Size(max = 500) String reason,
        RequestStatus status,
        String adminComment,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String disciplineCode,
        ActivityType activityType,
        String targetName,
        String currentTimeSlotLabel,
        String currentRoomCode,
        WeekParity parity
) {
    public static ChangeRequestDto from(ChangeRequest c) {
        ScheduleEntry e = c.getScheduleEntry();
        TeachingAssignment a = e.getAssignment();
        String target = a.getSeries() != null ? a.getSeries().getName()
                : a.getGroup() != null ? a.getGroup().getCode()
                : a.getSubgroup() != null ? a.getSubgroup().getName() : null;
        return new ChangeRequestDto(
                c.getId(),
                e.getId(),
                c.getProfessor().getId(), c.getProfessor().getUser().getFullName(),
                c.getProposedTimeSlot().getId(), TimeSlotDto.label(c.getProposedTimeSlot()),
                c.getProposedRoom() != null ? c.getProposedRoom().getId() : null,
                c.getProposedRoom() != null ? c.getProposedRoom().getCode() : null,
                c.getReason(), c.getStatus(), c.getAdminComment(),
                c.getCreatedAt(), c.getResolvedAt(),
                a.getDiscipline().getCode(), a.getType(), target,
                TimeSlotDto.label(e.getTimeSlot()), e.getRoom().getCode(), e.getParity());
    }
}
