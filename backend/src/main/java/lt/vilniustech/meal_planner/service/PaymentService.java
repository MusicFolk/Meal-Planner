package lt.vilniustech.meal_planner.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Payment;
import lt.vilniustech.meal_planner.enums.PaymentMethod;
import lt.vilniustech.meal_planner.enums.PaymentPurpose;
import lt.vilniustech.meal_planner.enums.PaymentStatus;
import lt.vilniustech.meal_planner.event.PaymentCompletedEvent;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentService implements IPayments, IBankConfirmation {

    private final PaymentRepository payments;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final BigDecimal reviewPrice;

    public PaymentService(PaymentRepository payments, ApplicationEventPublisher events, Clock clock,
                          @Value("${app.prices.review}") BigDecimal reviewPrice) {
        this.payments = payments;
        this.events = events;
        this.clock = clock;
        this.reviewPrice = reviewPrice;
    }

    @Override
    public BigDecimal reviewPrice() {
        return reviewPrice;
    }

    /** Sukuria mokėjimo įrašą su būsena „Laukiama“ (FR-24). */
    @Override
    public Payment initiateReviewPayment(Client client, PaymentMethod method, Long mealPlanId, String question) {
        Payment payment = new Payment(client, reviewPrice, PaymentPurpose.REVIEW, method, clock.instant());
        payment.setReviewDetails(mealPlanId, question);
        return payments.save(payment);
    }

    /**
     * Priima banko atsakymą. Sėkmingas mokėjimas pažymimas „Apmokėta“ ir paskelbiamas įvykis (FR-25),
     * nesėkmingas – „Nepavyko“, kiti duomenys nekeičiami (FR-26, NFR-26).
     */
    @Override
    public Payment confirm(UUID reference, boolean successful) {
        Payment payment = payments.findByReference(reference)
                .orElseThrow(() -> ApiException.notFound("Mokėjimas nerastas."));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.conflict("Mokėjimas jau apdorotas.");
        }
        if (!successful) {
            payment.setStatus(PaymentStatus.FAILED);
            return payment;
        }
        payment.setStatus(PaymentStatus.PAID);
        events.publishEvent(new PaymentCompletedEvent(payment));
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment findOwned(UUID reference, Long clientId) {
        Payment payment = payments.findByReference(reference)
                .orElseThrow(() -> ApiException.notFound("Mokėjimas nerastas."));
        if (payment.getClient() == null || !payment.getClient().getId().equals(clientId)) {
            throw ApiException.notFound("Mokėjimas nerastas.");
        }
        return payment;
    }
}
