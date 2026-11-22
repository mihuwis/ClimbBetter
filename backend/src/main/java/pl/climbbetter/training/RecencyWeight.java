package pl.climbbetter.training;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A compact quadratic weight for the age of an observation, never its grade. */
public final class RecencyWeight {

    private static final int SCALE = 10;

    private RecencyWeight() {
    }

    public static BigDecimal weight(long ageDays, int horizonDays) {
        if (ageDays < 0) {
            throw new IllegalArgumentException("Observation age cannot be negative");
        }
        if (horizonDays <= 0) {
            throw new IllegalArgumentException("Recency horizon must be positive");
        }
        if (ageDays >= horizonDays) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        BigDecimal horizon = BigDecimal.valueOf(horizonDays);
        BigDecimal remaining = horizon.subtract(BigDecimal.valueOf(ageDays));
        return remaining.multiply(remaining)
                .divide(horizon.multiply(horizon), SCALE, RoundingMode.HALF_UP);
    }
}
