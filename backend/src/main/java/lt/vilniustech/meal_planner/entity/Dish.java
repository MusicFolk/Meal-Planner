package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/** Valgymui parinktas patiekalas su porcija ir maistine verte. */
@Entity
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id")
    private Meal meal;

    private String name;

    private int portionGrams;

    @Embedded
    private NutritionValue nutrition = NutritionValue.zero();

    @OneToOne(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    private Recipe recipe;

    protected Dish() {
    }

    Dish(Meal meal, String name) {
        this.meal = meal;
        this.name = name;
        this.recipe = new Recipe(this);
    }

    public Long getId() {
        return id;
    }

    public Meal getMeal() {
        return meal;
    }

    public String getName() {
        return name;
    }

    public int getPortionGrams() {
        return portionGrams;
    }

    public NutritionValue getNutrition() {
        return nutrition;
    }

    public void setCalculated(int portionGrams, NutritionValue nutrition) {
        this.portionGrams = portionGrams;
        this.nutrition = nutrition;
    }

    public Recipe getRecipe() {
        return recipe;
    }
}
