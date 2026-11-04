package lt.vilniustech.meal_planner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Maistinė vertė: kcal, baltymai, riebalai ir angliavandeniai («dataType» NutritionValue). */
@Embeddable
public class NutritionValue {

    @Column(name = "kcal", nullable = false)
    private int kcal;

    @Column(name = "protein_g", nullable = false)
    private BigDecimal proteinG = BigDecimal.ZERO;

    @Column(name = "fat_g", nullable = false)
    private BigDecimal fatG = BigDecimal.ZERO;

    @Column(name = "carbs_g", nullable = false)
    private BigDecimal carbsG = BigDecimal.ZERO;

    protected NutritionValue() {
    }

    public NutritionValue(int kcal, BigDecimal proteinG, BigDecimal fatG, BigDecimal carbsG) {
        this.kcal = kcal;
        this.proteinG = proteinG;
        this.fatG = fatG;
        this.carbsG = carbsG;
    }

    public static NutritionValue zero() {
        return new NutritionValue(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** Maistinė vertė nurodytam produkto kiekiui, kai ši vertė nurodyta 100 g (NFR-22). */
    public Accumulator forGrams(int grams) {
        BigDecimal factor = BigDecimal.valueOf(grams).divide(BigDecimal.valueOf(100));
        return new Accumulator(BigDecimal.valueOf(kcal).multiply(factor), proteinG.multiply(factor),
                fatG.multiply(factor), carbsG.multiply(factor));
    }

    public int getKcal() {
        return kcal;
    }

    public BigDecimal getProteinG() {
        return proteinG;
    }

    public BigDecimal getFatG() {
        return fatG;
    }

    public BigDecimal getCarbsG() {
        return carbsG;
    }

    /** Tikslus (neapvalintas) maistinės vertės sumavimas; apvalinama tik galutinė reikšmė. */
    public record Accumulator(BigDecimal kcal, BigDecimal protein, BigDecimal fat, BigDecimal carbs) {

        public static Accumulator empty() {
            return new Accumulator(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        public Accumulator plus(Accumulator other) {
            return new Accumulator(kcal.add(other.kcal), protein.add(other.protein), fat.add(other.fat),
                    carbs.add(other.carbs));
        }

        public NutritionValue toNutritionValue() {
            return new NutritionValue(kcal.setScale(0, RoundingMode.HALF_UP).intValue(),
                    protein.setScale(1, RoundingMode.HALF_UP), fat.setScale(1, RoundingMode.HALF_UP),
                    carbs.setScale(1, RoundingMode.HALF_UP));
        }
    }
}
