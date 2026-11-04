package lt.vilniustech.meal_planner.controller;

import jakarta.validation.Valid;
import java.util.List;
import lt.vilniustech.meal_planner.dto.UserDtos.AccountStatus;
import lt.vilniustech.meal_planner.dto.UserDtos.CreateDietitianRequest;
import lt.vilniustech.meal_planner.dto.UserDtos.CreatedDietitianDto;
import lt.vilniustech.meal_planner.dto.UserDtos.UserDto;
import lt.vilniustech.meal_planner.enums.Role;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AdminUserService admin;

    public AdminUserController(AdminUserService admin) {
        this.admin = admin;
    }

    @GetMapping("/users")
    public List<UserDto> users(@RequestParam(required = false) Role role,
                               @RequestParam(required = false) AccountStatus status) {
        return admin.list(role, status);
    }

    @PostMapping("/dietitians")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedDietitianDto createDietitian(@Valid @RequestBody CreateDietitianRequest request) {
        return admin.createDietitian(request);
    }

    @PostMapping("/users/{id}/block")
    public UserDto block(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return admin.setBlocked(user.getId(), id, true);
    }

    @PostMapping("/users/{id}/unblock")
    public UserDto unblock(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return admin.setBlocked(user.getId(), id, false);
    }
}
