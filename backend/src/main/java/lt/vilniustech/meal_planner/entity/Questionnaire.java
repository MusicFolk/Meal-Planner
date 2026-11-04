package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lt.vilniustech.meal_planner.calories.ActivityLevel;
import lt.vilniustech.meal_planner.calories.Gender;
import lt.vilniustech.meal_planner.calories.Goal;

/** Kliento anketa su biometriniais duomenimis, tikslu ir apskaičiuotais KMI bei paros poreikiu (FR-01–FR-07). */
@Entity
public class Questionnaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private int heightCm;

    private BigDecimal weightKg;

    private int age;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    private boolean chronicDisease;

    private BigDecimal bmi;

    private int dailyCalories;

    private Instant updatedAt;

    @ManyToMany
    @JoinTable(name = "questionnaire_allergen",
            joinColumns = @JoinColumn(name = "questionnaire_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id"))
    private Set<Allergen> allergies = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "questionnaire_disliked_product",
            joinColumns = @JoinColumn(name = "questionnaire_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    private Set<Product> dislikedProducts = new HashSet<>();

    protected Questionnaire() {
    }

    public Questionnaire(Client client) {
        this.client = client;
    }

    public void update(int heightCm, BigDecimal weightKg, int age, Gender gender, ActivityLevel activityLevel,
                       Goal goal, boolean chronicDisease) {
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.age = age;
        this.gender = gender;
        this.activityLevel = activityLevel;
        this.goal = goal;
        this.chronicDisease = chronicDisease;
    }

    public void setCalculated(BigDecimal bmi, int dailyCalories, Instant updatedAt) {
        this.bmi = bmi;
        this.dailyCalories = dailyCalories;
        this.updatedAt = updatedAt;
    }

    public void setRestrictions(Set<Allergen> allergies, Set<Product> dislikedProducts) {
        this.allergies.clear();
        this.allergies.addAll(allergies);
        this.dislikedProducts.clear();
        this.dislikedProducts.addAll(dislikedProducts);
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public int getHeightCm() {
        return heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public int getAge() {
        return age;
    }

    public Gender getGender() {
        return gender;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public Goal getGoal() {
        return goal;
    }

    public boolean isChronicDisease() {
        return chronicDisease;
    }

    public BigDecimal getBmi() {
        return bmi;
    }

    public int getDailyCalories() {
        return dailyCalories;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<Allergen> getAllergies() {
        return allergies;
    }

    public Set<Product> getDislikedProducts() {
        return dislikedProducts;
    }
}
