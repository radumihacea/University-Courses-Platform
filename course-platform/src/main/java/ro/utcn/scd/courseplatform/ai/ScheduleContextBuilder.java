package ro.utcn.scd.courseplatform.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ro.utcn.scd.courseplatform.dto.ScheduleEntryDto;
import ro.utcn.scd.courseplatform.model.SemesterWeek;
import ro.utcn.scd.courseplatform.model.enums.ActivityType;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;
import ro.utcn.scd.courseplatform.repository.SemesterWeekRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Transforma orarul unei persoane intr-un text scurt si clar pe care modelul AI il poate citi.
 *
 * - Partea "grea" (data de azi, saptamana para/impara, orele de azi si de maine) este calculata
 *   aici, in Java: un model mic poate gresi calculele, dar reformuleaza corect un text pregatit.
 * - Textul este cat mai compact (numele complete ale disciplinelor apar o singura data, intr-o
 *   legenda), pentru ca modelul il citeste la fiecare intrebare: mai putin text = raspuns mai rapid.
 */
@Component
@RequiredArgsConstructor
public class ScheduleContextBuilder {

    private static final Locale RO = Locale.forLanguageTag("ro");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", RO);
    private static final List<DayOfWeek> WORK_DAYS = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private final SemesterWeekRepository semesterWeekRepository;

    /**
     * @param showAudience true pentru profesori (le spune CUI predau: seria / grupa / semigrupa);
     *                     pentru studenti nu e necesar si doar ar lungi textul.
     */
    public String build(String subject, List<ScheduleEntryDto> week, LocalDate today, boolean showAudience) {
        StringBuilder sb = new StringBuilder();
        sb.append("Persoana: ").append(subject).append("\n");

        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        sb.append("Azi: ").append(today.format(DATE)).append(", ").append(weekInfo(today)).append("\n");
        sb.append("Maine: ").append(tomorrow.format(DATE)).append(", ").append(weekInfo(tomorrow)).append("\n");
        sb.append("Saptamana viitoare: ").append(weekInfo(nextMonday)).append("\n\n");

        if (week.isEmpty()) {
            sb.append("Orar: nu exista inca un orar publicat sau persoana nu are ore.\n");
            return sb.toString();
        }

        // Legenda disciplinelor (numele complet o singura data)
        Map<String, String> disciplines = new LinkedHashMap<>();
        week.forEach(e -> disciplines.putIfAbsent(e.disciplineCode(), e.disciplineName()));
        sb.append("Discipline: ");
        disciplines.forEach((code, name) -> sb.append(code).append(" = ").append(name).append("; "));
        sb.append("\n\n");

        sb.append("AZI:\n").append(forDate(week, today, showAudience));
        sb.append("MAINE:\n").append(forDate(week, tomorrow, showAudience)).append("\n");

        sb.append("ORAR SAPTAMANAL (S.I. = doar saptamanile impare, S.P. = doar saptamanile pare):\n");
        for (DayOfWeek day : WORK_DAYS) {
            List<ScheduleEntryDto> entries = week.stream().filter(e -> e.dayOfWeek() == day).toList();
            sb.append(dayName(day)).append(": ");
            if (entries.isEmpty()) {
                sb.append("nicio ora\n");
                continue;
            }
            sb.append("\n");
            for (ScheduleEntryDto e : entries) {
                sb.append("- ").append(line(e, showAudience));
                if (e.parity() != WeekParity.ALL) {
                    sb.append(e.parity() == WeekParity.ODD ? " [S.I.]" : " [S.P.]");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------

    /** Orele dintr-o anumita zi, deja filtrate dupa paritatea saptamanii. */
    private String forDate(List<ScheduleEntryDto> week, LocalDate date, boolean showAudience) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return "- weekend, nu sunt ore\n";
        }
        Optional<SemesterWeek> sw = semesterWeekRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
        if (sw.isEmpty()) {
            return "- in afara semestrului, nu sunt ore\n";
        }
        if (sw.get().isHoliday()) {
            return "- vacanta, nu sunt ore\n";
        }
        WeekParity parity = sw.get().getParity();
        List<ScheduleEntryDto> entries = week.stream()
                .filter(e -> e.dayOfWeek() == date.getDayOfWeek())
                .filter(e -> e.parity() == WeekParity.ALL || e.parity() == parity)
                .toList();
        if (entries.isEmpty()) {
            return "- nicio ora\n";
        }
        StringBuilder sb = new StringBuilder();
        entries.forEach(e -> sb.append("- ").append(line(e, showAudience)).append("\n"));
        return sb.toString();
    }

    private String weekInfo(LocalDate date) {
        Optional<SemesterWeek> sw = semesterWeekRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
        if (sw.isEmpty()) return "in afara semestrului";
        if (sw.get().isHoliday()) return "vacanta";
        return "saptamana " + sw.get().getWeekNumber() + " (" + (sw.get().getParity() == WeekParity.ODD ? "impara" : "para") + ")";
    }

    /** Ex: "10:00-12:00 SCD laborator, sala G6, Asistent_Lab_SCD" */
    private static String line(ScheduleEntryDto e, boolean showAudience) {
        return e.startTime().toString().substring(0, 5) + "-" + e.endTime().toString().substring(0, 5) + " "
                + e.disciplineCode() + " " + typeName(e.type())
                + ", sala " + e.roomCode()
                + ", " + e.professorName()
                + (showAudience && e.targetName() != null ? ", cu " + e.targetName() : "");
    }

    private static String typeName(ActivityType type) {
        return switch (type) {
            case COURSE -> "curs";
            case SEMINAR -> "seminar";
            case LAB -> "laborator";
            case PROJECT -> "proiect";
        };
    }

    private static String dayName(DayOfWeek day) {
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
