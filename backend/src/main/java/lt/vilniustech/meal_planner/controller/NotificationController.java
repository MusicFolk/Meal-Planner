package lt.vilniustech.meal_planner.controller;

import java.util.List;
import lt.vilniustech.meal_planner.dto.NotificationDto;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public List<NotificationDto> list(@AuthenticationPrincipal AppUserDetails user) {
        return notifications.list(user.getId());
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        notifications.markRead(user.getId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AppUserDetails user) {
        notifications.markAllRead(user.getId());
    }
}
