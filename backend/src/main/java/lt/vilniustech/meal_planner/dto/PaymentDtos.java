package lt.vilniustech.meal_planner.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lt.vilniustech.meal_planner.entity.Payment;
import lt.vilniustech.meal_planner.enums.PaymentMethod;
import lt.vilniustech.meal_planner.enums.PaymentPurpose;
import lt.vilniustech.meal_planner.enums.PaymentStatus;
import lt.vilniustech.meal_planner.enums.SubscriptionStatus;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record PayRequest(@NotNull(message = "Pasirinkite mokėjimo būdą.") PaymentMethod method) {
    }

    public record PaymentInitiated(UUID reference, String redirectUrl) {
    }

    public record PaymentDto(UUID reference, BigDecimal amount, PaymentPurpose purpose, PaymentMethod method,
                             PaymentStatus status, Instant createdAt) {

        public static PaymentDto of(Payment p) {
            return new PaymentDto(p.getReference(), p.getAmount(), p.getPurpose(), p.getMethod(), p.getStatus(),
                    p.getCreatedAt());
        }
    }

    public record BankDecision(@NotNull boolean approved) {
    }

    public record SubscriptionDto(SubscriptionStatus status, Instant validUntil, Instant trialEndsAt,
                                  Instant paidUntil, BigDecimal price, boolean canPay) {
    }
}
