package ro.utcn.scd.courseplatform.websocket;

import ro.utcn.scd.courseplatform.dto.NotificationDto;

import java.util.List;

/** Eveniment intern: s-au creat notificari noi care trebuie trimise live. */
public record NotificationsCreatedEvent(List<NotificationDto> notifications) {
}
