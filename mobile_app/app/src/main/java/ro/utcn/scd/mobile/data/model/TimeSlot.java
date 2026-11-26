package ro.utcn.scd.mobile.data.model;

/** Un interval din grila orarului (ex: Luni 08:00-10:00). */
public class TimeSlot {
    public Long id;
    public String dayOfWeek;
    public String startTime;
    public String endTime;
    public String label;
}
