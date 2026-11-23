package lt.vilniustech.meal_planner.controller;

import jakarta.validation.Valid;
import lt.vilniustech.meal_planner.dto.AiConfigurationDtos.AiConfigurationDto;
import lt.vilniustech.meal_planner.dto.AiConfigurationDtos.UpdateAiConfigurationRequest;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.AiConfigurationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** DI parametrai: modelis ir dienos generavimų limitas (NI-20, FR-41). */
@RestController
@RequestMapping("/api/admin/ai-configuration")
public class AdminAiConfigurationController {

    private final AiConfigurationService configuration;

    public AdminAiConfigurationController(AiConfigurationService configuration) {
        this.configuration = configuration;
    }

    @GetMapping
    public AiConfigurationDto current() {
        return configuration.current();
    }

    @PutMapping
    public AiConfigurationDto update(@AuthenticationPrincipal AppUserDetails user,
                                     @Valid @RequestBody UpdateAiConfigurationRequest request) {
        return configuration.update(user.getId(), request);
    }
}
