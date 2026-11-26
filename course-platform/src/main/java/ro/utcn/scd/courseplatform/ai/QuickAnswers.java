package ro.utcn.scd.courseplatform.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ro.utcn.scd.courseplatform.dto.ScheduleEntryDto;
import ro.utcn.scd.courseplatform.model.SemesterWeek;
import ro.utcn.scd.courseplatform.model.enums.ActivityType;
import ro.utcn.scd.courseplatform.model.enums.WeekParity;
import ro.utcn.scd.courseplatform.repository.SemesterWeekRepository;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Raspunsuri instant, fara modelul AI, pentru intrebarile simple si frecvente:
 *   - "Ce ore am azi / maine / miercuri?", "Am ore vineri?", "Si joi?"
 *   - "In ce sala e laboratorul de SCD?", "Cand am curs de MAD?"
 *   - "Cate ore am pe saptamana?"
 * Raspunsul se construieste direct din orar (deci este mereu corect) in cateva milisecunde.
 * Orice alta intrebare (mai complicata) merge la modelul AI.
 */
@Component
@RequiredArgsConstructor
public class QuickAnswers {

    private static final Locale RO = Locale.forLanguageTag("ro");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM", RO);

    private static final Map<String, DayOfWeek> DAY_WORDS = Map.of(
            "luni", DayOfWeek.MONDAY, "marti", DayOfWeek.TUESDAY, "miercuri", DayOfWeek.WEDNESDAY,
            "joi", DayOfWeek.THURSDAY, "vineri", DayOfWeek.FRIDAY, "sambata", DayOfWeek.SATURDAY,
            "duminica", DayOfWeek.SUNDAY);

    /** Cuvinte care arata ca intrebarea este despre orar. */
    private static final Set<String> SCHEDULE_WORDS = new HashSet<>(Arrays.asList(
            "ore", "ora", "orar", "orarul", "program", "programul", "am", "predau", "cursuri", "liber", "libera",
            "activitati", "laboratoare", "seminare", "facultate", "scoala"));

    private final SemesterWeekRepository semesterWeekRepository;

    public Optional<String> answer(String question, List<ScheduleEntryDto> week, LocalDate today, boolean showAudience) {
        String q = normalize(question);
        List<String> words = Arrays.asList(q.split("[^a-z0-9]+"));
        if (words.isEmpty()) {
            return Optional.empty();
        }

        // "de ce ...", "cum pot ...", "pot muta ..." -> modelul AI
        if (q.startsWith("de ce") || q.contains("cum pot") || words.contains("muta") || words.contains("schimba")
                || words.contains("recomanzi") || words.contains("sfat")) {
            return Optional.empty();
        }

        LocalDate date = dateFrom(words, today);
        String discipline = disciplineFrom(q, words, week);
        ActivityType type = typeFrom(words);

        // "Cate ore am pe saptamana?"
        if (words.contains("cate") && q.contains("saptaman") && discipline == null) {
            return Optional.of(weeklyCount(week));
        }
        // "In ce sala e laboratorul de SCD?", "Cand am curs de MAD?" (eventual intr-o anumita zi)
        if (discipline != null) {
            return Optional.of(disciplineAnswer(week, discipline, type, date, today, showAudience));
        }
        // "Ce ore am miercuri?", "Am ore azi?", "Si joi?"
        if (date != null && (words.stream().anyMatch(SCHEDULE_WORDS::contains) || words.size() <= 3)) {
            return Optional.of(dayAnswer(week, date, today, showAudience));
        }
        return Optional.empty();
    }

    // ------------------------------------------------------------------ ce s-a intrebat

    /** azi, maine, poimaine sau o zi a saptamanii (cea mai apropiata, inclusiv azi). */
    private static LocalDate dateFrom(List<String> words, LocalDate today) {
        if (words.contains("azi") || words.contains("astazi")) {
            return today;
        }
        if (words.contains("poimaine")) {
            return today.plusDays(2);
        }
        if (words.contains("maine")) {
            return today.plusDays(1);
        }
        for (String w : words) {
            DayOfWeek day = DAY_WORDS.get(w);
            if (day != null) {
                return today.with(TemporalAdjusters.nextOrSame(day));
            }
        }
        return null;
    }

    /** Codul disciplinei (ex. "SCD") daca apare in intrebare - dupa cod sau dupa numele complet. */
    private static String disciplineFrom(String q, List<String> words, List<ScheduleEntryDto> week) {
        for (ScheduleEntryDto e : week) {
            String code = normalize(e.disciplineCode());
            if (words.contains(code) || q.contains(normalize(e.disciplineName()))) {
                return e.disciplineCode();
            }
        }
        return null;
    }

    private static ActivityType typeFrom(List<String> words) {
        for (String w : words) {
            if (w.startsWith("laborator") || w.equals("lab") || w.equals("labul")) return ActivityType.LAB;
            if (w.equals("curs") || w.equals("cursul")) return ActivityType.COURSE;
            if (w.startsWith("seminar")) return ActivityType.SEMINAR;
            if (w.startsWith("proiect")) return ActivityType.PROJECT;
        }
        return null;
    }

    // ------------------------------------------------------------------ raspunsuri

    private String dayAnswer(List<ScheduleEntryDto> week, LocalDate date, LocalDate today, boolean showAudience) {
        String when = when(date, today);
        DayStatus status = status(date);
        if (status.reason != null) {
            return when + " " + status.reason + " – nu ai ore.";
        }
        List<ScheduleEntryDto> entries = forDate(week, date, status.parity);
        if (entries.isEmpty()) {
            return when + " (" + status.weekLabel + ") nu ai ore.";
        }
        StringBuilder sb = new StringBuilder(when).append(" (").append(status.weekLabel).append(") ai ")
                .append(entries.size() == 1 ? "o activitate" : entries.size() + " activități").append(":\n");
        entries.forEach(e -> sb.append("• ").append(line(e, showAudience)).append("\n"));
        return sb.toString().trim();
    }

    private String disciplineAnswer(List<ScheduleEntryDto> week, String code, ActivityType type, LocalDate date,
                                    LocalDate today, boolean showAudience) {
        List<ScheduleEntryDto> entries = week.stream()
                .filter(e -> e.disciplineCode().equals(code))
                .filter(e -> type == null || e.type() == type)
                .toList();
        String what = (type != null ? typeName(type) + " de " : "") + code;
        if (entries.isEmpty()) {
            return "Nu ai " + what + " în orar.";
        }
        if (date != null) {
            DayStatus status = status(date);
            String when = when(date, today);
            if (status.reason != null) {
                return when + " " + status.reason + " – nu ai " + what + ".";
            }
            List<ScheduleEntryDto> that = forDate(entries, date, status.parity);
            if (that.isEmpty()) {
                return when + " nu ai " + what + ".";
            }
            StringBuilder sb = new StringBuilder(capitalize(what)).append(" ").append(when.toLowerCase(RO)).append(":\n");
            that.forEach(e -> sb.append("• ").append(line(e, showAudience)).append("\n"));
            return sb.toString().trim();
        }
        StringBuilder sb = new StringBuilder(capitalize(what)).append(" (").append(entries.get(0).disciplineName())
                .append("):\n");
        for (ScheduleEntryDto e : entries) {
            sb.append("• ").append(dayName(e.dayOfWeek())).append(", ").append(line(e, showAudience));
            if (e.parity() != WeekParity.ALL) {
                sb.append(e.parity() == WeekParity.ODD ? " – doar în săptămânile impare" : " – doar în săptămânile pare");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private static String weeklyCount(List<ScheduleEntryDto> week) {
        if (week.isEmpty()) {
            return "Nu ai ore în orarul publicat.";
        }
        Map<ActivityType, Integer> byType = new LinkedHashMap<>();
        long minutes = 0;
        int biweekly = 0;
        for (ScheduleEntryDto e : week) {
            byType.merge(e.type(), 1, Integer::sum);
            minutes += Duration.between(e.startTime(), e.endTime()).toMinutes();
            if (e.parity() != WeekParity.ALL) biweekly++;
        }
        List<String> parts = new ArrayList<>();
        byType.forEach((t, n) -> parts.add(n + " " + (n == 1 ? typeName(t) : typePlural(t))));
        return "Ai " + week.size() + " activități în orar (" + String.join(", ", parts) + "), adică "
                + (minutes / 60) + " ore." + (biweekly > 0
                ? " Dintre ele, " + (biweekly == 1 ? "una are" : biweekly + " au") + " loc doar o dată la două săptămâni, deci într-o săptămână concretă ai mai puține."
                : "");
    }

    // ------------------------------------------------------------------ calendar

    private record DayStatus(String reason, WeekParity parity, String weekLabel) {
    }

    private DayStatus status(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return new DayStatus("este weekend", null, null);
        }
        Optional<SemesterWeek> sw = semesterWeekRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
        if (sw.isEmpty()) {
            return new DayStatus("este în afara semestrului", null, null);
        }
        if (sw.get().isHoliday()) {
            return new DayStatus("este vacanță", null, null);
        }
        WeekParity parity = sw.get().getParity();
        return new DayStatus(null, parity,
                "săptămâna " + sw.get().getWeekNumber() + ", " + (parity == WeekParity.ODD ? "impară" : "pară"));
    }

    private static List<ScheduleEntryDto> forDate(List<ScheduleEntryDto> week, LocalDate date, WeekParity parity) {
        return week.stream()
                .filter(e -> e.dayOfWeek() == date.getDayOfWeek())
                .filter(e -> e.parity() == WeekParity.ALL || e.parity() == parity)
                .sorted(ScheduleEntryDto.CHRONOLOGICAL)
                .toList();
    }

    /** "Azi, miercuri 26 noiembrie" / "Mâine, ..." / "Vineri, 28 noiembrie" */
    private static String when(LocalDate date, LocalDate today) {
        String day = dayName(date.getDayOfWeek());
        String full = day + " " + date.format(DATE);
        if (date.equals(today)) return "Azi, " + full.toLowerCase(RO);
        if (date.equals(today.plusDays(1))) return "Mâine, " + full.toLowerCase(RO);
        return full;
    }

    /** Ex: "10:00–12:00 SCD laborator, sala G6, Asistent_Lab_SCD" */
    private static String line(ScheduleEntryDto e, boolean showAudience) {
        return e.startTime().toString().substring(0, 5) + "–" + e.endTime().toString().substring(0, 5) + " "
                + e.disciplineCode() + " " + typeName(e.type()) + ", sala " + e.roomCode()
                + (showAudience ? (e.targetName() != null ? ", cu " + e.targetName() : "") : ", " + e.professorName());
    }

    private static String typeName(ActivityType type) {
        return switch (type) {
            case COURSE -> "curs";
            case SEMINAR -> "seminar";
            case LAB -> "laborator";
            case PROJECT -> "proiect";
        };
    }

    private static String typePlural(ActivityType type) {
        return switch (type) {
            case COURSE -> "cursuri";
            case SEMINAR -> "seminare";
            case LAB -> "laboratoare";
            case PROJECT -> "proiecte";
        };
    }

    private static String dayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Luni";
            case TUESDAY -> "Marți";
            case WEDNESDAY -> "Miercuri";
            case THURSDAY -> "Joi";
            case FRIDAY -> "Vineri";
            case SATURDAY -> "Sâmbătă";
            case SUNDAY -> "Duminică";
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(RO) + s.substring(1);
    }

    /** litere mici, fara diacritice: "Când am laboratorul?" -> "cand am laboratorul?" */
    static String normalize(String s) {
        String noMarks = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noMarks.toLowerCase(Locale.ROOT).trim();
    }
}
