package ro.utcn.scd.courseplatform.dto;

/** Un conflict gasit intr-un orar. type: ROOM, PROFESSOR sau STUDENTS. */
public record ConflictDto(
        String type,
        String description,
        Long firstEntryId,
        Long secondEntryId
) {
}
