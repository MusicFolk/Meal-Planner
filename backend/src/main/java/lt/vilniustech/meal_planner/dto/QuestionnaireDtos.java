package lt.vilniustech.meal_planner.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lt.vilniustech.meal_planner.calories.ActivityLevel;
import lt.vilniustech.meal_planner.calories.BmiCategory;
import lt.vilniustech.meal_planner.calories.Gender;
import lt.vilniustech.meal_planner.calories.Goal;
import lt.vilniustech.meal_planner.dto.CatalogDtos.AllergenDto;
import lt.vilniustech.meal_planner.dto.CatalogDtos.ProductDto;

public final class QuestionnaireDtos {

    private QuestionnaireDtos() {
    }

    /** Anketos duomenys su ribomis pagal 17 lentelę (FR-01, FR-02). */
    public record QuestionnaireRequest(
            @NotNull(message = "Įveskite ūgį.")
            @Min(value = 100, message = "Ūgis turi būti nuo 100 iki 250 cm.")
            @Max(value = 250, message = "Ūgis turi būti nuo 100 iki 250 cm.")
            Integer heightCm,
            @NotNull(message = "Įveskite svorį.")
            @DecimalMin(value = "30.0", message = "Svoris turi būti nuo 30 iki 300 kg.")
            @DecimalMax(value = "300.0", message = "Svoris turi būti nuo 30 iki 300 kg.")
            @Digits(integer = 3, fraction = 1, message = "Svoris nurodomas 0,1 kg tikslumu.")
            BigDecimal weightKg,
            @NotNull(message = "Įveskite amžių.")
            @Min(value = 18, message = "Amžius turi būti nuo 18 iki 100 metų.")
            @Max(value = 100, message = "Amžius turi būti nuo 18 iki 100 metų.")
            Integer age,
            @NotNull(message = "Pasirinkite lytį.")
            Gender gender,
            @NotNull(message = "Pasirinkite fizinio aktyvumo lygį.")
            ActivityLevel activityLevel,
            @NotNull(message = "Pasirinkite tikslą.")
            Goal goal,
            @NotNull(message = "Nurodykite, ar sergate lėtine liga.")
            Boolean chronicDisease,
            List<Long> allergenIds,
            @Size(max = 20, message = "Galima nurodyti ne daugiau kaip 20 nemėgstamų produktų.")
            List<Long> dislikedProductIds) {
    }

    /** Skaičiavimo paaiškinimas: BMR, aktyvumo koeficientas, tikslo korekcija (FR-05). */
    public record CalorieExplanation(double bmr, double activityFactor, int afterActivity, double goalFactor,
                                     int calculatedCalories, int safeMinimum, boolean safeLimitApplied) {
    }

    public record Metrics(BigDecimal bmi, BmiCategory bmiCategory, int dailyCalories,
                          CalorieExplanation explanation) {
    }

    public record QuestionnaireResponse(int heightCm, BigDecimal weightKg, int age, Gender gender,
                                        ActivityLevel activityLevel, Goal goal, boolean chronicDisease,
                                        List<AllergenDto> allergens, List<ProductDto> dislikedProducts,
                                        Instant updatedAt, Metrics metrics) {
    }

    /**
     * Išsaugojimo rezultatas. firstTime – anketa pildyta pirmą kartą; kitu atveju klientui siūloma
     * perkurti planą (FR-07). chronicDiseaseWarning – rekomendacija kreiptis į dietologą (FR-03).
     */
    public record SaveResponse(QuestionnaireResponse questionnaire, boolean firstTime,
                               boolean chronicDiseaseWarning, boolean safeLimitApplied) {
    }
}
