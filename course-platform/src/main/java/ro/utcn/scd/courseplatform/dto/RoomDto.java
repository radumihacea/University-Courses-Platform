package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.Room;
import ro.utcn.scd.courseplatform.model.enums.RoomType;

/** buildingName este doar pentru afisare; la creare/modificare se trimite buildingId. */
public record RoomDto(
        Long id,
        @NotBlank @Size(max = 30) String code,
        @NotNull Long buildingId,
        String buildingName,
        @NotNull @Positive Integer capacity,
        @NotNull RoomType type
) {
    public static RoomDto from(Room r) {
        return new RoomDto(r.getId(), r.getCode(), r.getBuilding().getId(), r.getBuilding().getName(),
                r.getCapacity(), r.getType());
    }
}
