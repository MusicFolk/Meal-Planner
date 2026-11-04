package lt.vilniustech.meal_planner.calories;

import java.math.BigDecimal;

/** Kalorijų skaičiavimo komponento (KAL) sąsaja. */
public interface ICalorieCalculator {

    BigDecimal bmi(int heightCm, BigDecimal weightKg);

    BmiCategory bmiCategory(BigDecimal bmi);

    CalorieResult dailyCalories(int heightCm, BigDecimal weightKg, int age, Gender gender,
                                ActivityLevel activityLevel, Goal goal);

    MealTargets mealTargets(int dailyCalories);

    double portionFactor(int targetCalories, int dishCalories);

    /** Ar porcijos koeficientas leistinose ribose (0,7–1,3). */
    boolean portionFactorAllowed(double factor);

    /** Dienos kaloringumo nuokrypis nuo paros poreikio procentais, 0,1 tikslumu. */
    BigDecimal dayDeviationPercent(int dayCalories, int dailyCalories);

    /** Ar dienos nuokrypis neviršija ±10 %. */
    boolean deviationAllowed(BigDecimal deviationPercent);
}
