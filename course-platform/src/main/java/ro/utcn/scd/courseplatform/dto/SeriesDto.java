package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.Series;

public record SeriesDto(
        Long id,
        @NotBlank @Size(max = 20) String name,
        @NotNull @Min(1) @Max(6) Integer studyYear,
        @NotBlank @Size(max = 100) String specialization
) {
    public static SeriesDto from(Series s) {
        return new SeriesDto(s.getId(), s.getName(), s.getStudyYear(), s.getSpecialization());
    }
}
