package lt.vilniustech.meal_planner.controller;

import jakarta.validation.Valid;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.QuestionnaireRequest;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.QuestionnaireResponse;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.SaveResponse;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.QuestionnaireService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questionnaire")
public class QuestionnaireController {

    private final QuestionnaireService questionnaires;

    public QuestionnaireController(QuestionnaireService questionnaires) {
        this.questionnaires = questionnaires;
    }

    /** Grąžina anketą arba 204, jei ji dar neužpildyta. */
    @GetMapping
    public ResponseEntity<QuestionnaireResponse> get(@AuthenticationPrincipal AppUserDetails user) {
        return questionnaires.get(user.getId()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public SaveResponse save(@AuthenticationPrincipal AppUserDetails user,
                             @Valid @RequestBody QuestionnaireRequest request) {
        return questionnaires.save(user.getId(), request);
    }
}
