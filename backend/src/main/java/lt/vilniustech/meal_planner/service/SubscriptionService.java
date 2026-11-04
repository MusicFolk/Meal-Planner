package lt.vilniustech.meal_planner.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lt.vilniustech.meal_planner.dto.PaymentDtos.PaymentInitiated;
import lt.vilniustech.meal_planner.dto.PaymentDtos.SubscriptionDto;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Payment;
import lt.vilniustech.meal_planner.entity.Subscription;
import lt.vilniustech.meal_planner.enums.PaymentMethod;
import lt.vilniustech.meal_planner.enums.PaymentPurpose;
import lt.vilniustech.meal_planner.enums.SubscriptionStatus;
import lt.vilniustech.meal_planner.event.PaymentCompletedEvent;
import lt.vilniustech.meal_planner.exception.ApiException;
import lt.vilniustech.meal_planner.repository.PaymentRepository;
import lt.vilniustech.meal_planner.repository.SubscriptionRepository;
import lt.vilniustech.meal_planner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriptionService implements ISubscription {

    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final IBank bank;
    private final NotificationService notifications;
    private final Clock clock;
    private final BigDecimal price;
    private final Duration trialDuration;
    private final Duration paidDuration;

    public SubscriptionService(SubscriptionRepository subscriptions, PaymentRepository payments, UserRepository users,
                               IBank bank, NotificationService notifications, Clock clock,
                               @Value("${app.prices.subscription}") BigDecimal price,
                               @Value("${app.subscription.trial-days}") int trialDays,
                               @Value("${app.subscription.paid-days}") int paidDays) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.users = users;
        this.bank = bank;
        this.notifications = notifications;
        this.clock = clock;
        this.price = price;
        this.trialDuration = Duration.ofDays(trialDays);
        this.paidDuration = Duration.ofDays(paidDays);
    }

    /** Naujam klientui automatiškai suteikiamas 7 dienų bandomasis laikotarpis (FR-28). */
    @Override
    public void startTrial(Client client) {
        subscriptions.save(new Subscription(client, clock.instant().plus(trialDuration)));
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatus status(Long clientId) {
        return find(clientId).status(clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canGeneratePlans(Long clientId) {
        return status(clientId) != SubscriptionStatus.EXPIRED;
    }

    @Transactional(readOnly = true)
    public SubscriptionDto details(Long clientId) {
        Instant now = clock.instant();
        Subscription s = find(clientId);
        SubscriptionStatus status = s.status(now);
        return new SubscriptionDto(status, s.validUntil(now), s.getTrialEndsAt(), s.getPaidUntil(), price,
                status != SubscriptionStatus.ACTIVE);
    }

    /** Prenumeratą galima apmokėti, kai jos būsena „Bandomasis laikotarpis“ arba „Pasibaigusi“ (FR-43). */
    public PaymentInitiated initiatePayment(Long clientId, PaymentMethod method) {
        if (status(clientId) == SubscriptionStatus.ACTIVE) {
            throw ApiException.conflict("Prenumerata jau aktyvi.");
        }
        Client client = users.findById(clientId).map(Client.class::cast)
                .orElseThrow(() -> ApiException.notFound("Klientas nerastas."));
        Payment payment = payments.save(new Payment(client, price, PaymentPurpose.SUBSCRIPTION, method,
                clock.instant()));
        return new PaymentInitiated(payment.getReference(), bank.initiate(payment));
    }

    /** Sėkmingai apmokėjus, prenumerata tampa aktyvi 30 dienų nuo apmokėjimo dienos (FR-43). */
    @EventListener
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        Payment payment = event.payment();
        if (payment.getPurpose() != PaymentPurpose.SUBSCRIPTION) {
            return;
        }
        Subscription subscription = find(payment.getClient().getId());
        subscription.setPaidUntil(clock.instant().plus(paidDuration));
        notifications.notify(payment.getClient(), "Prenumerata aktyvuota 30 dienų.");
    }

    private Subscription find(Long clientId) {
        return subscriptions.findByClientId(clientId)
                .orElseThrow(() -> ApiException.notFound("Prenumerata nerasta."));
    }
}
