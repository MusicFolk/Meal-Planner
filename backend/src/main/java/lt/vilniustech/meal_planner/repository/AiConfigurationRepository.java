package lt.vilniustech.meal_planner.repository;

import java.util.Optional;
import lt.vilniustech.meal_planner.entity.AiConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConfigurationRepository extends JpaRepository<AiConfiguration, Long> {

    /** Galiojanti konfigūracija – naujausias įrašas. */
    Optional<AiConfiguration> findFirstByOrderByChangedAtDescIdDesc();
}
