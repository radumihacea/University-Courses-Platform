package ro.utcn.scd.mobile.data.model;

import java.util.List;

/** Un slot in care se poate muta o ora (profesorul si studentii sunt liberi), cu salile libere. */
public class MoveOption {
    public Long timeSlotId;
    public String label;
    public String dayOfWeek;
    public String startTime;
    public String endTime;
    /** profesorul a marcat slotul ca preferat */
    public boolean preferred;
    public List<Room> rooms;
}
