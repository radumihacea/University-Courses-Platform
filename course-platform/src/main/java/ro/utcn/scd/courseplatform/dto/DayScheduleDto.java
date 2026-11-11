package ro.utcn.scd.courseplatform.dto;

import ro.utcn.scd.courseplatform.model.enums.WeekParity;

import java.time.LocalDate;
import java.util.List;

/**
 * Orarul unei zile pentru un student sau profesor (folosit de aplicatia mobila).
 * message explica situatiile speciale: weekend, vacanta, in afara semestrului, niciun orar publicat.
 */
public record DayScheduleDto(
        LocalDate date,
        Integer weekNumber,
        WeekParity parity,
        boolean holiday,
        String message,
        List<ScheduleEntryDto> entries
) {
}
