package lt.vilniustech.meal_planner.service;

import lt.vilniustech.meal_planner.entity.Payment;
import org.springframework.stereotype.Component;

/** Banko mokėjimų sąsajos imitacija: klientas nukreipiamas į atskirą langą su veiksmais „Apmokėti“ ir „Atmesti“. */
@Component
public class BankSimulator implements IBank {

    @Override
    public String initiate(Payment payment) {
        return "/bankas/" + payment.getReference();
    }
}
