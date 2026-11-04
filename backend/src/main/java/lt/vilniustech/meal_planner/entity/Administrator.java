package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lt.vilniustech.meal_planner.enums.Role;

@Entity
@DiscriminatorValue("ADMIN")
public class Administrator extends User {

    protected Administrator() {
    }

    @Override
    public Role role() {
        return Role.ADMIN;
    }
}
