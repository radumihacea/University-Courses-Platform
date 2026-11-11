package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.Building;

public record BuildingDto(
        Long id,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 200) String address
) {
    public static BuildingDto from(Building b) {
        return new BuildingDto(b.getId(), b.getName(), b.getAddress());
    }
}
