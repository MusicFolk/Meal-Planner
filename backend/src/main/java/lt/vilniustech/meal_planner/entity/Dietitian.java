package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lt.vilniustech.meal_planner.enums.Role;

/** Dietologo paskyra; laikiną slaptažodį būtina pakeisti prisijungus pirmą kartą (FR-38). */
@Entity
@DiscriminatorValue("DIETITIAN")
public class Dietitian extends User {

    private Boolean mustChangePassword;

    protected Dietitian() {
    }

    public Dietitian(String email, String passwordHash, String firstName, String lastName) {
        super(email, passwordHash, firstName, lastName);
        this.mustChangePassword = true;
    }

    @Override
    public Role role() {
        return Role.DIETITIAN;
    }

    public boolean isMustChangePassword() {
        return Boolean.TRUE.equals(mustChangePassword);
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
