package lt.vilniustech.meal_planner.service;

import java.util.List;
import java.util.Optional;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ExportedPlanDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.MealPlanDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.RecipeDto;

/** Plano turinio sąsaja kitiems komponentams, pvz., peržiūrų (PLN-6). */
public interface IMealPlans {

    Optional<Long> activePlanId(Long clientId);

    MealPlanDto planView(Long planId);

    RecipeDto recipeView(Long planId, Long dishId);

    /** Ar patiekalas priklauso planui. */
    boolean dishBelongsToPlan(Long dishId, Long planId);

    /** Visi kliento planai su receptais ir produktų sąrašais – duomenų eksportui (FR-45). */
    List<ExportedPlanDto> exportPlans(Long clientId);
}
