package ro.utcn.scd.mobile.data.model;

/** O cerere de mutare a unei ore, trimisa de profesor si aprobata / respinsa de admin. */
public class ChangeRequest {
    public Long id;
    public Long scheduleEntryId;
    public Long professorId;
    public Long proposedTimeSlotId;
    public String proposedTimeSlotLabel;
    public Long proposedRoomId;
    public String proposedRoomCode;
    public String reason;
    /** PENDING, APPROVED, REJECTED */
    public String status;
    public String adminComment;
    public String createdAt;
    public String disciplineCode;
    public String activityType;
    public String targetName;
    public String currentTimeSlotLabel;
    public String currentRoomCode;
}
