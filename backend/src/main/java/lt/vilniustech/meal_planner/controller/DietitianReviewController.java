package lt.vilniustech.meal_planner.controller;

import jakarta.validation.Valid;
import java.util.List;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.RecipeDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.CommentDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.CommentRequest;
import lt.vilniustech.meal_planner.dto.ReviewDtos.DietitianReviewDetailDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.DietitianReviewSummaryDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.SubmitRequest;
import lt.vilniustech.meal_planner.dto.ReviewDtos.UpdateCommentRequest;
import lt.vilniustech.meal_planner.enums.ReviewStatus;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dietitian/reviews")
public class DietitianReviewController {

    private final ReviewService reviews;

    public DietitianReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping
    public List<DietitianReviewSummaryDto> list(@AuthenticationPrincipal AppUserDetails user,
                                                @RequestParam(required = false) ReviewStatus status) {
        return reviews.dietitianList(user.getId(), status);
    }

    @PostMapping("/{id}/open")
    public DietitianReviewDetailDto open(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return reviews.open(user.getId(), id);
    }

    @GetMapping("/{id}/dishes/{dishId}")
    public RecipeDto recipe(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id,
                            @PathVariable Long dishId) {
        return reviews.recipe(user.getId(), id, dishId);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id,
                                 @Valid @RequestBody CommentRequest request) {
        return reviews.addDishComment(user.getId(), id, request);
    }

    @PutMapping("/{id}/comments/{commentId}")
    public CommentDto updateComment(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id,
                                    @PathVariable Long commentId, @Valid @RequestBody UpdateCommentRequest request) {
        return reviews.updateComment(user.getId(), id, commentId, request.text());
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id,
                              @PathVariable Long commentId) {
        reviews.deleteComment(user.getId(), id, commentId);
    }

    @PostMapping("/{id}/submit")
    public DietitianReviewDetailDto submit(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id,
                                           @Valid @RequestBody SubmitRequest request) {
        return reviews.submit(user.getId(), id, request.generalComment());
    }
}
