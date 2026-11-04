package lt.vilniustech.meal_planner.service;

import java.security.SecureRandom;
import java.util.List;
import lt.vilniustech.meal_planner.dto.UserDtos.AccountStatus;
import lt.vilniustech.meal_planner.dto.UserDtos.CreateDietitianRequest;
import lt.vilniustech.meal_planner.dto.UserDtos.CreatedDietitianDto;
import lt.vilniustech.meal_planner.dto.UserDtos.UserDto;
import lt.vilniustech.meal_planner.entity.Dietitian;
import lt.vilniustech.meal_planner.entity.User;
import lt.vilniustech.meal_planner.enums.Role;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminUserService {

    private static final String LETTERS = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ISessions sessions;
    private final SecureRandom random = new SecureRandom();

    public AdminUserService(UserRepository users, PasswordEncoder passwordEncoder, ISessions sessions) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
    }

    /** Naudotojų sąrašas su filtrais pagal vaidmenį ir būseną (FR-39). */
    @Transactional(readOnly = true)
    public List<UserDto> list(Role role, AccountStatus status) {
        Boolean blocked = status == null ? null : status == AccountStatus.BLOCKED;
        return users.search(role, blocked).stream().map(UserDto::of).toList();
    }

    /** Sukuria dietologo paskyrą su vienkartiniu laikinu slaptažodžiu (FR-38). */
    public CreatedDietitianDto createDietitian(CreateDietitianRequest request) {
        if (users.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Šiuo el. paštu paskyra jau užregistruota.", "email");
        }
        String temporaryPassword = generatePassword();
        Dietitian dietitian = new Dietitian(request.email(), passwordEncoder.encode(temporaryPassword),
                request.firstName().trim(), request.lastName().trim());
        users.save(dietitian);
        return new CreatedDietitianDto(UserDto.of(dietitian), temporaryPassword);
    }

    /** Užblokuotas naudotojas negali prisijungti, jo sesijos panaikinamos, duomenys neištrinami (FR-40). */
    public UserDto setBlocked(Long adminId, Long userId, boolean blocked) {
        if (adminId.equals(userId)) {
            throw ApiException.badRequest("Savo paskyros užblokuoti negalima.");
        }
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Naudotojas nerastas."));
        user.setBlocked(blocked);
        if (blocked) {
            sessions.invalidateAll(userId);
        }
        return UserDto.of(user);
    }

    private String generatePassword() {
        String all = LETTERS + DIGITS;
        StringBuilder sb = new StringBuilder();
        sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        while (sb.length() < TEMP_PASSWORD_LENGTH) {
            sb.insert(random.nextInt(sb.length() + 1), all.charAt(random.nextInt(all.length())));
        }
        return sb.toString();
    }
}
