package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.StudentGroup;

public record StudentGroupDto(
        Long id,
        @NotBlank @Size(max = 10) String code,
        @NotNull Long seriesId,
        String seriesName
) {
    public static StudentGroupDto from(StudentGroup g) {
        return new StudentGroupDto(g.getId(), g.getCode(), g.getSeries().getId(), g.getSeries().getName());
    }
}
