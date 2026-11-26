package ro.utcn.scd.mobile.data.model;

/** O notificare (din lista sau primita live prin WebSocket). */
public class AppNotification {
    public Long id;
    public Long userId;
    public String message;
    public Boolean read;
    /** ex: "2026-11-26T14:03:12.123" */
    public String createdAt;

    public boolean isRead() {
        return Boolean.TRUE.equals(read);
    }
}
