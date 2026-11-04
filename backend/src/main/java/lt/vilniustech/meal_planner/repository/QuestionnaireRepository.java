package lt.vilniustech.meal_planner.repository;

import java.util.Optional;
import lt.vilniustech.meal_planner.entity.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {

    Optional<Questionnaire> findByClientId(Long clientId);
}
