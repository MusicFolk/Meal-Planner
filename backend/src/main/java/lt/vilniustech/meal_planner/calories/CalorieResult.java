package lt.vilniustech.meal_planner.calories;

/**
 * Paros kalorijų poreikio skaičiavimo rezultatas su tarpinėmis reikšmėmis paaiškinimui (FR-05, FR-06).
 *
 * @param bmr                bazinė medžiagų apykaita (kcal), neapvalinta
 * @param activityFactor     aktyvumo koeficientas
 * @param afterActivity      BMR × aktyvumo koeficientas, neapvalinta
 * @param goalFactor         tikslo koeficientas
 * @param calculatedCalories BMR × aktyvumo × tikslo koeficientas, suapvalinta iki 1 kcal
 * @param safeMinimum        saugi paros riba pagal lytį
 * @param dailyCalories      plano sudarymui naudojamas paros poreikis (ne mažesnis už saugią ribą)
 * @param safeLimitApplied   ar pritaikyta saugi riba
 */
public record CalorieResult(
        double bmr,
        double activityFactor,
        double afterActivity,
        double goalFactor,
        int calculatedCalories,
        int safeMinimum,
        int dailyCalories,
        boolean safeLimitApplied) {
}
