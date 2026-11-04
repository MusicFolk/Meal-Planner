package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;

/** Patiekalo receptas, jungiantis ingredientus ir gaminimo žingsnius. */
@Entity
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id")
    private Dish dish;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Ingredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepNumber")
    private List<RecipeStep> steps = new ArrayList<>();

    protected Recipe() {
    }

    Recipe(Dish dish) {
        this.dish = dish;
    }

    public void addIngredient(Product product, int grams) {
        ingredients.add(new Ingredient(this, product, grams));
    }

    public void addStep(String description) {
        steps.add(new RecipeStep(this, steps.size() + 1, description));
    }

    public Long getId() {
        return id;
    }

    public Dish getDish() {
        return dish;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public List<RecipeStep> getSteps() {
        return steps;
    }
}
