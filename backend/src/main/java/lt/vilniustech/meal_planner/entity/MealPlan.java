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
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Savaitės mitybos planas – struktūrinė klasė, kurią sudaro 7 dienos ir produktų sąrašas.
 * Planas be kliento (client = null) yra demonstracinis šablonas (1 sprintas).
 */
@Entity
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private Instant createdAt;

    private boolean active;

    private boolean aiGenerated;

    private Integer dailyCalories;

    @OneToMany(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber")
    private List<PlanDay> days = new ArrayList<>();

    @OneToOne(mappedBy = "mealPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private ShoppingList shoppingList;

    protected MealPlan() {
    }

    public MealPlan(Client client, Instant createdAt, int dailyCalories, boolean aiGenerated) {
        this.client = client;
        this.createdAt = createdAt;
        this.dailyCalories = dailyCalories;
        this.aiGenerated = aiGenerated;
        this.active = true;
    }

    public PlanDay addDay(int dayNumber) {
        PlanDay day = new PlanDay(this, dayNumber);
        days.add(day);
        return day;
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAiGenerated() {
        return aiGenerated;
    }

    public Integer getDailyCalories() {
        return dailyCalories;
    }

    public List<PlanDay> getDays() {
        return days;
    }

    public ShoppingList getShoppingList() {
        return shoppingList;
    }

    public void setShoppingList(ShoppingList shoppingList) {
        this.shoppingList = shoppingList;
    }
}
