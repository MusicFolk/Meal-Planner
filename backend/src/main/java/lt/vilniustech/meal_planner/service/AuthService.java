package lt.vilniustech.meal_planner.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lt.vilniustech.meal_planner.dto.AuthDtos.ChangePasswordRequest;
import lt.vilniustech.meal_planner.dto.AuthDtos.LoginRequest;
import lt.vilniustech.meal_planner.dto.AuthDtos.MeResponse;
import lt.vilniustech.meal_planner.dto.AuthDtos.RegisterRequest;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Dietitian;
import lt.vilniustech.meal_planner.entity.User;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.UserRepository;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    static final String INVALID_CREDENTIALS = "Neteisingas el. paštas arba slaptažodis.";
    static final String LOCKED = "Prisijungimas užrakintas 15 minučių. Bandykite vėliau.";
    static final String BLOCKED = "Paskyra užblokuota. Kreipkitės į sistemos administratorių.";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final SessionService sessions;
    private final ISubscription subscriptions;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       SecurityContextRepository securityContextRepository, SessionService sessions,
                       ISubscription subscriptions, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.sessions = sessions;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    /** Užregistruoja klientą ir pradeda 7 dienų bandomąjį laikotarpį (FR-28, FR-29). */
    @Transactional
    public void register(RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Šiuo el. paštu paskyra jau užregistruota.", "email");
        }
        Client client = new Client(request.email(), passwordEncoder.encode(request.password()),
                request.firstName().trim());
        users.save(client);
        subscriptions.startTrial(client);
    }

    /**
     * Patikrina prisijungimo duomenis ir sukuria sesiją (FR-30, FR-31). Nesėkmingi bandymai
     * įrašomi atskira transakcija, kad jie išliktų ir grąžinus klaidą.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public MeResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Instant now = clock.instant();
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));
        if (user.isLocked(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, LOCKED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(now.plus(LOCK_DURATION));
                throw new ApiException(HttpStatus.UNAUTHORIZED, LOCKED);
            }
            user.setFailedLoginAttempts(attempts);
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        if (user.isBlocked()) {
            throw new ApiException(HttpStatus.FORBIDDEN, BLOCKED);
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        createSession(user, httpRequest, httpResponse);
        return MeResponse.of(user);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        return MeResponse.of(findUser(userId));
    }

    /** Pakeičia slaptažodį; dietologui tai privaloma prisijungus pirmą kartą (FR-38). */
    @Transactional
    public MeResponse changePassword(Long userId, ChangePasswordRequest request, HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Neteisingas dabartinis slaptažodis.", "currentPassword");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Naujas slaptažodis turi skirtis nuo dabartinio.",
                    "newPassword");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        if (user instanceof Dietitian dietitian) {
            dietitian.setMustChangePassword(false);
        }
        // Atnaujinamos sesijos teisės (pvz., dietologas gauna ROLE_DIETITIAN)
        createSession(user, httpRequest, httpResponse);
        return MeResponse.of(user);
    }

    private void createSession(User user, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AppUserDetails principal = new AppUserDetails(user);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        sessions.register(user.getId(), httpRequest.getSession());
    }

    private User findUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("Naudotojas nerastas."));
    }
}
