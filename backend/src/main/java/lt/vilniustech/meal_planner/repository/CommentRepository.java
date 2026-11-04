package lt.vilniustech.meal_planner.repository;

import lt.vilniustech.meal_planner.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
