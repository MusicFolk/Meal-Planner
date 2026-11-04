package lt.vilniustech.meal_planner.controller;

import lt.vilniustech.meal_planner.dto.MealPlanDtos.GenerationStatusDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.MealPlanDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.PurchasedRequest;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.RecipeDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ShoppingListDto;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.MealPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-plans")
public class MealPlanController {

    private final MealPlanService plans;

    public MealPlanController(MealPlanService plans) {
        this.plans = plans;
    }

    /** Aktualus planas arba 204, jei plano dar nėra (FR-17). */
    @GetMapping("/active")
    public ResponseEntity<MealPlanDto> active(@AuthenticationPrincipal AppUserDetails user) {
        return plans.activePlan(user.getId()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/generation-status")
    public GenerationStatusDto generationStatus(@AuthenticationPrincipal AppUserDetails user) {
        return plans.generationStatus(user.getId());
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public MealPlanDto generate(@AuthenticationPrincipal AppUserDetails user) {
        return plans.generate(user.getId());
    }

    @GetMapping("/dishes/{dishId}")
    public RecipeDto recipe(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long dishId) {
        return plans.recipe(user.getId(), dishId);
    }

    @GetMapping("/{planId}/shopping-list")
    public ShoppingListDto shoppingList(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long planId) {
        return plans.shoppingList(user.getId(), planId);
    }

    @PutMapping("/shopping-list/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPurchased(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long itemId,
                             @RequestBody PurchasedRequest request) {
        plans.setPurchased(user.getId(), itemId, request.purchased());
    }
}
