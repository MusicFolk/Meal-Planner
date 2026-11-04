package lt.vilniustech.meal_planner.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lt.vilniustech.meal_planner.dto.PaymentDtos.PayRequest;
import lt.vilniustech.meal_planner.dto.PaymentDtos.PaymentDto;
import lt.vilniustech.meal_planner.dto.PaymentDtos.PaymentInitiated;
import lt.vilniustech.meal_planner.dto.PaymentDtos.SubscriptionDto;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.PaymentService;
import lt.vilniustech.meal_planner.service.SubscriptionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final SubscriptionService subscriptions;
    private final PaymentService payments;

    public PaymentController(SubscriptionService subscriptions, PaymentService payments) {
        this.subscriptions = subscriptions;
        this.payments = payments;
    }

    @GetMapping("/api/subscription")
    public SubscriptionDto subscription(@AuthenticationPrincipal AppUserDetails user) {
        return subscriptions.details(user.getId());
    }

    @PostMapping("/api/subscription/pay")
    public PaymentInitiated pay(@AuthenticationPrincipal AppUserDetails user, @Valid @RequestBody PayRequest request) {
        return subscriptions.initiatePayment(user.getId(), request.method());
    }

    @GetMapping("/api/payments/{reference}")
    public PaymentDto payment(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID reference) {
        return PaymentDto.of(payments.findOwned(reference, user.getId()));
    }
}
