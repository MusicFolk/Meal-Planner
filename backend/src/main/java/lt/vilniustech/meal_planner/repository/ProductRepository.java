package lt.vilniustech.meal_planner.repository;

import java.util.List;
import lt.vilniustech.meal_planner.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByNameAsc();
}
