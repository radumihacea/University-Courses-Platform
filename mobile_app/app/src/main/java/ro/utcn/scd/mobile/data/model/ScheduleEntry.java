package ro.utcn.scd.mobile.data.model;

/** O ora din orar, asa cum o trimite backend-ul (ScheduleEntryDto). */
public class ScheduleEntry {
    public Long id;
    public String disciplineCode;
    public String disciplineName;
    /** COURSE, SEMINAR, LAB, PROJECT */
    public String type;
    public Long professorId;
    public String professorName;
    public Long roomId;
    public String roomCode;
    public Long timeSlotId;
    /** MONDAY ... FRIDAY */
    public String dayOfWeek;
    public String dayName;
    /** "08:00" sau "08:00:00" */
    public String startTime;
    public String endTime;
    /** ALL, ODD (saptamani impare), EVEN (saptamani pare) */
    public String parity;
    /** cui i se preda: seria, grupa sau semigrupa */
    public String targetName;

    public String start() {
        return startTime == null ? "" : startTime.substring(0, Math.min(5, startTime.length()));
    }

    public String end() {
        return endTime == null ? "" : endTime.substring(0, Math.min(5, endTime.length()));
    }
}
