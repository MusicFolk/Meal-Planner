package lt.vilniustech.meal_planner.calories;

/** Fizinio aktyvumo lygis ir jo koeficientas (UI-07). */
public enum ActivityLevel {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9);

    private final double factor;

    ActivityLevel(double factor) {
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }
}
