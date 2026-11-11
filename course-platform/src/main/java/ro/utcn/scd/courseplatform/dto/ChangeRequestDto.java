package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.ChangeRequest;
import ro.utcn.scd.courseplatform.model.enums.RequestStatus;

import java.time.LocalDateTime;

/**
 * Cerere de mutare a unei ore. status, adminComment, createdAt si resolvedAt
 * sunt doar pentru citire: aprobarea/respingerea se face printr-o operatie separata (pasul urmator).
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
        LocalDateTime resolvedAt
) {
    public static ChangeRequestDto from(ChangeRequest c) {
        return new ChangeRequestDto(
                c.getId(),
                c.getScheduleEntry().getId(),
                c.getProfessor().getId(), c.getProfessor().getUser().getFullName(),
                c.getProposedTimeSlot().getId(), TimeSlotDto.label(c.getProposedTimeSlot()),
                c.getProposedRoom() != null ? c.getProposedRoom().getId() : null,
                c.getProposedRoom() != null ? c.getProposedRoom().getCode() : null,
                c.getReason(), c.getStatus(), c.getAdminComment(),
                c.getCreatedAt(), c.getResolvedAt());
    }
}
