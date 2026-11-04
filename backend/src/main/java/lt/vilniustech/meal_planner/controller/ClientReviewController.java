package lt.vilniustech.meal_planner.controller;

import jakarta.validation.Valid;
import java.util.List;
import lt.vilniustech.meal_planner.dto.PaymentDtos.PaymentInitiated;
import lt.vilniustech.meal_planner.dto.ReviewDtos.ClientReviewDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.OrderInfoDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.OrderRequest;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ClientReviewController {

    private final ReviewService reviews;

    public ClientReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/order-info")
    public OrderInfoDto orderInfo(@AuthenticationPrincipal AppUserDetails user) {
        return reviews.orderInfo(user.getId());
    }

    @PostMapping("/order")
    public PaymentInitiated order(@AuthenticationPrincipal AppUserDetails user,
                                  @Valid @RequestBody OrderRequest request) {
        return reviews.order(user.getId(), request);
    }

    /** Aktualaus plano peržiūra arba 204, jei peržiūra neužsakyta. */
    @GetMapping("/current")
    public ResponseEntity<ClientReviewDto> current(@AuthenticationPrincipal AppUserDetails user) {
        return reviews.currentReview(user.getId()).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public List<ClientReviewDto> all(@AuthenticationPrincipal AppUserDetails user) {
        return reviews.clientReviews(user.getId());
    }
}
