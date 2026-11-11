package ro.utcn.scd.courseplatform.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ro.utcn.scd.courseplatform.dto.NotificationDto;
import ro.utcn.scd.courseplatform.repository.UserRepository;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MECANISMUL ACTIV AL SERVERULUI: notificari trimise in timp real prin WebSocket.
 *
 * Clientul (aplicatia web sau mobila) se conecteaza la
 *     ws://localhost:8080/ws/notifications?userId=25
 * si ramane conectat. Serverul tine evidenta conexiunilor deschise pentru fiecare
 * utilizator si ii trimite notificarile in momentul in care sunt create,
 * fara ca clientul sa mai intrebe ("push", nu "pull").
 *
 * Un utilizator poate avea mai multe conexiuni (ex: web + telefon); le primeste pe toate.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_LIMIT_BYTES = 64 * 1024;

    private final UserRepository userRepository;

    /** userId -> conexiunile deschise ale utilizatorului. */
    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Long userId = userIdFrom(session.getUri());
        if (userId == null || !userRepository.existsById(userId)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Parametrul userId lipseste sau este invalid"));
            return;
        }
        // Decoratorul permite trimiterea sigura din mai multe fire de executie simultan
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES);
        session.getAttributes().put("userId", userId);
        sessionsByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(safe);
        log.info("WebSocket: utilizatorul {} s-a conectat ({} conexiuni active).", userId, onlineConnections());

        safe.sendMessage(new TextMessage(
                "{\"type\":\"CONNECTED\",\"userId\":" + userId + ",\"message\":\"Conectat la notificari live.\"}"));
    }

    /** Clientul poate trimite "ping" ca sa verifice conexiunea; serverul raspunde "pong". */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if ("ping".equalsIgnoreCase(message.getPayload().trim())) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof Long id) {
            Set<WebSocketSession> sessions = sessionsByUser.get(id);
            if (sessions != null) {
                sessions.removeIf(s -> s.getId().equals(session.getId()));
                if (sessions.isEmpty()) {
                    sessionsByUser.remove(id);
                }
            }
            log.info("WebSocket: utilizatorul {} s-a deconectat.", id);
        }
    }

    /** Trimite notificarea pe toate conexiunile deschise ale destinatarului (daca e online). */
    public void send(NotificationDto notification) {
        Set<WebSocketSession> sessions = sessionsByUser.get(notification.userId());
        if (sessions == null) {
            return; // utilizatorul nu e conectat acum; o va vedea in lista de notificari
        }
        TextMessage message = new TextMessage(toJson(notification));
        for (WebSocketSession s : sessions) {
            try {
                if (s.isOpen()) {
                    s.sendMessage(message);
                }
            } catch (IOException | IllegalStateException e) {
                log.warn("WebSocket: nu s-a putut trimite catre utilizatorul {}: {}", notification.userId(), e.getMessage());
            }
        }
    }

    public int onlineUsers() {
        return sessionsByUser.size();
    }

    public int onlineConnections() {
        return sessionsByUser.values().stream().mapToInt(Set::size).sum();
    }

    // ------------------------------------------------------------------

    private static Long userIdFrom(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        for (String param : uri.getQuery().split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals("userId")) {
                try {
                    return Long.parseLong(kv[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Mesajul trimis clientului, in format JSON. */
    private static String toJson(NotificationDto n) {
        return "{\"type\":\"NOTIFICATION\""
                + ",\"id\":" + n.id()
                + ",\"userId\":" + n.userId()
                + ",\"message\":\"" + escape(n.message()) + "\""
                + ",\"read\":" + n.read()
                + ",\"createdAt\":\"" + n.createdAt() + "\"}";
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
