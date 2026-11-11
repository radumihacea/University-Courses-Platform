package ro.utcn.scd.courseplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ro.utcn.scd.courseplatform.websocket.NotificationWebSocketHandler;

/** Inregistreaza endpoint-ul WebSocket pentru notificari: ws://localhost:8080/ws/notifications?userId=... */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // "*" pentru dezvoltare: permite conexiuni din Vue (alt port) si din aplicatia Android
        registry.addHandler(notificationHandler, "/ws/notifications")
                .setAllowedOriginPatterns("*");
    }
}
