package lt.vilniustech.meal_planner.repository;

import java.time.Instant;
import java.util.Optional;
import lt.vilniustech.meal_planner.entity.PlanGeneration;
import lt.vilniustech.meal_planner.enums.GenerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanGenerationRepository extends JpaRepository<PlanGeneration, Long> {

    /** Šios dienos sėkmingi generavimai – dienos limitui (FR-15). */
    long countByClientIdAndStatusAndStartedAtGreaterThanEqual(Long clientId, GenerationStatus status, Instant from);

    Optional<PlanGeneration> findFirstByClientIdAndStatus(Long clientId, GenerationStatus status);

    Optional<PlanGeneration> findFirstByClientIdOrderByStartedAtDesc(Long clientId);
}
