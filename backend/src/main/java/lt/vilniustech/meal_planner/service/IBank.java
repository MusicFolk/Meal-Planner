package lt.vilniustech.meal_planner.service;

import lt.vilniustech.meal_planner.entity.Payment;

/** Banko mokėjimų sąsaja. Prototipe – imitacija (NFR-09); tikroje sistemoje – mokėjimų tiekėjo sąsaja. */
public interface IBank {

    /** Inicijuoja mokėjimą ir grąžina adresą, į kurį nukreipiamas klientas. */
    String initiate(Payment payment);
}
