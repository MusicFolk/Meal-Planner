package lt.vilniustech.meal_planner.repository;

import java.util.Optional;
import lt.vilniustech.meal_planner.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByClientId(Long clientId);
}
