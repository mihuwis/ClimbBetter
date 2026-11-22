package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/** Uses the existing relative-effort calibration between adjacent grade differences. */
public final class RelativeEffortInterpolator {
    private static final BigDecimal MIN_DIFFERENCE = BigDecimal.valueOf(-6);
    private static final BigDecimal MAX_DIFFERENCE = BigDecimal.valueOf(5);
    // Match the stored multiplier before calculating load, so snapshots reproduce the result.
    private static final int CALCULATION_SCALE = 4;

    private RelativeEffortInterpolator() {
    }

    public static Result interpolate(
            int gradeIndex,
            BigDecimal currentLevelIndex,
            Map<Integer, BigDecimal> multipliers
    ) {
        Objects.requireNonNull(currentLevelIndex, "Current level index is required");
        Objects.requireNonNull(multipliers, "Relative effort calibration is required");
        if (gradeIndex < 0 || currentLevelIndex.signum() < 0) {
            throw new IllegalArgumentException("Grade indexes cannot be negative");
        }

        BigDecimal difference = BigDecimal.valueOf(gradeIndex).subtract(currentLevelIndex);
        boolean outside = difference.compareTo(MIN_DIFFERENCE) < 0
                || difference.compareTo(MAX_DIFFERENCE) > 0;
        BigDecimal calibratedDifference = difference.max(MIN_DIFFERENCE).min(MAX_DIFFERENCE);
        int lower = calibratedDifference.setScale(0, RoundingMode.FLOOR).intValueExact();
        int upper = calibratedDifference.setScale(0, RoundingMode.CEILING).intValueExact();
        BigDecimal lowerMultiplier = requiredMultiplier(multipliers, lower);
        if (lower == upper) {
            return new Result(difference, lowerMultiplier, outside);
        }

        BigDecimal upperMultiplier = requiredMultiplier(multipliers, upper);
        BigDecimal fraction = calibratedDifference.subtract(BigDecimal.valueOf(lower));
        BigDecimal multiplier = lowerMultiplier
                .add(upperMultiplier.subtract(lowerMultiplier).multiply(fraction))
                .setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
        return new Result(difference, multiplier, outside);
    }

    private static BigDecimal requiredMultiplier(Map<Integer, BigDecimal> multipliers, int difference) {
        BigDecimal multiplier = multipliers.get(difference);
        if (multiplier == null) {
            throw new IllegalArgumentException("No active relative effort rule for difference " + difference);
        }
        if (multiplier.signum() <= 0) {
            throw new IllegalArgumentException("Relative effort multipliers must be positive");
        }
        return multiplier;
    }

    public record Result(
            BigDecimal relativeGradeDifference,
            BigDecimal multiplier,
            boolean outsideCalibratedRange
    ) {
    }
}
