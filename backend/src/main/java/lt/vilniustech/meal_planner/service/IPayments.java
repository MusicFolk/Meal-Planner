package lt.vilniustech.meal_planner.service;

import java.math.BigDecimal;
import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.entity.Payment;
import lt.vilniustech.meal_planner.enums.PaymentMethod;

/** Mokėjimų sąsaja, naudojama peržiūros mokesčiui apmokėti (MOK-4). */
public interface IPayments {

    BigDecimal reviewPrice();

    Payment initiateReviewPayment(Client client, PaymentMethod method, Long mealPlanId, String question);
}
