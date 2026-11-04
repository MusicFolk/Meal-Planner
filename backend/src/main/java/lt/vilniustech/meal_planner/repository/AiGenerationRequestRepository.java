package lt.vilniustech.meal_planner.repository;

import java.util.List;
import lt.vilniustech.meal_planner.entity.AiGenerationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiGenerationRequestRepository extends JpaRepository<AiGenerationRequest, Long> {

    List<AiGenerationRequest> findByPlanGenerationIdOrderByIdAsc(Long planGenerationId);
}
