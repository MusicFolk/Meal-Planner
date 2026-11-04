package lt.vilniustech.meal_planner.calories;

/** Lytis ir jos konstanta Mifflin–St Jeor formulėje. */
public enum Gender {
    MALE(5, 1500),
    FEMALE(-161, 1200);

    private final int bmrConstant;
    private final int safeMinimumCalories;

    Gender(int bmrConstant, int safeMinimumCalories) {
        this.bmrConstant = bmrConstant;
        this.safeMinimumCalories = safeMinimumCalories;
    }

    public int bmrConstant() {
        return bmrConstant;
    }

    /** Saugi paros kalorijų riba (FR-06). */
    public int safeMinimumCalories() {
        return safeMinimumCalories;
    }
}
