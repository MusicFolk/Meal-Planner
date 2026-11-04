package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.time.Instant;
import lt.vilniustech.meal_planner.enums.SubscriptionStatus;

/** Kliento prenumerata. Būsena apskaičiuojama pagal datas užklausos metu (FR-42–FR-44). */
@Entity
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private Instant trialEndsAt;

    private Instant paidUntil;

    protected Subscription() {
    }

    public Subscription(Client client, Instant trialEndsAt) {
        this.client = client;
        this.trialEndsAt = trialEndsAt;
    }

    /** Išvestinis atributas /status. Prenumerata automatiškai nepratęsiama (FR-44). */
    public SubscriptionStatus status(Instant now) {
        if (paidUntil != null && paidUntil.isAfter(now)) {
            return SubscriptionStatus.ACTIVE;
        }
        if (trialEndsAt.isAfter(now)) {
            return SubscriptionStatus.TRIAL;
        }
        return SubscriptionStatus.EXPIRED;
    }

    /** Galiojimo pabaiga pagal dabartinę būseną. */
    public Instant validUntil(Instant now) {
        return switch (status(now)) {
            case ACTIVE -> paidUntil;
            case TRIAL -> trialEndsAt;
            case EXPIRED -> paidUntil != null && paidUntil.isAfter(trialEndsAt) ? paidUntil : trialEndsAt;
        };
    }

    public Long getId() {
        return id;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public Instant getPaidUntil() {
        return paidUntil;
    }

    public void setPaidUntil(Instant paidUntil) {
        this.paidUntil = paidUntil;
    }
}
