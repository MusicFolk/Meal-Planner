package lt.vilniustech.meal_planner.dto;

import java.time.Instant;
import lt.vilniustech.meal_planner.entity.Notification;

public record NotificationDto(Long id, String message, Instant createdAt, boolean read) {

    public static NotificationDto of(Notification n) {
        return new NotificationDto(n.getId(), n.getMessage(), n.getCreatedAt(), n.isRead());
    }
}
