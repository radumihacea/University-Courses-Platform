package ro.utcn.scd.courseplatform.dto;

import ro.utcn.scd.courseplatform.model.SemesterWeek;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;

import java.time.LocalDate;

/** O saptamana din calendarul semestrului (doar citire). */
public record SemesterWeekDto(
        Long id,
        Integer weekNumber,
        LocalDate startDate,
        LocalDate endDate,
        WeekParity parity,
        boolean holiday
) {
    public static SemesterWeekDto from(SemesterWeek w) {
        return new SemesterWeekDto(w.getId(), w.getWeekNumber(), w.getStartDate(), w.getEndDate(),
                w.getParity(), w.isHoliday());
    }
}
