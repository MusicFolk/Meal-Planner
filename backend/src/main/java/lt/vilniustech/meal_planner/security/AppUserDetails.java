package lt.vilniustech.meal_planner.security;

import java.util.List;
import lt.vilniustech.meal_planner.entity.Dietitian;
import lt.vilniustech.meal_planner.entity.User;
import lt.vilniustech.meal_planner.enums.Role;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Prisijungusio naudotojo duomenys sesijoje. Dietologas, kuris dar nepakeitė laikino slaptažodžio,
 * gauna tik ROLE_DIETITIAN_PENDING teisę ir dietologo funkcijų nepasiekia (FR-38).
 */
public class AppUserDetails extends org.springframework.security.core.userdetails.User {

    private final Long id;
    private final Role role;
    private final boolean mustChangePassword;

    public AppUserDetails(User user) {
        super(user.getEmail(), user.getPasswordHash(), authorities(user));
        this.id = user.getId();
        this.role = user.getRole();
        this.mustChangePassword = user instanceof Dietitian d && d.isMustChangePassword();
    }

    private static List<SimpleGrantedAuthority> authorities(User user) {
        if (user instanceof Dietitian d && d.isMustChangePassword()) {
            return List.of(new SimpleGrantedAuthority("ROLE_DIETITIAN_PENDING"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
