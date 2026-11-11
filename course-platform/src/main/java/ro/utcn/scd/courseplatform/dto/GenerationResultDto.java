package ro.utcn.scd.courseplatform.dto;

import java.util.List;

/** Raportul generarii: cate activitati au fost plasate si de ce au ramas unele neplasate. */
public record GenerationResultDto(
        TimetableDto timetable,
        int totalActivities,
        int placed,
        List<String> unplaced,
        double score,
        int attempts,
        long durationMs
) {
}
