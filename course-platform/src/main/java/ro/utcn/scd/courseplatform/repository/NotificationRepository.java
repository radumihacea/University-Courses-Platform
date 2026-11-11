package ro.utcn.scd.courseplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.scd.courseplatform.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
