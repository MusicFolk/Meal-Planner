package lt.vilniustech.meal_planner.service;

import lt.vilniustech.meal_planner.entity.Client;
import lt.vilniustech.meal_planner.enums.SubscriptionStatus;

/** Prenumeratos sąsaja: ar klientui leidžiama generuoti planą (MOK-4). */
public interface ISubscription {

    void startTrial(Client client);

    SubscriptionStatus status(Long clientId);

    boolean canGeneratePlans(Long clientId);
}
