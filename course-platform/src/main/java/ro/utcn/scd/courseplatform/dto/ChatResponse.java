package ro.utcn.scd.courseplatform.dto;

/** Raspunsul asistentului AI. subject = al cui orar a fost folosit. */
public record ChatResponse(
        String answer,
        String model,
        String subject,
        long durationMs
) {
}
