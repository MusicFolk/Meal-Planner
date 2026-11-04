package lt.vilniustech.meal_planner.entity;

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
import java.time.Instant;
import lt.vilniustech.meal_planner.enums.GenerationStatus;

/**
 * Vienas mitybos plano generavimas su DI. Pagal būseną neleidžiama pradėti antro generavimo ir
 * skaičiuojamas dienos limitas (FR-08, FR-14, FR-15). Sėkmingo generavimo rezultatas – mitybos planas.
 */
@Entity
public class PlanGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id")
    private MealPlan mealPlan;

    private Instant startedAt;

    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    private GenerationStatus status;

    private String model;

    private String errorMessage;

    protected PlanGeneration() {
    }

    public PlanGeneration(Client client, String model, Instant startedAt) {
        this.client = client;
        this.model = model;
        this.startedAt = startedAt;
        this.status = GenerationStatus.IN_PROGRESS;
    }

    public void succeed(MealPlan result, Instant finishedAt) {
        this.mealPlan = result;
        this.status = GenerationStatus.SUCCESS;
        this.finishedAt = finishedAt;
    }

    public void fail(String errorMessage, Instant finishedAt) {
        this.status = GenerationStatus.FAILED;
        this.errorMessage = errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
        this.finishedAt = finishedAt;
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public MealPlan getMealPlan() {
        return mealPlan;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public GenerationStatus getStatus() {
        return status;
    }

    public String getModel() {
        return model;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
