package lt.vilniustech.meal_planner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.MealPlanDto;
import lt.vilniustech.meal_planner.dto.QuestionnaireDtos.QuestionnaireResponse;
import lt.vilniustech.meal_planner.entity.Comment;
import lt.vilniustech.meal_planner.enums.PaymentMethod;
import lt.vilniustech.meal_planner.enums.ReviewStatus;

public final class ReviewDtos {

    static final String COMMENT_MESSAGE = "Komentaras negali būti tuščias arba ilgesnis nei 2000 simbolių.";

    private ReviewDtos() {
    }

    public record OrderInfoDto(BigDecimal price, Long planId, boolean canOrder, String reason) {
    }

    public record OrderRequest(
            @NotNull(message = "Pasirinkite mokėjimo būdą.") PaymentMethod method,
            @Size(max = 500, message = "Klausimas negali būti ilgesnis nei 500 simbolių.") String question) {
    }

    public record CommentDto(Long id, Long dishId, String dishName, String text, Instant createdAt) {

        public static CommentDto of(Comment c) {
            return new CommentDto(c.getId(), c.getDish() == null ? null : c.getDish().getId(),
                    c.getDish() == null ? null : c.getDish().getName(), c.getText(), c.getCreatedAt());
        }
    }

    /** Peržiūros būsena ir, jei peržiūra atlikta, komentarai klientui (FR-21, FR-27). */
    public record ClientReviewDto(Long id, Long planId, ReviewStatus status, String question, Instant submittedAt,
                                  Instant reviewedAt, String dietitianName, List<CommentDto> comments) {
    }

    public record DietitianReviewSummaryDto(Long id, ReviewStatus status, Instant submittedAt, String question,
                                            boolean assignedToMe) {
    }

    /** Peržiūros langas dietologui: anketa be kontaktinių duomenų ir visas planas (FR-35). */
    public record DietitianReviewDetailDto(Long id, ReviewStatus status, Instant submittedAt, Instant reviewedAt,
                                           String question, QuestionnaireResponse questionnaire, MealPlanDto plan,
                                           List<CommentDto> comments) {
    }

    public record CommentRequest(
            @NotNull(message = "Pasirinkite patiekalą.") Long dishId,
            @NotBlank(message = COMMENT_MESSAGE) @Size(max = 2000, message = COMMENT_MESSAGE) String text) {
    }

    public record UpdateCommentRequest(
            @NotBlank(message = COMMENT_MESSAGE) @Size(max = 2000, message = COMMENT_MESSAGE) String text) {
    }

    public record SubmitRequest(
            @NotBlank(message = COMMENT_MESSAGE) @Size(max = 2000, message = COMMENT_MESSAGE) String generalComment) {
    }
}
