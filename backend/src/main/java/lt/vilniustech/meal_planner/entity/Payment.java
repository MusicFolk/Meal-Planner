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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lt.vilniustech.meal_planner.enums.PaymentMethod;
import lt.vilniustech.meal_planner.enums.PaymentPurpose;
import lt.vilniustech.meal_planner.enums.PaymentStatus;

/** Mokėjimas už prenumeratą arba peržiūrą (FR-24–FR-26, FR-43). */
@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unikalus mokėjimo ID, perduodamas bankui. */
    private UUID reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentPurpose purpose;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Instant createdAt;

    /** Peržiūros užsakymo duomenys, kol laukiama banko patvirtinimo. */
    private Long reviewMealPlanId;

    private String reviewQuestion;

    protected Payment() {
    }

    public Payment(Client client, BigDecimal amount, PaymentPurpose purpose, PaymentMethod method, Instant createdAt) {
        this.reference = UUID.randomUUID();
        this.client = client;
        this.amount = amount;
        this.purpose = purpose;
        this.method = method;
        this.status = PaymentStatus.PENDING;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getReference() {
        return reference;
    }

    public Client getClient() {
        return client;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentPurpose getPurpose() {
        return purpose;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getReviewMealPlanId() {
        return reviewMealPlanId;
    }

    public String getReviewQuestion() {
        return reviewQuestion;
    }

    public void setReviewDetails(Long mealPlanId, String question) {
        this.reviewMealPlanId = mealPlanId;
        this.reviewQuestion = question;
    }
}
