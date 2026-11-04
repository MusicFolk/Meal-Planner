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
import java.time.Instant;
import lt.vilniustech.meal_planner.enums.AiRequestResult;
import lt.vilniustech.meal_planner.enums.MealType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Vienas DI kvietimas vienam valgymo tipui: bandymo numeris, kalorijų tikslas, užklausa ir atsakymas
 * JSONB formatu, patikros rezultatas ir trukmė (FR-10–FR-14, NFR-05).
 */
@Entity
public class AiGenerationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_generation_id")
    private PlanGeneration planGeneration;

    @Enumerated(EnumType.STRING)
    private MealType mealType;

    private int attempt;

    private int targetCalories;

    @JdbcTypeCode(SqlTypes.JSON)
    private String requestJson;

    @JdbcTypeCode(SqlTypes.JSON)
    private String responseJson;

    @Enumerated(EnumType.STRING)
    private AiRequestResult result;

    private int durationMs;

    private Instant createdAt;

    protected AiGenerationRequest() {
    }

    public AiGenerationRequest(PlanGeneration planGeneration, MealType mealType, int attempt, int targetCalories,
                               String requestJson, String responseJson, AiRequestResult result, int durationMs,
                               Instant createdAt) {
        this.planGeneration = planGeneration;
        this.mealType = mealType;
        this.attempt = attempt;
        this.targetCalories = targetCalories;
        this.requestJson = requestJson;
        this.responseJson = responseJson;
        this.result = result;
        this.durationMs = durationMs;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public PlanGeneration getPlanGeneration() {
        return planGeneration;
    }

    public MealType getMealType() {
        return mealType;
    }

    public int getAttempt() {
        return attempt;
    }

    public int getTargetCalories() {
        return targetCalories;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public AiRequestResult getResult() {
        return result;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
