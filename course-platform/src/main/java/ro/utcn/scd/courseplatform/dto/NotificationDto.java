package ro.utcn.scd.courseplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ro.utcn.scd.courseplatform.model.Notification;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        @NotNull Long userId,
        @NotBlank @Size(max = 500) String message,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getUser().getId(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
