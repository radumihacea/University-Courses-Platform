package ro.utcn.scd.mobile.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import ro.utcn.scd.mobile.data.model.AppNotification;

/**
 * Mecanismul activ: conexiunea WebSocket cu backend-ul (ws://.../ws/notifications?userId=...).
 * Serverul trimite notificarile in momentul in care apar (ex. orar publicat, cerere aprobata),
 * fara ca aplicatia sa intrebe periodic. Daca legatura cade, se reconecteaza automat.
 */
public final class LiveNotifications {

    public interface Listener {
        void onNotification(AppNotification notification);

        default void onConnectionChanged(boolean connected) {
        }
    }

    private static final long RECONNECT_DELAY_MS = 5_000;
    private static LiveNotifications instance;

    private final Session session;
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private WebSocket socket;
    private boolean started;
    private boolean connected;

    private final Runnable reconnect = this::connect;

    private LiveNotifications(Context context) {
        session = Session.get(context);
        client = ApiClient.get(context).http().newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(25, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized LiveNotifications get(Context context) {
        if (instance == null) {
            instance = new LiveNotifications(context.getApplicationContext());
        }
        return instance;
    }

    public void addListener(Listener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return connected;
    }

    /** Porneste conexiunea (apelat cand ecranul principal devine vizibil). */
    public void start() {
        if (started || !session.isLoggedIn()) {
            return;
        }
        started = true;
        connect();
    }

    /** Inchide conexiunea (aplicatia in fundal sau deconectare). */
    public void stop() {
        started = false;
        main.removeCallbacks(reconnect);
        if (socket != null) {
            socket.close(1000, "Aplicatia a fost inchisa");
            socket = null;
        }
        setConnected(false);
    }

    private void connect() {
        if (!started) {
            return;
        }
        // http://10.0.2.2:8080 -> ws://10.0.2.2:8080
        String url = session.serverUrl().replaceFirst("^http", "ws")
                + "/ws/notifications?userId=" + session.userId();
        socket = client.newWebSocket(new Request.Builder().url(url).build(), new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                main.post(() -> {
                    if (webSocket == socket) {
                        setConnected(true);
                    }
                });
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                try {
                    JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                    if (json.has("type") && "NOTIFICATION".equals(json.get("type").getAsString())) {
                        AppNotification notification = gson.fromJson(json, AppNotification.class);
                        main.post(() -> {
                            for (Listener l : listeners) {
                                l.onNotification(notification);
                            }
                        });
                    }
                } catch (Exception ignored) {
                    // mesaj necunoscut - il ignoram
                }
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                main.post(() -> onDisconnected(webSocket));
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                main.post(() -> onDisconnected(webSocket));
            }
        });
    }

    private void onDisconnected(WebSocket webSocket) {
        if (webSocket != socket) {
            return; // o conexiune veche, deja inlocuita
        }
        setConnected(false);
        if (started) {
            main.removeCallbacks(reconnect);
            main.postDelayed(reconnect, RECONNECT_DELAY_MS);
        }
    }

    private void setConnected(boolean value) {
        if (connected == value) {
            return;
        }
        connected = value;
        for (Listener l : listeners) {
            l.onConnectionChanged(value);
        }
    }
}
