package lt.vilniustech.meal_planner.service;

import java.time.Clock;
import java.util.List;
import lt.vilniustech.meal_planner.dto.NotificationDto;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Notification;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    public void notify(Client client, String message) {
        notifications.save(new Notification(client, message, clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(Long clientId) {
        return notifications.findByClientIdOrderByCreatedAtDesc(clientId).stream().map(NotificationDto::of).toList();
    }

    public void markRead(Long clientId, Long id) {
        notifications.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> ApiException.notFound("Pranešimas nerastas."))
                .setRead(true);
    }

    public void markAllRead(Long clientId) {
        notifications.markAllRead(clientId);
    }
}
