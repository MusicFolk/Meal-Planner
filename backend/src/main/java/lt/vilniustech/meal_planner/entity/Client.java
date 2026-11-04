package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.DayOfWeek;
import lt.vilniustech.meal_planner.enums.Role;
import lt.vilniustech.meal_planner.enums.Theme;

/** Kliento paskyra su sutikimu tvarkyti sveikatos duomenis ir sąsajos nustatymais (FR-28, FR-47). */
@Entity
@DiscriminatorValue("CLIENT")
public class Client extends User {

    private Boolean healthDataConsent;

    @Enumerated(EnumType.STRING)
    private Theme theme;

    @Enumerated(EnumType.STRING)
    private DayOfWeek firstDayOfWeek;

    protected Client() {
    }

    public Client(String email, String passwordHash, String firstName) {
        super(email, passwordHash, firstName, null);
        this.healthDataConsent = true;
        this.theme = Theme.SYSTEM;
        this.firstDayOfWeek = DayOfWeek.MONDAY;
    }

    @Override
    public Role role() {
        return Role.CLIENT;
    }

    public boolean isHealthDataConsent() {
        return Boolean.TRUE.equals(healthDataConsent);
    }

    public Theme getTheme() {
        return theme == null ? Theme.SYSTEM : theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public DayOfWeek getFirstDayOfWeek() {
        return firstDayOfWeek == null ? DayOfWeek.MONDAY : firstDayOfWeek;
    }

    public void setFirstDayOfWeek(DayOfWeek firstDayOfWeek) {
        this.firstDayOfWeek = firstDayOfWeek;
    }
}
