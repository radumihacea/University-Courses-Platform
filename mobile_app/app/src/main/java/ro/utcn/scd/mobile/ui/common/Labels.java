package ro.utcn.scd.mobile.ui.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import ro.utcn.scd.mobile.R;

/** Texte si culori folosite in mai multe ecrane (in romana, ca in panoul web). */
public final class Labels {

    public static final Locale RO = new Locale("ro", "RO");

    private static final DateTimeFormatter DAY_TITLE = DateTimeFormatter.ofPattern("EEEE, d MMMM", RO);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", RO);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM, HH:mm", RO);

    private Labels() {
    }

    public static String activityType(String type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case "COURSE":
                return "Curs";
            case "SEMINAR":
                return "Seminar";
            case "LAB":
                return "Laborator";
            case "PROJECT":
                return "Proiect";
            default:
                return type;
        }
    }

    public static int activityColor(String type) {
        if (type == null) {
            return R.color.type_course;
        }
        switch (type) {
            case "SEMINAR":
                return R.color.type_seminar;
            case "LAB":
                return R.color.type_lab;
            case "PROJECT":
                return R.color.type_project;
            default:
                return R.color.type_course;
        }
    }

    /** null pentru orele din fiecare saptamana */
    public static String parity(String parity) {
        if ("ODD".equals(parity)) {
            return "Săpt. impare";
        }
        if ("EVEN".equals(parity)) {
            return "Săpt. pare";
        }
        return null;
    }

    public static String dayName(String dayOfWeek) {
        if (dayOfWeek == null) {
            return "";
        }
        switch (dayOfWeek) {
            case "MONDAY":
                return "Luni";
            case "TUESDAY":
                return "Marți";
            case "WEDNESDAY":
                return "Miercuri";
            case "THURSDAY":
                return "Joi";
            case "FRIDAY":
                return "Vineri";
            case "SATURDAY":
                return "Sâmbătă";
            default:
                return "Duminică";
        }
    }

    /** ex: "Joi, 26 noiembrie" */
    public static String dayTitle(LocalDate date) {
        return capitalize(date.format(DAY_TITLE));
    }

    /** ex: "acum 3 min", "azi, 14:03", "ieri, 09:10", "25 nov., 14:03" */
    public static String relativeTime(String isoDateTime) {
        try {
            LocalDateTime time = LocalDateTime.parse(isoDateTime);
            LocalDateTime now = LocalDateTime.now();
            long minutes = ChronoUnit.MINUTES.between(time, now);
            if (minutes < 1) {
                return "chiar acum";
            }
            if (minutes < 60) {
                return "acum " + minutes + " min";
            }
            if (time.toLocalDate().equals(now.toLocalDate())) {
                return "azi, " + time.format(TIME);
            }
            if (time.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
                return "ieri, " + time.format(TIME);
            }
            return time.format(SHORT_DATE);
        } catch (Exception e) {
            return isoDateTime == null ? "" : isoDateTime;
        }
    }

    /** ex: "Marți 10:00–12:00" */
    public static String slot(String dayOfWeek, String start, String end) {
        return dayName(dayOfWeek) + " " + hhmm(start) + "–" + hhmm(end);
    }

    /** "08:00:00" -> "08:00" */
    public static String hhmm(String time) {
        return time == null ? "" : time.substring(0, Math.min(5, time.length()));
    }

    /** Etichetele venite din backend sunt fara diacritice ("Marti 10:00-12:00"): le infrumusetam. */
    public static String pretty(String label) {
        if (label == null) {
            return "";
        }
        return label.replace("Marti", "Marți").replace("Sambata", "Sâmbătă").replace("Duminica", "Duminică")
                .replace("sapt.", "săpt.").replace("-", "–");
    }

    public static String requestStatus(String status) {
        if ("APPROVED".equals(status)) {
            return "Aprobată";
        }
        if ("REJECTED".equals(status)) {
            return "Respinsă";
        }
        return "În așteptare";
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("[\\s_]+");
        String first = parts[0].substring(0, 1);
        String second = parts.length > 1 && !parts[parts.length - 1].isEmpty() ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase(RO);
    }

    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? "" : s.substring(0, 1).toUpperCase(RO) + s.substring(1);
    }
}
