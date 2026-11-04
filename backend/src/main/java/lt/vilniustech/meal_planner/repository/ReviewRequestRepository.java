package lt.vilniustech.meal_planner.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lt.vilniustech.meal_planner.entity.ReviewRequest;
import lt.vilniustech.meal_planner.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, Long> {

    List<ReviewRequest> findByClientIdOrderBySubmittedAtDesc(Long clientId);

    Optional<ReviewRequest> findFirstByMealPlanIdOrderBySubmittedAtDesc(Long mealPlanId);

    boolean existsByMealPlanIdAndStatusIn(Long mealPlanId, Collection<ReviewStatus> statuses);

    /** „Laukia“ užklausos matomos visiems dietologams, kitos – tik priskirtam (FR-34). */
    @Query("""
            select r from ReviewRequest r
            where (r.status = lt.vilniustech.meal_planner.enums.ReviewStatus.PENDING or r.dietitian.id = :dietitianId)
              and (:status is null or r.status = :status)
            order by r.submittedAt asc
            """)
    List<ReviewRequest> findVisibleTo(@Param("dietitianId") Long dietitianId, @Param("status") ReviewStatus status);
}
