package lt.vilniustech.meal_planner.service;

import java.util.Optional;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.QuestionnaireResponse;

/** Anketos sąsaja kitiems komponentams (ANK-5). */
public interface IQuestionnaire {

    /** Kliento paros kalorijų poreikis, jei anketa užpildyta. */
    Optional<Integer> dailyCalories(Long clientId);

    /** Anketa be kontaktinių duomenų (be vardo ir el. pašto) – dietologui (FR-35, NFR-54). */
    Optional<QuestionnaireResponse> anonymized(Long clientId);
}
