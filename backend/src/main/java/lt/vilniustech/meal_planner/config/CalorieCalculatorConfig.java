package lt.vilniustech.meal_planner.config;

import lt.vilniustech.meal_planner.calories.CalorieCalculator;
import lt.vilniustech.meal_planner.calories.ICalorieCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Kalorijų skaičiavimo modulis neturi Spring priklausomybių, todėl jis registruojamas čia. */
@Configuration
public class CalorieCalculatorConfig {

    @Bean
    public ICalorieCalculator calorieCalculator() {
        return new CalorieCalculator();
    }
}
