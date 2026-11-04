package lt.vilniustech.meal_planner.enums;

/** Vieno DI kvietimo patikros rezultatas (FR-11–FR-14). */
public enum AiRequestResult {
    VALID,
    INVALID_SCHEMA,
    INVALID_PRODUCT,
    PORTION_OUT_OF_RANGE,
    TIMEOUT,
    API_ERROR
}
