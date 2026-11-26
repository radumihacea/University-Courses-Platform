package ro.utcn.scd.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Instant;

import ro.utcn.scd.mobile.data.model.AuthResponse;

/**
 * Sesiunea utilizatorului, salvata in SharedPreferences ca sa ramana autentificat
 * si dupa ce aplicatia este inchisa. Tine si adresa serverului (backend-ul Spring Boot).
 */
public final class Session {

    /** 10.0.2.2 = calculatorul pe care ruleaza emulatorul (localhost-ul laptopului). */
    public static final String DEFAULT_SERVER = "http://10.0.2.2:8080";

    private static final String PREFS = "ucp_session";
    private static Session instance;

    private final SharedPreferences prefs;

    private Session(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized Session get(Context context) {
        if (instance == null) {
            instance = new Session(context.getApplicationContext());
        }
        return instance;
    }

    public void save(AuthResponse auth) {
        long expires;
        try {
            expires = Instant.parse(auth.expiresAt).toEpochMilli();
        } catch (Exception e) {
            expires = System.currentTimeMillis() + 12L * 60 * 60 * 1000;
        }
        prefs.edit()
                .putString("token", auth.token)
                .putLong("expiresAt", expires)
                .putLong("userId", auth.userId != null ? auth.userId : -1)
                .putString("fullName", auth.fullName)
                .putString("email", auth.email)
                .putString("role", auth.role)
                .putLong("personId", "PROFESSOR".equals(auth.role)
                        ? (auth.professorId != null ? auth.professorId : -1)
                        : (auth.studentId != null ? auth.studentId : -1))
                .apply();
    }

    /** Sterge datele de autentificare (adresa serverului ramane). */
    public void clear() {
        String server = serverUrl();
        prefs.edit().clear().putString("serverUrl", server).apply();
    }

    public boolean isLoggedIn() {
        return token() != null && prefs.getLong("expiresAt", 0) > System.currentTimeMillis();
    }

    public String token() {
        return prefs.getString("token", null);
    }

    public long userId() {
        return prefs.getLong("userId", -1);
    }

    /** studentId pentru studenti, professorId pentru profesori */
    public long personId() {
        return prefs.getLong("personId", -1);
    }

    public String fullName() {
        return prefs.getString("fullName", "");
    }

    public String email() {
        return prefs.getString("email", "");
    }

    public String role() {
        return prefs.getString("role", "");
    }

    public boolean isProfessor() {
        return "PROFESSOR".equals(role());
    }

    public String serverUrl() {
        return prefs.getString("serverUrl", DEFAULT_SERVER);
    }

    public void setServerUrl(String url) {
        String clean = url.trim();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://" + clean;
        }
        prefs.edit().putString("serverUrl", clean).apply();
    }
}
