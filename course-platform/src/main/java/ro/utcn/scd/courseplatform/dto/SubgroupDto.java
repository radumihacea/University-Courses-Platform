package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.Subgroup;

public record SubgroupDto(
        Long id,
        @NotBlank @Size(max = 15) String name,
        @NotNull Long groupId,
        String groupCode,
        @NotNull @PositiveOrZero Integer studentCount
) {
    public static SubgroupDto from(Subgroup s) {
        return new SubgroupDto(s.getId(), s.getName(), s.getGroup().getId(), s.getGroup().getCode(),
                s.getStudentCount());
    }
}
