package lt.vilniustech.meal_planner.repository;

import lt.vilniustech.meal_planner.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishRepository extends JpaRepository<Dish, Long> {
}
