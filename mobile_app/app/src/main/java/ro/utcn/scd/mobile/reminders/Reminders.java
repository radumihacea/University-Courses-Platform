package ro.utcn.scd.mobile.reminders;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import ro.utcn.scd.mobile.R;
import ro.utcn.scd.mobile.data.ApiClient;
import ro.utcn.scd.mobile.data.Session;
import ro.utcn.scd.mobile.data.Settings;
import ro.utcn.scd.mobile.data.model.DaySchedule;
import ro.utcn.scd.mobile.data.model.ScheduleEntry;
import ro.utcn.scd.mobile.ui.common.Labels;
import ro.utcn.scd.mobile.ui.login.LoginActivity;

/**
 * Reminderele: o notificare pe telefon cu cateva minute inainte de fiecare ora,
 * chiar daca aplicatia este inchisa (folosind AlarmManager-ul sistemului).
 *
 * Se programeaza orele din urmatoarele 7 zile, luate din backend zi cu zi, ca sa se tina cont
 * de saptamanile pare / impare si de vacante. Se reprogrameaza la fiecare deschidere a aplicatiei
 * si cand vine o notificare (orarul se poate fi schimbat).
 */
public final class Reminders {

    static final String EXTRA_ID = "id";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TEXT = "text";
    private static final String CHANNEL = "reminders";
    private static final int DAYS_AHEAD = 7;

    private Reminders() {
    }

    /** Reprogrameaza toate reminderele (sau le anuleaza, daca sunt dezactivate). */
    public static void reschedule(Context context) {
        Context app = context.getApplicationContext();
        cancelAll(app);
        Settings settings = Settings.get(app);
        if (!settings.remindersEnabled() || !Session.get(app).isLoggedIn()) {
            return;
        }
        int minutesBefore = settings.reminderMinutes();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < DAYS_AHEAD; i++) {
            LocalDate date = today.plusDays(i);
            int dayIndex = i;
            ApiClient.get(app).day(date, new ApiClient.Callback<DaySchedule>() {
                @Override
                public void onSuccess(DaySchedule day) {
                    if (day.entries == null) {
                        return;
                    }
                    Set<String> codes = settings.scheduledAlarms();
                    for (ScheduleEntry e : day.entries) {
                        LocalDateTime start = date.atTime(LocalTime.parse(e.start()));
                        LocalDateTime at = start.minusMinutes(minutesBefore);
                        if (at.isBefore(LocalDateTime.now())) {
                            continue;
                        }
                        int code = (int) (e.id * 10 + dayIndex);
                        String title = e.disciplineCode + " " + Labels.activityType(e.type).toLowerCase(Labels.RO)
                                + " în " + minutesBefore + " min";
                        String text = e.start() + "–" + e.end() + ", sala " + e.roomCode + " · "
                                + (Session.get(app).isProfessor() ? e.targetName : e.professorName);
                        schedule(app, code, at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), title, text);
                        codes.add(String.valueOf(code));
                    }
                    settings.setScheduledAlarms(codes);
                }

                @Override
                public void onError(String message) {
                    // fara conexiune: se reincearca la urmatoarea deschidere a aplicatiei
                }
            });
        }
    }

    public static void cancelAll(Context context) {
        Settings settings = Settings.get(context);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (String code : settings.scheduledAlarms()) {
            PendingIntent pi = PendingIntent.getBroadcast(context, Integer.parseInt(code),
                    new Intent(context, ReminderReceiver.class),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                alarms.cancel(pi);
                pi.cancel();
            }
        }
        settings.setScheduledAlarms(new HashSet<>());
    }

    /** Pentru demonstratie: un reminder de proba dupa cateva secunde. */
    public static void sendTest(Context context) {
        Context app = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(() -> notify(app, 999_999,
                "SCD laborator în " + Settings.get(app).reminderMinutes() + " min",
                "Acesta este un reminder de probă. Așa vei fi anunțat înainte de fiecare oră."), 5_000);
    }

    private static void schedule(Context context, int code, long triggerAtMillis, String title, String text) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .putExtra(EXTRA_ID, code)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_TEXT, text);
        PendingIntent pi = PendingIntent.getBroadcast(context, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Alarmele exacte cer o permisiune speciala pe Android 12+; fara ea folosim una aproximativa.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }

    /** Afiseaza notificarea in bara de sus a telefonului. */
    static void notify(Context context, int id, String title, String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return; // utilizatorul nu a permis notificarile
        }
        ensureChannel(context);
        PendingIntent open = PendingIntent.getActivity(context, 0,
                new Intent(context, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setColor(ContextCompat.getColor(context, R.color.utcn_red))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(id, builder.build());
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Remindere ore",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notificare înainte de începerea fiecărei ore");
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
