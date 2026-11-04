package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lt.vilniustech.meal_planner.enums.MealType;

/** Valgymas (pusryčiai, pietūs arba vakarienė) su kalorijų tikslu. */
@Entity
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_day_id")
    private PlanDay planDay;

    @Enumerated(EnumType.STRING)
    private MealType type;

    private Integer targetCalories;

    @OneToOne(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    private Dish dish;

    protected Meal() {
    }

    Meal(PlanDay planDay, MealType type, Integer targetCalories) {
        this.planDay = planDay;
        this.type = type;
        this.targetCalories = targetCalories;
    }

    public Dish setDish(String name) {
        this.dish = new Dish(this, name);
        return dish;
    }

    public Long getId() {
        return id;
    }

    public PlanDay getPlanDay() {
        return planDay;
    }

    public MealType getType() {
        return type;
    }

    public Integer getTargetCalories() {
        return targetCalories;
    }

    public Dish getDish() {
        return dish;
    }
}
