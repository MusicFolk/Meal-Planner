package lt.vilniustech.meal_planner.calories;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Kalorijų skaičiavimo modulis (KAL). Atskiras modulis be priklausomybių nuo kitų sistemos
 * komponentų ir DB (NFR-48), todėl jį galima naudoti ir kitose sistemose.
 */
public class CalorieCalculator implements ICalorieCalculator {

    private static final BigDecimal UNDERWEIGHT_LIMIT = new BigDecimal("18.5");
    private static final BigDecimal OVERWEIGHT_LIMIT = new BigDecimal("25.0");
    private static final BigDecimal OBESE_LIMIT = new BigDecimal("30.0");

    private static final double BREAKFAST_SHARE = 0.25;
    private static final double LUNCH_SHARE = 0.40;

    private static final double MIN_PORTION_FACTOR = 0.7;
    private static final double MAX_PORTION_FACTOR = 1.3;
    private static final BigDecimal MAX_DEVIATION_PERCENT = BigDecimal.TEN;

    /** KMI = m / h², suapvalintas iki 0,1 (FR-04). */
    @Override
    public BigDecimal bmi(int heightCm, BigDecimal weightKg) {
        BigDecimal heightM = BigDecimal.valueOf(heightCm).divide(BigDecimal.valueOf(100));
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    @Override
    public BmiCategory bmiCategory(BigDecimal bmi) {
        if (bmi.compareTo(UNDERWEIGHT_LIMIT) < 0) {
            return BmiCategory.UNDERWEIGHT;
        }
        if (bmi.compareTo(OVERWEIGHT_LIMIT) < 0) {
            return BmiCategory.NORMAL;
        }
        if (bmi.compareTo(OBESE_LIMIT) < 0) {
            return BmiCategory.OVERWEIGHT;
        }
        return BmiCategory.OBESE;
    }

    /**
     * Paros poreikis pagal Mifflin–St Jeor formulę: (10m + 6,25h − 5a + s) × k × tikslo koeficientas.
     * Tarpinės reikšmės neapvalinamos, galutinis rezultatas apvalinamas iki 1 kcal (NFR-21).
     */
    @Override
    public CalorieResult dailyCalories(int heightCm, BigDecimal weightKg, int age, Gender gender,
                                       ActivityLevel activityLevel, Goal goal) {
        double bmr = 10 * weightKg.doubleValue() + 6.25 * heightCm - 5 * age + gender.bmrConstant();
        double afterActivity = bmr * activityLevel.factor();
        int calculated = (int) Math.round(afterActivity * goal.factor());
        int safeMinimum = gender.safeMinimumCalories();
        boolean safeLimitApplied = calculated < safeMinimum;
        int daily = safeLimitApplied ? safeMinimum : calculated;
        return new CalorieResult(bmr, activityLevel.factor(), afterActivity, goal.factor(), calculated,
                safeMinimum, daily, safeLimitApplied);
    }

    /** Pusryčiams 25 %, pietums 40 %, vakarienei likusi dalis (35 %), kad suma sutaptų su paros poreikiu. */
    @Override
    public MealTargets mealTargets(int dailyCalories) {
        int breakfast = (int) Math.round(dailyCalories * BREAKFAST_SHARE);
        int lunch = (int) Math.round(dailyCalories * LUNCH_SHARE);
        return new MealTargets(breakfast, lunch, dailyCalories - breakfast - lunch);
    }

    /** Porcijos koeficientas = valgymo tikslas / patiekalo kaloringumas (FR-13). */
    @Override
    public double portionFactor(int targetCalories, int dishCalories) {
        if (dishCalories <= 0) {
            throw new IllegalArgumentException("Patiekalo kaloringumas turi būti teigiamas");
        }
        return (double) targetCalories / dishCalories;
    }

    /** Porcijų koregavimo riba: koeficientas nuo 0,7 iki 1,3 (FR-13, KAL-4). */
    @Override
    public boolean portionFactorAllowed(double factor) {
        return factor >= MIN_PORTION_FACTOR && factor <= MAX_PORTION_FACTOR;
    }

    /** Nuokrypis = (dienos kaloringumas − paros poreikis) / paros poreikis × 100 % (FR-13). */
    @Override
    public BigDecimal dayDeviationPercent(int dayCalories, int dailyCalories) {
        if (dailyCalories <= 0) {
            throw new IllegalArgumentException("Paros poreikis turi būti teigiamas");
        }
        return BigDecimal.valueOf(dayCalories - dailyCalories).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(dailyCalories), 1, RoundingMode.HALF_UP);
    }

    @Override
    public boolean deviationAllowed(BigDecimal deviationPercent) {
        return deviationPercent.abs().compareTo(MAX_DEVIATION_PERCENT) <= 0;
    }
}
