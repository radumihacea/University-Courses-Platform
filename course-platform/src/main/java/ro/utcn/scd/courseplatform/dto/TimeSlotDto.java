package ro.utcn.scd.courseplatform.dto;

import ro.utcn.scd.courseplatform.model.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Slot orar (doar citire). label = text gata de afisat, ex: "Luni 08:00-10:00". */
public record TimeSlotDto(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String label
) {
    public static TimeSlotDto from(TimeSlot t) {
        return new TimeSlotDto(t.getId(), t.getDayOfWeek(), t.getStartTime(), t.getEndTime(), label(t));
    }

    public static String label(TimeSlot t) {
        return dayName(t.getDayOfWeek()) + " " + t.getStartTime() + "-" + t.getEndTime();
    }

    public static String dayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Luni";
            case TUESDAY -> "Marti";
            case WEDNESDAY -> "Miercuri";
            case THURSDAY -> "Joi";
            case FRIDAY -> "Vineri";
            case SATURDAY -> "Sambata";
            case SUNDAY -> "Duminica";
        };
    }
}
