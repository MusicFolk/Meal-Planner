package lt.vilniustech.meal_planner.service;

import java.util.List;
import java.util.Optional;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ExportedPlanDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.MealPlanDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.RecipeDto;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.ShoppingListDto;
import lt.vilniustech.meal_planner.entity.Dish;
import lt.vilniustech.meal_planner.entity.MealPlan;
import lt.vilniustech.meal_planner.entity.ShoppingListItem;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.mapper.MealPlanMapper;
import lt.vilniustech.meal_planner.repository.DishRepository;
import lt.vilniustech.meal_planner.repository.MealPlanRepository;
import lt.vilniustech.meal_planner.repository.ShoppingListItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MealPlanService implements IMealPlans {

    private final MealPlanRepository plans;
    private final DishRepository dishes;
    private final ShoppingListItemRepository shoppingItems;

    public MealPlanService(MealPlanRepository plans, DishRepository dishes,
                           ShoppingListItemRepository shoppingItems) {
        this.plans = plans;
        this.dishes = dishes;
        this.shoppingItems = shoppingItems;
    }

    @Transactional(readOnly = true)
    public Optional<MealPlanDto> activePlan(Long clientId) {
        return plans.findByClientIdAndActiveTrue(clientId).map(MealPlanMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RecipeDto recipe(Long clientId, Long dishId) {
        Dish dish = dishes.findById(dishId).orElseThrow(() -> ApiException.notFound("Patiekalas nerastas."));
        requireOwner(dish.getMeal().getPlanDay().getMealPlan(), clientId);
        return MealPlanMapper.toRecipe(dish);
    }

    @Transactional(readOnly = true)
    public ShoppingListDto shoppingList(Long clientId, Long planId) {
        MealPlan plan = plans.findById(planId).orElseThrow(() -> ApiException.notFound("Planas nerastas."));
        requireOwner(plan, clientId);
        return MealPlanMapper.toDto(plan.getShoppingList());
    }

    /** Išsaugo produktų sąrašo varnelę serveryje (FR-20). */
    public void setPurchased(Long clientId, Long itemId, boolean purchased) {
        ShoppingListItem item = shoppingItems.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Produktas nerastas."));
        requireOwner(item.getShoppingList().getMealPlan(), clientId);
        item.setPurchased(purchased);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> activePlanId(Long clientId) {
        return plans.findByClientIdAndActiveTrue(clientId).map(MealPlan::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public MealPlanDto planView(Long planId) {
        return MealPlanMapper.toDto(plans.findById(planId)
                .orElseThrow(() -> ApiException.notFound("Planas nerastas.")));
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDto recipeView(Long planId, Long dishId) {
        Dish dish = dishes.findById(dishId).orElseThrow(() -> ApiException.notFound("Patiekalas nerastas."));
        if (!dish.getMeal().getPlanDay().getMealPlan().getId().equals(planId)) {
            throw ApiException.notFound("Patiekalas nerastas.");
        }
        return MealPlanMapper.toRecipe(dish);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean dishBelongsToPlan(Long dishId, Long planId) {
        return dishes.findById(dishId)
                .map(d -> d.getMeal().getPlanDay().getMealPlan().getId().equals(planId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportedPlanDto> exportPlans(Long clientId) {
        return plans.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(plan -> new ExportedPlanDto(MealPlanMapper.toDto(plan),
                        plan.getDays().stream().flatMap(day -> day.getMealsInOrder().stream())
                                .map(meal -> MealPlanMapper.toRecipe(meal.getDish())).toList(),
                        MealPlanMapper.toDto(plan.getShoppingList())))
                .toList();
    }

    /** Klientas mato tik savo duomenis (NFR-54). */
    private static void requireOwner(MealPlan plan, Long clientId) {
        if (plan.getClient() == null || !plan.getClient().getId().equals(clientId)) {
            throw ApiException.notFound("Planas nerastas.");
        }
    }
}
