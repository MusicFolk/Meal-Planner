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
}
