package lt.vilniustech.meal_planner.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.QuestionnaireResponse;
import lt.vilniustech.meal_planner.entity.Product;

/** Anketos sąsaja kitiems komponentams (ANK-5). */
public interface IQuestionnaire {

    /** Kliento paros kalorijų poreikis, jei anketa užpildyta. */
    Optional<Integer> dailyCalories(Long clientId);

    /** Anketa be kontaktinių duomenų (be vardo ir el. pašto) – dietologui (FR-35, NFR-54). */
    Optional<QuestionnaireResponse> anonymized(Long clientId);

    /**
     * Plano generavimo duomenys: paros poreikis ir leistinų produktų sąrašas be kliento alergenų ir
     * nemėgstamų produktų (ANK-5, FR-10). Kiti anketos duomenys neperduodami.
     */
    Optional<GenerationInput> generationInput(Long clientId);

    /**
     * @param forbiddenProductIds produktai su kliento alergenais ir nemėgstami produktai – galutinei plano
     *                            patikrai (FR-13)
     */
    record GenerationInput(int dailyCalories, List<Product> allowedProducts, Set<Long> forbiddenProductIds) {
    }
}
