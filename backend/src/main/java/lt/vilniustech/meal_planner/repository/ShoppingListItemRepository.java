package lt.vilniustech.meal_planner.repository;

import lt.vilniustech.meal_planner.entity.ShoppingListItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {
}
