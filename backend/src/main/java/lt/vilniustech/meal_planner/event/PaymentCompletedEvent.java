package lt.vilniustech.meal_planner.event;

import lt.vilniustech.meal_planner.entity.Payment;

/** Įvykis, paskelbiamas bankui patvirtinus sėkmingą mokėjimą. Apdorojamas toje pačioje transakcijoje. */
public record PaymentCompletedEvent(Payment payment) {
}
