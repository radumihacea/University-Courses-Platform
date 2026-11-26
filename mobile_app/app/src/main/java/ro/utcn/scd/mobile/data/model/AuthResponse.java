package ro.utcn.scd.mobile.data.model;

/** Raspunsul la login (POST /api/auth/login). */
public class AuthResponse {
    public String token;
    /** momentul expirarii tokenului, ex: "2026-11-26T12:00:00Z" */
    public String expiresAt;
    public Long userId;
    public String fullName;
    public String email;
    /** STUDENT, PROFESSOR sau ADMIN */
    public String role;
    public Long professorId;
    public Long studentId;
}
