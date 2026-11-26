package ro.utcn.scd.mobile.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Preferintele utilizatorului: tema si reminderele. Raman salvate si dupa deconectare. */
public final class Settings {

    public static final int THEME_DARK = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_SYSTEM = 2;
    public static final String[] THEME_NAMES = {"Întunecată", "Luminoasă", "Ca sistemul"};

    private static Settings instance;
    private final SharedPreferences prefs;

    private Settings(Context context) {
        prefs = context.getSharedPreferences("ucp_settings", Context.MODE_PRIVATE);
    }

    public static synchronized Settings get(Context context) {
        if (instance == null) {
            instance = new Settings(context.getApplicationContext());
        }
        return instance;
    }

    public int theme() {
        return prefs.getInt("theme", THEME_DARK);
    }

    public void setTheme(int theme) {
        prefs.edit().putInt("theme", theme).apply();
        applyTheme();
    }

    public void applyTheme() {
        int mode;
        switch (theme()) {
            case THEME_LIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case THEME_SYSTEM:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
            default:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public boolean remindersEnabled() {
        return prefs.getBoolean("reminders", false);
    }

    public void setRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean("reminders", enabled).apply();
    }

    /** Cu cate minute inainte de ora vine reminderul. */
    public int reminderMinutes() {
        return prefs.getInt("reminderMinutes", 15);
    }

    public void setReminderMinutes(int minutes) {
        prefs.edit().putInt("reminderMinutes", minutes).apply();
    }

    /** Codurile alarmelor programate (ca sa le putem anula la reprogramare). */
    public java.util.Set<String> scheduledAlarms() {
        return new java.util.HashSet<>(prefs.getStringSet("alarms", new java.util.HashSet<>()));
    }

    public void setScheduledAlarms(java.util.Set<String> codes) {
        prefs.edit().putStringSet("alarms", codes).apply();
    }
}
