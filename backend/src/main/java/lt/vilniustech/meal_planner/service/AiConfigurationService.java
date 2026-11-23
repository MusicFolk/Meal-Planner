package lt.vilniustech.meal_planner.service;

import java.time.Clock;
import lt.vilniustech.meal_planner.dto.AiConfigurationDtos.AiConfigurationDto;
import lt.vilniustech.meal_planner.dto.AiConfigurationDtos.UpdateAiConfigurationRequest;
import lt.vilniustech.meal_planner.entity.Administrator;
import lt.vilniustech.meal_planner.entity.AiConfiguration;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.AiConfigurationRepository;
import lt.vilniustech.meal_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DI parametrų administravimas (NAU-3, FR-41). Galioja naujausias įrašas; jei jų nėra – aplinkos kintamasis
 * GEMINI_MODEL ir konfigūracijoje nurodytas dienos limitas (DIA-4). Nauji parametrai taikomi nuo kito generavimo.
 */
@Service
@Transactional
public class AiConfigurationService implements IAiConfiguration {

    private final AiConfigurationRepository configurations;
    private final UserRepository users;
    private final Clock clock;
    private final String defaultModel;
    private final int defaultLimit;

    public AiConfigurationService(AiConfigurationRepository configurations, UserRepository users, Clock clock,
                                  @Value("${app.ai.model}") String defaultModel,
                                  @Value("${app.plans.daily-generation-limit}") int defaultLimit) {
        this.configurations = configurations;
        this.users = users;
        this.clock = clock;
        this.defaultModel = defaultModel;
        this.defaultLimit = defaultLimit;
    }

    @Transactional(readOnly = true)
    public AiConfigurationDto current() {
        return configurations.findFirstByOrderByChangedAtDescIdDesc()
                .map(c -> new AiConfigurationDto(c.getModel(), c.getDailyGenerationLimit(), c.getChangedAt(),
                        c.getChangedBy() == null ? null : c.getChangedBy().getFullName()))
                .orElseGet(() -> new AiConfigurationDto(defaultModel, defaultLimit, null, null));
    }

    /** Išsaugo naujus DI parametrus nauju įrašu su keitimo data ir administratoriumi. */
    public AiConfigurationDto update(Long adminId, UpdateAiConfigurationRequest request) {
        Administrator admin = users.findById(adminId).filter(Administrator.class::isInstance)
                .map(Administrator.class::cast)
                .orElseThrow(() -> ApiException.forbidden("Neturite teisės atlikti šio veiksmo."));
        configurations.save(new AiConfiguration(request.model().trim(), request.dailyGenerationLimit(),
                clock.instant(), admin));
        return current();
    }

    @Override
    @Transactional(readOnly = true)
    public String model() {
        return current().model();
    }

    @Override
    @Transactional(readOnly = true)
    public int dailyGenerationLimit() {
        return current().dailyGenerationLimit();
    }
}
