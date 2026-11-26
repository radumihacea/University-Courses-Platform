package ro.utcn.scd.mobile.data.model;

import java.util.List;

/** Orarul unei zile (GET /api/schedule/{students|professors}/{id}/day?date=...). */
public class DaySchedule {
    public String date;
    /** null in weekend sau in afara semestrului */
    public Integer weekNumber;
    public String parity;
    public boolean holiday;
    /** explicatie pentru zilele fara ore (weekend, vacanta, niciun orar publicat) */
    public String message;
    public List<ScheduleEntry> entries;
}
