package lt.vilniustech.meal_planner.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lt.vilniustech.meal_planner.dto.AuthDtos.ChangePasswordRequest;
import lt.vilniustech.meal_planner.dto.AuthDtos.LoginRequest;
import lt.vilniustech.meal_planner.dto.AuthDtos.MeResponse;
import lt.vilniustech.meal_planner.dto.AuthDtos.RegisterRequest;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Sukuria CSRF žetoną slapuke XSRF-TOKEN (NFR-51). */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Map.of("message", "Paskyra sėkmingai sukurta. Galite prisijungti.");
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                            HttpServletResponse httpResponse) {
        return authService.login(request, httpRequest, httpResponse);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AppUserDetails user) {
        return authService.me(user.getId());
    }

    @PostMapping("/change-password")
    public MeResponse changePassword(@AuthenticationPrincipal AppUserDetails user,
                                     @Valid @RequestBody ChangePasswordRequest request,
                                     HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return authService.changePassword(user.getId(), request, httpRequest, httpResponse);
    }
}
