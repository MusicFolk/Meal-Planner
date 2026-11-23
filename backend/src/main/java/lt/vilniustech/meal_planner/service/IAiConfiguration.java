package lt.vilniustech.meal_planner.service;

/** Galiojantys DI parametrai kitiems komponentams: DI adapteriui (DIA-4) ir mitybos plano komponentui (FR-08). */
public interface IAiConfiguration {

    String model();

    int dailyGenerationLimit();
}
