package lt.vilniustech.meal_planner.repository;

import java.util.List;
import java.util.Optional;
import lt.vilniustech.meal_planner.entity.User;
import lt.vilniustech.meal_planner.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (:blocked is null or u.blocked = :blocked)
            order by u.createdAt desc
            """)
    List<User> search(@Param("role") Role role, @Param("blocked") Boolean blocked);

    /** Ištrina naudotoją; susiję duomenys pašalinami DB išorinių raktų taisyklėmis (ON DELETE CASCADE). */
    @Modifying(clearAutomatically = true)
    @Query("delete from User u where u.id = :id")
    void hardDelete(@Param("id") Long id);
}
