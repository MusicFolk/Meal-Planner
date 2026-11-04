package lt.vilniustech.meal_planner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ExportedPlanDto;
import lt.vilniustech.meal_planner.dto.PaymentDtos.PaymentDto;
import lt.vilniustech.meal_planner.dto.PaymentDtos.SubscriptionDto;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.QuestionnaireResponse;
import lt.vilniustech.meal_planner.dto.ReviewDtos.ClientReviewDto;
import lt.vilniustech.meal_planner.entity.User;
import lt.vilniustech.meal_planner.enums.Role;
import lt.vilniustech.meal_planner.enums.Theme;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserDto(Long id, String email, String firstName, String lastName, Role role, boolean blocked,
                          Instant createdAt) {

        public static UserDto of(User u) {
            return new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole(),
                    u.isBlocked(), u.getCreatedAt());
        }
    }

    public enum AccountStatus {
        ACTIVE,
        BLOCKED
    }

    public record CreateDietitianRequest(
            @NotBlank(message = "Įveskite vardą.")
            @Size(max = 50, message = "Vardas negali būti ilgesnis nei 50 simbolių.")
            String firstName,
            @NotBlank(message = "Įveskite pavardę.")
            @Size(max = 50, message = "Pavardė negali būti ilgesnė nei 50 simbolių.")
            String lastName,
            @NotBlank(message = "Įveskite el. paštą.")
            @Email(message = "Neteisingas el. pašto formatas.")
            @Size(max = 100, message = "El. paštas negali būti ilgesnis nei 100 simbolių.")
            String email) {
    }

    /** Laikinas slaptažodis parodomas tik vieną kartą (FR-38). */
    public record CreatedDietitianDto(UserDto user, String temporaryPassword) {
    }

    public record SettingsDto(
            @NotNull(message = "Pasirinkite temą.") Theme theme,
            @NotNull(message = "Pasirinkite savaitės pirmąją dieną.") DayOfWeek firstDayOfWeek) {
    }

    public record ProfileExport(String firstName, String email, Instant registeredAt, boolean healthDataConsent,
                                Theme theme, DayOfWeek firstDayOfWeek) {
    }

    /** Visi kliento duomenys vienu JSON failu (FR-45, NFR-10). */
    public record ClientDataExport(Instant exportedAt, ProfileExport profile, QuestionnaireResponse questionnaire,
                                   List<ExportedPlanDto> mealPlans, List<ClientReviewDto> reviews,
                                   SubscriptionDto subscription, List<PaymentDto> payments) {
    }
}
