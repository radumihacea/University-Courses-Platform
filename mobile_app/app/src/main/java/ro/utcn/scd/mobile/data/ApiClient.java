package ro.utcn.scd.mobile.data;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import ro.utcn.scd.mobile.data.model.AppNotification;
import ro.utcn.scd.mobile.data.model.AuthResponse;
import ro.utcn.scd.mobile.data.model.ChangeRequest;
import ro.utcn.scd.mobile.data.model.ChatMessage;
import ro.utcn.scd.mobile.data.model.DaySchedule;
import ro.utcn.scd.mobile.data.model.MoveOption;
import ro.utcn.scd.mobile.data.model.OptionalCourse;
import ro.utcn.scd.mobile.data.model.ProfessorConstraint;
import ro.utcn.scd.mobile.data.model.ScheduleEntry;
import ro.utcn.scd.mobile.data.model.TimeSlot;
import ro.utcn.scd.mobile.ui.login.LoginActivity;

/**
 * Toate apelurile catre backend-ul Spring Boot (REST).
 *
 * Cererile se fac pe un fir de executie separat (OkHttp), iar rezultatul este livrat
 * pe firul principal (UI), ca sa poata fi afisat direct.
 * Fiecare cerere trimite antetul "Authorization: Bearer <token>" primit la login.
 */
public final class ApiClient {

    /** Rezultatul unei cereri: fie onSuccess, fie onError cu un mesaj gata de afisat. */
    public interface Callback<T> {
        void onSuccess(T result);

        void onError(String message);
    }

    /** Raspunsul asistentului AI, primit bucata cu bucata. */
    public interface StreamCallback {
        void onToken(String text);

        void onDone(String model, long durationMs, Long firstTokenMs);

        void onError(String message);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static ApiClient instance;

    private final Context app;
    private final Session session;
    private final OkHttpClient http;
    /** modelul AI ruleaza pe procesor si poate raspunde in zeci de secunde */
    private final OkHttpClient slowHttp;
    private final Gson gson = new Gson();
    private final Handler main = new Handler(Looper.getMainLooper());

    private ApiClient(Context context) {
        app = context.getApplicationContext();
        session = Session.get(app);
        http = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        slowHttp = http.newBuilder().readTimeout(180, TimeUnit.SECONDS).build();
    }

    public static synchronized ApiClient get(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }

    OkHttpClient http() {
        return http;
    }

    // ------------------------------------------------------------------ autentificare

    public void login(String email, String password, Callback<AuthResponse> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        Request request = new Request.Builder()
                .url(session.serverUrl() + "/api/auth/login")
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();
        execute(request, AuthResponse.class, callback, false);
    }

    // ------------------------------------------------------------------ orar

    /** Orarul unei zile pentru utilizatorul autentificat (student sau profesor). */
    public void day(LocalDate date, Callback<DaySchedule> callback) {
        execute(authed(schedulePath() + "/day?date=" + date).get().build(), DaySchedule.class, callback, true);
    }

    /** Orarul saptamanal (toate orele, inclusiv cele din saptamanile pare / impare). */
    public void week(Callback<List<ScheduleEntry>> callback) {
        Type type = new TypeToken<List<ScheduleEntry>>() {
        }.getType();
        execute(authed(schedulePath() + "/week").get().build(), type, callback, true);
    }

    private String schedulePath() {
        return "/api/schedule/" + (session.isProfessor() ? "professors/" : "students/") + session.personId();
    }

    // ------------------------------------------------------------------ notificari

    public void notifications(Callback<List<AppNotification>> callback) {
        Type type = new TypeToken<List<AppNotification>>() {
        }.getType();
        execute(authed("/api/notifications?userId=" + session.userId()).get().build(), type, callback, true);
    }

    public void markRead(long id, Callback<AppNotification> callback) {
        Request request = authed("/api/notifications/" + id + "/read")
                .patch(RequestBody.create(new byte[0], (MediaType) null))
                .build();
        execute(request, AppNotification.class, callback, true);
    }

    // ------------------------------------------------------------------ profesor: cereri de mutare

    public void timeSlots(Callback<List<TimeSlot>> callback) {
        get("/api/time-slots", new TypeToken<List<TimeSlot>>() {
        }.getType(), callback);
    }

    /** Sloturile si salile in care se poate muta o ora fara conflicte. */
    public void moveOptions(long scheduleEntryId, Callback<List<MoveOption>> callback) {
        get("/api/change-requests/move-options?scheduleEntryId=" + scheduleEntryId,
                new TypeToken<List<MoveOption>>() {
                }.getType(), callback);
    }

    public void createChangeRequest(long scheduleEntryId, long timeSlotId, Long roomId, String reason,
                                    Callback<ChangeRequest> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("scheduleEntryId", scheduleEntryId);
        body.addProperty("professorId", session.personId());
        body.addProperty("proposedTimeSlotId", timeSlotId);
        if (roomId != null) {
            body.addProperty("proposedRoomId", roomId);
        }
        body.addProperty("reason", reason);
        send("POST", "/api/change-requests", body, ChangeRequest.class, callback);
    }

    public void myChangeRequests(Callback<List<ChangeRequest>> callback) {
        get("/api/change-requests?professorId=" + session.personId(), new TypeToken<List<ChangeRequest>>() {
        }.getType(), callback);
    }

    public void cancelChangeRequest(long id, Callback<Void> callback) {
        send("DELETE", "/api/change-requests/" + id, null, Void.class, callback);
    }

    // ------------------------------------------------------------------ profesor: disponibilitate

    public void constraints(Callback<List<ProfessorConstraint>> callback) {
        get("/api/professor-constraints?professorId=" + session.personId(),
                new TypeToken<List<ProfessorConstraint>>() {
                }.getType(), callback);
    }

    /** type = UNAVAILABLE sau PREFERRED */
    public void addConstraint(long timeSlotId, String type, boolean mandatory, String note,
                              Callback<ProfessorConstraint> callback) {
        JsonObject body = new JsonObject();
        body.addProperty("professorId", session.personId());
        body.addProperty("timeSlotId", timeSlotId);
        body.addProperty("type", type);
        body.addProperty("mandatory", mandatory);
        if (note != null && !note.isEmpty()) {
            body.addProperty("originalText", note);
        }
        send("POST", "/api/professor-constraints", body, ProfessorConstraint.class, callback);
    }

    public void deleteConstraint(long id, Callback<Void> callback) {
        send("DELETE", "/api/professor-constraints/" + id, null, Void.class, callback);
    }

    // ------------------------------------------------------------------ student: optionale

    private static final Type OPTIONALS = new TypeToken<List<OptionalCourse>>() {
    }.getType();

    public void optionals(Callback<List<OptionalCourse>> callback) {
        get("/api/students/" + session.personId() + "/optionals", OPTIONALS, callback);
    }

    public void setEnrolled(long assignmentId, boolean enrolled, Callback<List<OptionalCourse>> callback) {
        send(enrolled ? "POST" : "DELETE", "/api/students/" + session.personId() + "/optionals/" + assignmentId,
                null, OPTIONALS, callback);
    }

    // ------------------------------------------------------------------ asistent AI

    /**
     * Intreaba asistentul AI. Raspunsul vine in format NDJSON (cate un obiect JSON pe rand):
     * {"type":"token","text":"..."} pentru fiecare bucata de text, apoi {"type":"done",...}.
     * Intoarce apelul, ca sa poata fi oprit cu call.cancel().
     */
    public Call chatStream(String message, List<ChatMessage> history, StreamCallback callback) {
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        body.add("history", gson.toJsonTree(history));
        Request request = authed("/api/ai/chat/stream")
                .post(RequestBody.create(gson.toJson(body), JSON))
                .build();

        Call call = slowHttp.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call c, @NonNull IOException e) {
                if (!c.isCanceled()) {
                    main.post(() -> callback.onError(connectionError()));
                }
            }

            @Override
            public void onResponse(@NonNull Call c, @NonNull Response response) {
                try (Response r = response) {
                    ResponseBody responseBody = r.body();
                    if (!r.isSuccessful() || responseBody == null) {
                        String text = responseBody != null ? responseBody.string() : "";
                        if (r.code() == 401) {
                            handleUnauthorized();
                        }
                        String error = errorMessage(text, r.code());
                        main.post(() -> callback.onError(error));
                        return;
                    }
                    BufferedSource source = responseBody.source();
                    String line;
                    while ((line = source.readUtf8Line()) != null) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }
                        JsonObject event = JsonParser.parseString(line).getAsJsonObject();
                        String type = event.get("type").getAsString();
                        if ("token".equals(type)) {
                            String token = event.get("text").getAsString();
                            main.post(() -> callback.onToken(token));
                        } else if ("done".equals(type)) {
                            String model = event.get("model").getAsString();
                            long duration = event.get("durationMs").getAsLong();
                            JsonElement first = event.get("firstTokenMs");
                            Long firstToken = first == null || first.isJsonNull() ? null : first.getAsLong();
                            main.post(() -> callback.onDone(model, duration, firstToken));
                            return;
                        } else if ("error".equals(type)) {
                            String error = event.get("message").getAsString();
                            main.post(() -> callback.onError(error));
                            return;
                        }
                    }
                    main.post(() -> callback.onError("Răspunsul s-a întrerupt înainte de final."));
                } catch (Exception e) {
                    if (!c.isCanceled()) {
                        main.post(() -> callback.onError("Conexiunea cu serverul s-a întrerupt."));
                    }
                }
            }
        });
        return call;
    }

    // ------------------------------------------------------------------ comune

    private Request.Builder authed(String path) {
        Request.Builder builder = new Request.Builder().url(session.serverUrl() + path);
        String token = session.token();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private <T> void get(String path, Type type, Callback<T> callback) {
        execute(authed(path).get().build(), type, callback, true);
    }

    /** POST / PUT / PATCH / DELETE cu un corp JSON optional. */
    private <T> void send(String method, String path, JsonObject body, Type type, Callback<T> callback) {
        RequestBody requestBody = body != null
                ? RequestBody.create(gson.toJson(body), JSON)
                : ("DELETE".equals(method) ? null : RequestBody.create(new byte[0], (MediaType) null));
        execute(authed(path).method(method, requestBody).build(), type, callback, true);
    }

    private <T> void execute(Request request, Type type, Callback<T> callback, boolean authenticated) {
        http.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                main.post(() -> callback.onError(connectionError()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (Response r = response) {
                    ResponseBody body = r.body();
                    String text = body != null ? body.string() : "";
                    if (r.isSuccessful()) {
                        T value = gson.fromJson(text, type);
                        main.post(() -> callback.onSuccess(value));
                    } else {
                        if (r.code() == 401 && authenticated) {
                            handleUnauthorized();
                        }
                        String error = errorMessage(text, r.code());
                        main.post(() -> callback.onError(error));
                    }
                } catch (Exception e) {
                    main.post(() -> callback.onError("Răspuns neașteptat de la server."));
                }
            }
        });
    }

    private String connectionError() {
        return "Serverul nu răspunde. Verifică dacă backend-ul rulează la " + session.serverUrl() + ".";
    }

    /** Backend-ul trimite erorile ca {"status":..., "message":"...", "details":[...]}. */
    private String errorMessage(String body, int code) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("message") && !json.get("message").isJsonNull()) {
                return json.get("message").getAsString();
            }
        } catch (Exception ignored) {
            // corpul nu este JSON
        }
        return "Eroare " + code + ".";
    }

    /** Token expirat sau invalid: stergem sesiunea si ne intoarcem la ecranul de login. */
    private void handleUnauthorized() {
        session.clear();
        main.post(() -> {
            LiveNotifications.get(app).stop();
            Intent intent = new Intent(app, LoginActivity.class)
                    .putExtra(LoginActivity.EXTRA_EXPIRED, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            app.startActivity(intent);
        });
    }
}
