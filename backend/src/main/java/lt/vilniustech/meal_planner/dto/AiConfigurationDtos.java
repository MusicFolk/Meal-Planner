package lt.vilniustech.meal_planner.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AiConfigurationDtos {

    private AiConfigurationDtos() {
    }

    /**
     * Galiojantys DI parametrai. Jei administratorius jų dar nekeitė, changedAt ir changedBy – null, o reikšmės
     * imamos iš konfigūracijos (GEMINI_MODEL).
     */
    public record AiConfigurationDto(String model, int dailyGenerationLimit, Instant changedAt, String changedBy) {
    }

    public record UpdateAiConfigurationRequest(
            @NotBlank(message = "Įveskite DI modelio pavadinimą.")
            @Size(max = 100, message = "Modelio pavadinimas negali būti ilgesnis nei 100 simbolių.")
            @Pattern(regexp = "[A-Za-z0-9._-]*",
                    message = "Modelio pavadinime gali būti tik raidės, skaitmenys, taškas ir brūkšnelis.")
            String model,
            @NotNull(message = "Įveskite dienos generavimų limitą.")
            @Min(value = 1, message = "Limitas turi būti nuo 1 iki 20.")
            @Max(value = 20, message = "Limitas turi būti nuo 1 iki 20.")
            Integer dailyGenerationLimit) {
    }
}
