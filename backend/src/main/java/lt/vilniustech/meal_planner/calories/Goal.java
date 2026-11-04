package lt.vilniustech.meal_planner.calories;

/** Mitybos tikslas ir jo koeficientas (FR-05). */
public enum Goal {
    LOSE_WEIGHT(0.85),
    MAINTAIN(1.00),
    GAIN_MUSCLE(1.10);

    private final double factor;

    Goal(double factor) {
        this.factor = factor;
    }

    public double factor() {
        return factor;
    }
}
