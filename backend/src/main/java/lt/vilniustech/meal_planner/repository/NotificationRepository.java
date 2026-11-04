package lt.vilniustech.meal_planner.repository;

import java.util.List;
import java.util.Optional;
import lt.vilniustech.meal_planner.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByClientIdOrderByCreatedAtDesc(Long clientId);

    Optional<Notification> findByIdAndClientId(Long id, Long clientId);

    @Modifying
    @Query("update Notification n set n.read = true where n.client.id = :clientId and n.read = false")
    void markAllRead(@Param("clientId") Long clientId);
}
