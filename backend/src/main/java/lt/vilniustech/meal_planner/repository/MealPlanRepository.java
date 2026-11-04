package lt.vilniustech.meal_planner.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lt.vilniustech.meal_planner.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {

    Optional<MealPlan> findByClientIdAndActiveTrue(Long clientId);

    List<MealPlan> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /** Demonstracinis plano šablonas (1 sprintas). */
    Optional<MealPlan> findFirstByClientIsNullOrderByIdAsc();

    long countByClientIdAndCreatedAtGreaterThanEqual(Long clientId, Instant from);

    /** Ankstesnis aktualus planas pažymimas neaktualiu, bet neištrinamas (FR-15). */
    @Modifying(flushAutomatically = true)
    @Query("update MealPlan p set p.active = false where p.client.id = :clientId and p.active = true")
    void deactivateAll(@Param("clientId") Long clientId);
}
