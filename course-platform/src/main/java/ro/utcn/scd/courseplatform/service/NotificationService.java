package ro.utcn.scd.courseplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.scd.courseplatform.dto.NotificationDto;
import ro.utcn.scd.courseplatform.model.Notification;
import ro.utcn.scd.courseplatform.model.User;
import ro.utcn.scd.courseplatform.repository.NotificationRepository;
import ro.utcn.scd.courseplatform.websocket.NotificationsCreatedEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * CRUD pe notificari. Fiecare notificare noua se salveaza in baza de date
 * si apoi (dupa commit) se trimite live prin WebSocket catre destinatar.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository repository;
    private final EntityLookup lookup;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<NotificationDto> findAll(Long userId, boolean unreadOnly) {
        List<Notification> list;
        if (userId == null) {
            list = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else if (unreadOnly) {
            list = repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        } else {
            list = repository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return list.stream().map(NotificationDto::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificationDto findById(Long id) {
        return NotificationDto.from(lookup.notification(id));
    }

    public NotificationDto create(NotificationDto dto) {
        Notification n = new Notification();
        n.setUser(lookup.user(dto.userId()));
        n.setMessage(dto.message());
        n.setRead(false);
        NotificationDto saved = NotificationDto.from(repository.saveAndFlush(n));
        eventPublisher.publishEvent(new NotificationsCreatedEvent(List.of(saved)));
        return saved;
    }

    public NotificationDto update(Long id, NotificationDto dto) {
        Notification n = lookup.notification(id);
        n.setUser(lookup.user(dto.userId()));
        n.setMessage(dto.message());
        n.setRead(Boolean.TRUE.equals(dto.read()));
        return NotificationDto.from(repository.saveAndFlush(n));
    }

    public NotificationDto markAsRead(Long id) {
        Notification n = lookup.notification(id);
        n.setRead(true);
        return NotificationDto.from(repository.saveAndFlush(n));
    }

    public void delete(Long id) {
        repository.delete(lookup.notification(id));
        repository.flush();
    }

    /** Trimite acelasi mesaj mai multor utilizatori (ex: la publicarea orarului). */
    public void notifyUsers(Collection<User> users, String message) {
        List<Notification> notifications = new ArrayList<>();
        for (User user : users) {
            Notification n = new Notification();
            n.setUser(user);
            n.setMessage(message);
            n.setRead(false);
            notifications.add(n);
        }
        List<NotificationDto> saved = repository.saveAll(notifications).stream()
                .map(NotificationDto::from)
                .toList();
        eventPublisher.publishEvent(new NotificationsCreatedEvent(saved));
    }
}
