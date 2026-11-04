package lt.vilniustech.meal_planner.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Dietitian;
import lt.vilniustech.meal_planner.entity.User;
import lt.vilniustech.meal_planner.enums.Role;
import lt.vilniustech.meal_planner.enums.Theme;

public final class AuthDtos {

    static final String PASSWORD_PATTERN = "^(?=.*\\p{L})(?=.*\\d).{8,64}$";
    static final String PASSWORD_MESSAGE =
            "Slaptažodis turi būti 8–64 simbolių ir turėti bent vieną raidę ir vieną skaitmenį.";

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Įveskite vardą.")
            @Size(max = 50, message = "Vardas negali būti ilgesnis nei 50 simbolių.")
            String firstName,
            @NotBlank(message = "Įveskite el. paštą.")
            @Email(message = "Neteisingas el. pašto formatas.")
            @Size(max = 100, message = "El. paštas negali būti ilgesnis nei 100 simbolių.")
            String email,
            @NotBlank(message = "Įveskite slaptažodį.")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String password,
            @NotBlank(message = "Pakartokite slaptažodį.")
            String passwordRepeat,
            @AssertTrue(message = "Būtinas sutikimas tvarkyti sveikatos duomenis.")
            boolean healthDataConsent) {

        @AssertTrue(message = "Slaptažodžiai nesutampa.")
        public boolean isPasswordRepeatMatching() {
            return password == null || password.equals(passwordRepeat);
        }
    }

    public record LoginRequest(
            @NotBlank(message = "Įveskite el. paštą.")
            @Size(max = 100, message = "El. paštas negali būti ilgesnis nei 100 simbolių.")
            String email,
            @NotBlank(message = "Įveskite slaptažodį.")
            @Size(max = 64, message = "Slaptažodis negali būti ilgesnis nei 64 simboliai.")
            String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Įveskite dabartinį slaptažodį.")
            String currentPassword,
            @NotBlank(message = "Įveskite naują slaptažodį.")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String newPassword,
            @NotBlank(message = "Pakartokite naują slaptažodį.")
            String newPasswordRepeat) {

        @AssertTrue(message = "Slaptažodžiai nesutampa.")
        public boolean isNewPasswordRepeatMatching() {
            return newPassword == null || newPassword.equals(newPasswordRepeat);
        }
    }

    public record MeResponse(Long id, String email, String firstName, String lastName, Role role,
                             boolean mustChangePassword, Theme theme, DayOfWeek firstDayOfWeek) {

        public static MeResponse of(User user) {
            Theme theme = user instanceof Client c ? c.getTheme() : Theme.SYSTEM;
            DayOfWeek firstDay = user instanceof Client c ? c.getFirstDayOfWeek() : DayOfWeek.MONDAY;
            boolean mustChange = user instanceof Dietitian d && d.isMustChangePassword();
            return new MeResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                    user.getRole(), mustChange, theme, firstDay);
        }
    }
}
