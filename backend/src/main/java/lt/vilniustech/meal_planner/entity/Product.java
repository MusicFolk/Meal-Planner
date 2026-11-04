package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;
import lt.vilniustech.meal_planner.enums.ProductCategory;

/** Maisto produktas su maistine verte 100 g iš USDA FoodData Central (NFR-08). */
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private Integer fdcId;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @Embedded
    private NutritionValue nutritionPer100g;

    @ManyToMany
    @JoinTable(name = "product_allergen",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id"))
    private Set<Allergen> allergens = new HashSet<>();

    protected Product() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getFdcId() {
        return fdcId;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public NutritionValue getNutritionPer100g() {
        return nutritionPer100g;
    }

    public Set<Allergen> getAllergens() {
        return allergens;
    }
}
