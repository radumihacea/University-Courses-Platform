package ro.utcn.scd.mobile.data.model;

/** Un mesaj din conversatia cu asistentul AI. role = "user" sau "assistant". */
public class ChatMessage {
    public String role;
    public String content;

    /** Doar pentru afisare (nu se trimit la server, Gson le ignora fiind transient). */
    public transient boolean streaming;
    public transient boolean error;
    public transient String meta;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public boolean isUser() {
        return "user".equals(role);
    }
}
