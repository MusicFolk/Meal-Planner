package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.Optional;
import lt.vilniustech.meal_planner.enums.ReviewStatus;

/** Apmokėta plano peržiūros užklausa su kliento klausimu, būsena ir priskirtu dietologu. */
@Entity
public class ReviewRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id")
    private MealPlan mealPlan;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dietitian_id")
    private Dietitian dietitian;

    private String question;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    private Instant submittedAt;

    private Instant reviewedAt;

    @OneToMany(mappedBy = "reviewRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt, id")
    private List<Comment> comments = new ArrayList<>();

    protected ReviewRequest() {
    }

    public ReviewRequest(Client client, MealPlan mealPlan, Payment payment, String question, Instant submittedAt) {
        this.client = client;
        this.mealPlan = mealPlan;
        this.payment = payment;
        this.question = question;
        this.status = ReviewStatus.PENDING;
        this.submittedAt = submittedAt;
    }

    public void assignTo(Dietitian dietitian) {
        this.dietitian = dietitian;
        this.status = ReviewStatus.IN_REVIEW;
    }

    public void complete(Instant reviewedAt) {
        this.status = ReviewStatus.COMPLETED;
        this.reviewedAt = reviewedAt;
    }

    public boolean isAssignedTo(Long dietitianId) {
        return dietitian != null && dietitian.getId().equals(dietitianId);
    }

    public Optional<Comment> generalComment() {
        return comments.stream().filter(c -> c.getDish() == null).findFirst();
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

    public Dietitian getDietitian() {
        return dietitian;
    }

    public String getQuestion() {
        return question;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public List<Comment> getComments() {
        return comments;
    }
}
