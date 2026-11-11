package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.ProfessorConstraint;
import ro.utcn.scd.courseplatform.model.enums.ConstraintType;

public record ProfessorConstraintDto(
        Long id,
        @NotNull Long professorId,
        String professorName,
        @NotNull Long timeSlotId,
        String timeSlotLabel,
        @NotNull ConstraintType type,
        Boolean mandatory,
        @Size(max = 1000) String originalText
) {
    public static ProfessorConstraintDto from(ProfessorConstraint c) {
        return new ProfessorConstraintDto(
                c.getId(),
                c.getProfessor().getId(), c.getProfessor().getUser().getFullName(),
                c.getTimeSlot().getId(), TimeSlotDto.label(c.getTimeSlot()),
                c.getType(), c.isMandatory(), c.getOriginalText());
    }
}
