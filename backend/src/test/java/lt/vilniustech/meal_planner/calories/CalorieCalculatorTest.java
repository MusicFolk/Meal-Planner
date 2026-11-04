package lt.vilniustech.meal_planner.calories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CalorieCalculatorTest {

    private final CalorieCalculator calculator = new CalorieCalculator();

    @Test
    void bmiIsRoundedToOneDecimal() {
        assertThat(calculator.bmi(180, new BigDecimal("80.0"))).isEqualByComparingTo("24.7");
        assertThat(calculator.bmi(165, new BigDecimal("62.5"))).isEqualByComparingTo("23.0");
    }

    @ParameterizedTest
    @CsvSource({
            "18.4, UNDERWEIGHT",
            "18.5, NORMAL",
            "24.9, NORMAL",
            "25.0, OVERWEIGHT",
            "29.9, OVERWEIGHT",
            "30.0, OBESE"
    })
    void bmiCategoryBoundaries(String bmi, BmiCategory expected) {
        assertThat(calculator.bmiCategory(new BigDecimal(bmi))).isEqualTo(expected);
    }

    /** Rankinis skaičiavimas: BMR = 10*80 + 6,25*180 - 5*30 + 5 = 1780; 1780 * 1,55 * 0,85 = 2345,15. */
    @Test
    void dailyCaloriesMatchManualCalculationForMan() {
        CalorieResult result = calculator.dailyCalories(180, new BigDecimal("80.0"), 30, Gender.MALE,
                ActivityLevel.MODERATE, Goal.LOSE_WEIGHT);

        assertThat(result.bmr()).isEqualTo(1780.0);
        assertThat(result.afterActivity()).isCloseTo(2759.0, offset(1e-9));
        assertThat(result.calculatedCalories()).isEqualTo(2345);
        assertThat(result.dailyCalories()).isEqualTo(2345);
        assertThat(result.safeLimitApplied()).isFalse();
    }

    /** Rankinis skaičiavimas: BMR = 625 + 1031,25 - 135 - 161 = 1360,25; * 1,375 = 1870,34. */
    @Test
    void dailyCaloriesMatchManualCalculationForWoman() {
        CalorieResult result = calculator.dailyCalories(165, new BigDecimal("62.5"), 27, Gender.FEMALE,
                ActivityLevel.LIGHT, Goal.MAINTAIN);

        assertThat(result.bmr()).isEqualTo(1360.25);
        assertThat(result.dailyCalories()).isEqualTo(1870);
    }

    @Test
    void gainMuscleGoalAddsTenPercent() {
        CalorieResult result = calculator.dailyCalories(180, new BigDecimal("80.0"), 30, Gender.MALE,
                ActivityLevel.VERY_ACTIVE, Goal.GAIN_MUSCLE);

        // 1780 * 1,9 * 1,1 = 3720,2
        assertThat(result.dailyCalories()).isEqualTo(3720);
    }

    @Test
    void safeMinimumIsAppliedForWoman() {
        CalorieResult result = calculator.dailyCalories(150, new BigDecimal("45.0"), 70, Gender.FEMALE,
                ActivityLevel.SEDENTARY, Goal.LOSE_WEIGHT);

        assertThat(result.calculatedCalories()).isEqualTo(894);
        assertThat(result.dailyCalories()).isEqualTo(1200);
        assertThat(result.safeLimitApplied()).isTrue();
    }

    @Test
    void safeMinimumIsAppliedForMan() {
        CalorieResult result = calculator.dailyCalories(160, new BigDecimal("50.0"), 80, Gender.MALE,
                ActivityLevel.SEDENTARY, Goal.LOSE_WEIGHT);

        assertThat(result.dailyCalories()).isEqualTo(1500);
        assertThat(result.safeLimitApplied()).isTrue();
    }

    @Test
    void mealTargetsSplitDailyCaloriesAndSumUp() {
        MealTargets targets = calculator.mealTargets(2345);

        assertThat(targets.breakfast()).isEqualTo(586);
        assertThat(targets.lunch()).isEqualTo(938);
        assertThat(targets.dinner()).isEqualTo(821);
        assertThat(targets.breakfast() + targets.lunch() + targets.dinner()).isEqualTo(2345);
    }

    @Test
    void portionFactorIsTargetDividedByDishCalories() {
        assertThat(calculator.portionFactor(600, 500)).isEqualTo(1.2);
        assertThatThrownBy(() -> calculator.portionFactor(600, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void portionFactorBoundsAreInclusive() {
        assertThat(calculator.portionFactorAllowed(0.7)).isTrue();
        assertThat(calculator.portionFactorAllowed(1.3)).isTrue();
        assertThat(calculator.portionFactorAllowed(0.69)).isFalse();
        assertThat(calculator.portionFactorAllowed(1.31)).isFalse();
    }

    @Test
    void dayDeviationIsPercentOfDailyCalories() {
        assertThat(calculator.dayDeviationPercent(2200, 2000)).isEqualByComparingTo("10.0");
        assertThat(calculator.dayDeviationPercent(1849, 2000)).isEqualByComparingTo("-7.6");
        assertThat(calculator.deviationAllowed(new BigDecimal("10.0"))).isTrue();
        assertThat(calculator.deviationAllowed(new BigDecimal("-10.1"))).isFalse();
    }
}
