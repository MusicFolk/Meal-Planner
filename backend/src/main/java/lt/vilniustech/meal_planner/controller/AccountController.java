package lt.vilniustech.meal_planner.controller;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lt.vilniustech.meal_planner.dto.UserDtos.ClientDataExport;
import lt.vilniustech.meal_planner.dto.UserDtos.SettingsDto;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.AccountService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/settings")
    public SettingsDto settings(@AuthenticationPrincipal AppUserDetails user) {
        return accounts.settings(user.getId());
    }

    @PutMapping("/settings")
    public SettingsDto updateSettings(@AuthenticationPrincipal AppUserDetails user,
                                      @Valid @RequestBody SettingsDto settings) {
        return accounts.updateSettings(user.getId(), settings);
    }

    /** Failas „mealplanner_duomenys_MMMM-MM-DD.json“ (FR-45). */
    @GetMapping("/export")
    public ResponseEntity<ClientDataExport> export(@AuthenticationPrincipal AppUserDetails user) {
        String filename = "mealplanner_duomenys_" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build()
                        .toString())
                .body(accounts.export(user.getId()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserDetails user) {
        accounts.delete(user.getId());
    }
}
