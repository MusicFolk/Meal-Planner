package lt.vilniustech.meal_planner.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lt.vilniustech.meal_planner.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReference(UUID reference);

    List<Payment> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /** Nuasmenina kliento mokėjimus: lieka tik suma, paskirtis ir data apskaitai (FR-46). */
    @Modifying
    @Query("""
            update Payment p set p.client = null, p.reviewMealPlanId = null, p.reviewQuestion = null
            where p.client.id = :clientId
            """)
    void anonymizeByClientId(@Param("clientId") Long clientId);
}
