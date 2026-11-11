package ro.utcn.scd.courseplatform.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Trimite notificarile prin WebSocket DOAR dupa ce tranzactia a fost confirmata (commit).
 * Astfel clientul nu primeste niciodata o notificare pentru o operatie care a esuat
 * si a fost anulata (rollback) - datele raman consistente.
 */
@Component
@RequiredArgsConstructor
public class NotificationPushListener {

    private final NotificationWebSocketHandler webSocketHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationsCreated(NotificationsCreatedEvent event) {
        event.notifications().forEach(webSocketHandler::send);
    }
}
