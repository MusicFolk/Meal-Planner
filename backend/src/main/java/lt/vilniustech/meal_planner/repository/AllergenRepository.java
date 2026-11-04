package lt.vilniustech.meal_planner.repository;

import java.util.List;
import lt.vilniustech.meal_planner.entity.Allergen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergenRepository extends JpaRepository<Allergen, Long> {

    List<Allergen> findAllByOrderByIdAsc();
}
