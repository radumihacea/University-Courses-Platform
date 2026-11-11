package ro.utcn.scd.courseplatform.dto;

import ro.utcn.scd.courseplatform.model.Timetable;
import ro.utcn.scd.courseplatform.model.enums.TimetableStatus;

import java.time.LocalDateTime;

public record TimetableDto(
        Long id,
        String name,
        String academicYear,
        Integer semester,
        TimetableStatus status,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        long entryCount
) {
    public static TimetableDto from(Timetable t, long entryCount) {
        return new TimetableDto(t.getId(), t.getName(), t.getAcademicYear(), t.getSemester(), t.getStatus(),
                t.getCreatedAt(), t.getPublishedAt(), entryCount);
    }
}
