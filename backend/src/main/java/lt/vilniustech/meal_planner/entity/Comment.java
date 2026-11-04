package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

/** Dietologo komentaras visam planui (dish = null) arba konkrečiam patiekalui. */
@Entity
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_request_id")
    private ReviewRequest reviewRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id")
    private Dish dish;

    private String text;

    private Instant createdAt;

    protected Comment() {
    }

    public Comment(ReviewRequest reviewRequest, Dish dish, String text, Instant createdAt) {
        this.reviewRequest = reviewRequest;
        this.dish = dish;
        this.text = text;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public ReviewRequest getReviewRequest() {
        return reviewRequest;
    }

    public Dish getDish() {
        return dish;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
