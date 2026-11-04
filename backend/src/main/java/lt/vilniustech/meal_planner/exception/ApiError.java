package lt.vilniustech.meal_planner.exception;

import java.util.Map;

/** Klaidos atsakymas: bendras pranešimas ir klaidos prie formos laukų. */
public record ApiError(String message, Map<String, String> fieldErrors) {

    public static ApiError of(String message) {
        return new ApiError(message, Map.of());
    }
}
