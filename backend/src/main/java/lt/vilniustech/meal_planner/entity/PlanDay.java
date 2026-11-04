package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lt.vilniustech.meal_planner.enums.MealType;

/** Viena plano diena su bendru dienos kaloringumu. */
@Entity
public class PlanDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id")
    private MealPlan mealPlan;

    private int dayNumber;

    private int totalCalories;

    @OneToMany(mappedBy = "planDay", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Meal> meals = new ArrayList<>();

    protected PlanDay() {
    }

    PlanDay(MealPlan mealPlan, int dayNumber) {
        this.mealPlan = mealPlan;
        this.dayNumber = dayNumber;
    }

    public Meal addMeal(MealType type, Integer targetCalories) {
        Meal meal = new Meal(this, type, targetCalories);
        meals.add(meal);
        return meal;
    }

    public List<Meal> getMealsInOrder() {
        return meals.stream().sorted(Comparator.comparing(Meal::getType)).toList();
    }

    public Long getId() {
        return id;
    }

    public MealPlan getMealPlan() {
        return mealPlan;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public List<Meal> getMeals() {
        return meals;
    }
}
