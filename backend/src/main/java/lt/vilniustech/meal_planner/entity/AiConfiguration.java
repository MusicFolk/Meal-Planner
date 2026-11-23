package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

/**
 * DI parametrai: naudojamas DI modelis ir dienos generavimų limitas vienam klientui (NI-20, FR-41).
 * Kiekvienas pakeitimas išsaugomas nauju įrašu su keitimo data ir administratoriumi.
 */
@Entity
public class AiConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String model;

    private int dailyGenerationLimit;

    private Instant changedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private Administrator changedBy;

    protected AiConfiguration() {
    }

    public AiConfiguration(String model, int dailyGenerationLimit, Instant changedAt, Administrator changedBy) {
        this.model = model;
        this.dailyGenerationLimit = dailyGenerationLimit;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }

    public Long getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public int getDailyGenerationLimit() {
        return dailyGenerationLimit;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public Administrator getChangedBy() {
        return changedBy;
    }
}
