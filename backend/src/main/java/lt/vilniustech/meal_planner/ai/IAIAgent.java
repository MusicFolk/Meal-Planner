package lt.vilniustech.meal_planner.ai;

import java.time.Duration;

/**
 * DI adapterio (DIA) «DI agentas» sąsaja IDIAgentas. Gauna vieno valgymo tipo užklausą ir grąžina
 * JSON schemą atitinkančius patiekalus. DI modelį galima pakeisti keičiant tik konfigūraciją (NFR-45).
 */
public interface IAIAgent {

    /** Naudojamas DI modelis (DIA-4). */
    String modelName();

    /**
     * Sugeneruoja vieno valgymo tipo patiekalus (DIA-1–DIA-3).
     *
     * @param timeout ilgiausias laukimo laikas (ne daugiau kaip 25 s, NFR-28)
     */
    AiMealResponse generateMeals(AiMealRequest request, Duration timeout);
}
