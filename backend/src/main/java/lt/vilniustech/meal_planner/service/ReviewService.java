package lt.vilniustech.meal_planner.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lt.vilniustech.meal_planner.dto.MealPlanDtos.RecipeDto;
import lt.vilniustech.meal_planner.dto.PaymentDtos.PaymentInitiated;
import lt.vilniustech.meal_planner.dto.ReviewDtos;
import lt.vilniustech.meal_planner.dto.ReviewDtos.ClientReviewDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.CommentDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.DietitianReviewDetailDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.DietitianReviewSummaryDto;
import lt.vilniustech.meal_planner.dto.ReviewDtos.OrderInfoDto;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Comment;
import lt.vilniustech.meal_planner.entity.Dietitian;
import lt.vilniustech.meal_planner.entity.Payment;
import lt.vilniustech.meal_planner.entity.ReviewRequest;
import lt.vilniustech.meal_planner.enums.PaymentPurpose;
import lt.vilniustech.meal_planner.enums.ReviewStatus;
import lt.vilniustech.meal_planner.event.PaymentCompletedEvent;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.CommentRepository;
import lt.vilniustech.meal_planner.repository.DishRepository;
import lt.vilniustech.meal_planner.repository.MealPlanRepository;
import lt.vilniustech.meal_planner.repository.ReviewRequestRepository;
import lt.vilniustech.meal_planner.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {

    private static final Set<ReviewStatus> UNFINISHED = Set.of(ReviewStatus.PENDING, ReviewStatus.IN_REVIEW);

    private final ReviewRequestRepository reviews;
    private final CommentRepository comments;
    private final MealPlanRepository planRepository;
    private final DishRepository dishes;
    private final UserRepository users;
    private final IMealPlans plans;
    private final IQuestionnaire questionnaire;
    private final IPayments payments;
    private final IBank bank;
    private final NotificationService notifications;
    private final Clock clock;

    public ReviewService(ReviewRequestRepository reviews, CommentRepository comments,
                         MealPlanRepository planRepository, DishRepository dishes, UserRepository users,
                         IMealPlans plans, IQuestionnaire questionnaire, IPayments payments, IBank bank,
                         NotificationService notifications, Clock clock) {
        this.reviews = reviews;
        this.comments = comments;
        this.planRepository = planRepository;
        this.dishes = dishes;
        this.users = users;
        this.plans = plans;
        this.questionnaire = questionnaire;
        this.payments = payments;
        this.bank = bank;
        this.notifications = notifications;
        this.clock = clock;
    }

    // ---------- Klientas ----------

    /** Peržiūrą galima užsakyti tik aktualiam planui be neužbaigtos užklausos (FR-23). */
    @Transactional(readOnly = true)
    public OrderInfoDto orderInfo(Long clientId) {
        Optional<Long> planId = plans.activePlanId(clientId);
        String reason = null;
        if (planId.isEmpty()) {
            reason = "Peržiūrą galima užsakyti tik turint aktualų mitybos planą.";
        } else if (reviews.existsByMealPlanIdAndStatusIn(planId.get(), UNFINISHED)) {
            reason = "Šiam planui jau užsakyta peržiūra.";
        }
        return new OrderInfoDto(payments.reviewPrice(), planId.orElse(null), reason == null, reason);
    }

    /** Sukuria mokėjimą ir nukreipia klientą į banko sąsają; užklausa sukuriama tik apmokėjus (FR-24). */
    public PaymentInitiated order(Long clientId, ReviewDtos.OrderRequest request) {
        OrderInfoDto info = orderInfo(clientId);
        if (!info.canOrder()) {
            throw new ApiException(HttpStatus.CONFLICT, info.reason());
        }
        Client client = users.findById(clientId).map(Client.class::cast)
                .orElseThrow(() -> ApiException.notFound("Klientas nerastas."));
        String question = request.question() == null || request.question().isBlank() ? null
                : request.question().trim();
        Payment payment = payments.initiateReviewPayment(client, request.method(), info.planId(), question);
        return new PaymentInitiated(payment.getReference(), bank.initiate(payment));
    }

    /** Gavus banko patvirtinimą, sukuriama peržiūros užklausa su būsena „Laukia“ (FR-25). */
    @EventListener
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        Payment payment = event.payment();
        if (payment.getPurpose() != PaymentPurpose.REVIEW) {
            return;
        }
        var plan = planRepository.findById(payment.getReviewMealPlanId())
                .orElseThrow(() -> ApiException.notFound("Planas nerastas."));
        reviews.save(new ReviewRequest(payment.getClient(), plan, payment, payment.getReviewQuestion(),
                clock.instant()));
    }

    /** Naujausia aktualaus plano peržiūra (FR-21, FR-27). */
    @Transactional(readOnly = true)
    public Optional<ClientReviewDto> currentReview(Long clientId) {
        return plans.activePlanId(clientId)
                .flatMap(reviews::findFirstByMealPlanIdOrderBySubmittedAtDesc)
                .map(this::toClientDto);
    }

    @Transactional(readOnly = true)
    public List<ClientReviewDto> clientReviews(Long clientId) {
        return reviews.findByClientIdOrderBySubmittedAtDesc(clientId).stream().map(this::toClientDto).toList();
    }

    private ClientReviewDto toClientDto(ReviewRequest r) {
        boolean completed = r.getStatus() == ReviewStatus.COMPLETED;
        return new ClientReviewDto(r.getId(), r.getMealPlan().getId(), r.getStatus(), r.getQuestion(),
                r.getSubmittedAt(), r.getReviewedAt(),
                r.getDietitian() == null ? null : r.getDietitian().getFullName(),
                completed ? r.getComments().stream().map(CommentDto::of).toList() : List.of());
    }

    // ---------- Dietologas ----------

    @Transactional(readOnly = true)
    public List<DietitianReviewSummaryDto> dietitianList(Long dietitianId, ReviewStatus status) {
        return reviews.findVisibleTo(dietitianId, status).stream()
                .map(r -> new DietitianReviewSummaryDto(r.getId(), r.getStatus(), r.getSubmittedAt(),
                        r.getQuestion(), r.isAssignedTo(dietitianId)))
                .toList();
    }

    /** Atidarius „Laukia“ užklausą, ji priskiriama dietologui ir tampa „Peržiūrima“ (FR-35). */
    public DietitianReviewDetailDto open(Long dietitianId, Long reviewId) {
        ReviewRequest review = reviews.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Užklausa nerasta."));
        if (review.getStatus() == ReviewStatus.PENDING) {
            Dietitian dietitian = users.findById(dietitianId).map(Dietitian.class::cast)
                    .orElseThrow(() -> ApiException.notFound("Dietologas nerastas."));
            review.assignTo(dietitian);
        } else if (!review.isAssignedTo(dietitianId)) {
            throw ApiException.notFound("Užklausa nerasta.");
        }
        return toDetail(review);
    }

    @Transactional(readOnly = true)
    public RecipeDto recipe(Long dietitianId, Long reviewId, Long dishId) {
        ReviewRequest review = assigned(dietitianId, reviewId);
        return plans.recipeView(review.getMealPlan().getId(), dishId);
    }

    /** Komentarą galima rašyti patiekalui, kol peržiūra nepateikta (FR-36). */
    public CommentDto addDishComment(Long dietitianId, Long reviewId, ReviewDtos.CommentRequest request) {
        ReviewRequest review = editable(dietitianId, reviewId);
        if (!plans.dishBelongsToPlan(request.dishId(), review.getMealPlan().getId())) {
            throw ApiException.badRequest("Patiekalas nepriklauso šiam planui.");
        }
        Comment comment = new Comment(review, dishes.getReferenceById(request.dishId()), request.text().trim(),
                clock.instant());
        review.getComments().add(comment);
        comments.save(comment);
        return CommentDto.of(comment);
    }

    public CommentDto updateComment(Long dietitianId, Long reviewId, Long commentId, String text) {
        ReviewRequest review = editable(dietitianId, reviewId);
        Comment comment = findComment(review, commentId);
        comment.setText(text.trim());
        return CommentDto.of(comment);
    }

    public void deleteComment(Long dietitianId, Long reviewId, Long commentId) {
        ReviewRequest review = editable(dietitianId, reviewId);
        review.getComments().remove(findComment(review, commentId));
    }

    /**
     * Pateikia peržiūrą su bendru komentaru: būsena „Atlikta“, komentarų redaguoti nebegalima,
     * klientui sukuriamas pranešimas (FR-37).
     */
    public DietitianReviewDetailDto submit(Long dietitianId, Long reviewId, String generalComment) {
        ReviewRequest review = editable(dietitianId, reviewId);
        review.generalComment().ifPresentOrElse(c -> c.setText(generalComment.trim()),
                () -> review.getComments().add(new Comment(review, null, generalComment.trim(), clock.instant())));
        review.complete(clock.instant());
        notifications.notify(review.getClient(), "Dietologo peržiūra baigta (" + review.getDietitian().getFullName()
                + "). Komentarai pateikti plano lange.");
        return toDetail(review);
    }

    private DietitianReviewDetailDto toDetail(ReviewRequest review) {
        Long clientId = review.getClient().getId();
        return new DietitianReviewDetailDto(review.getId(), review.getStatus(), review.getSubmittedAt(),
                review.getReviewedAt(), review.getQuestion(), questionnaire.anonymized(clientId).orElse(null),
                plans.planView(review.getMealPlan().getId()),
                review.getComments().stream().map(CommentDto::of).toList());
    }

    private ReviewRequest assigned(Long dietitianId, Long reviewId) {
        ReviewRequest review = reviews.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Užklausa nerasta."));
        if (!review.isAssignedTo(dietitianId)) {
            throw ApiException.notFound("Užklausa nerasta.");
        }
        return review;
    }

    private ReviewRequest editable(Long dietitianId, Long reviewId) {
        ReviewRequest review = assigned(dietitianId, reviewId);
        if (review.getStatus() != ReviewStatus.IN_REVIEW) {
            throw ApiException.conflict("Peržiūra jau pateikta, komentarų redaguoti nebegalima.");
        }
        return review;
    }

    private static Comment findComment(ReviewRequest review, Long commentId) {
        return review.getComments().stream().filter(c -> c.getId().equals(commentId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Komentaras nerastas."));
    }
}
