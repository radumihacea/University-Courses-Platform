package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.Discipline;

public record DisciplineDto(
        Long id,
        @NotBlank @Size(max = 15) String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull @Min(1) @Max(30) Integer credits,
        @NotNull @Min(1) @Max(6) Integer studyYear,
        Boolean optional
) {
    public static DisciplineDto from(Discipline d) {
        return new DisciplineDto(d.getId(), d.getCode(), d.getName(), d.getCredits(),
                d.getStudyYear(), d.isOptional());
    }
}
