package lt.vilniustech.meal_planner.controller;

import java.util.UUID;
import lt.vilniustech.meal_planner.dto.PaymentDtos.BankDecision;
import lt.vilniustech.meal_planner.dto.PaymentDtos.PaymentDto;
import lt.vilniustech.meal_planner.security.AppUserDetails;
import lt.vilniustech.meal_planner.service.IBankConfirmation;
import lt.vilniustech.meal_planner.service.PaymentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Banko mokėjimų sąsajos imitacijos atsakymas programų serveriui (NFR-09). Imitacija veikia toje pačioje
 * naršyklėje, todėl patvirtinti galima tik prisijungusio kliento mokėjimą.
 */
@RestController
public class BankSimulatorController {

    private final IBankConfirmation confirmation;
    private final PaymentService payments;

    public BankSimulatorController(IBankConfirmation confirmation, PaymentService payments) {
        this.confirmation = confirmation;
        this.payments = payments;
    }

    @PostMapping("/api/bank/payments/{reference}/confirm")
    public PaymentDto confirm(@AuthenticationPrincipal AppUserDetails user, @PathVariable UUID reference,
                              @RequestBody BankDecision decision) {
        payments.findOwned(reference, user.getId());
        return PaymentDto.of(confirmation.confirm(reference, decision.approved()));
    }
}
