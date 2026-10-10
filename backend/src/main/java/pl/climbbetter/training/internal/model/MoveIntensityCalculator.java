package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

final class MoveIntensityCalculator {

    private static final int CALCULATION_SCALE = 10;

    BigDecimal calculate(
            BigDecimal gradePoints,
            BigDecimal edlCount
    ) {
        Objects.requireNonNull(
                gradePoints,
                "Grade points are required"
        );
        Objects.requireNonNull(
                edlCount,
                "EDL count is required"
        );

        if (gradePoints.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Grade points must be positive"
            );
        }

        if (edlCount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "EDL count must be positive"
            );
        }

        return gradePoints.divide(
                edlCount,
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
        );
    }
}