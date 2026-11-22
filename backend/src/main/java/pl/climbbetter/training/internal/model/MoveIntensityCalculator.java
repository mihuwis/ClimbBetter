package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

final class MoveIntensityCalculator {

    private static final int CALCULATION_SCALE = 10;

    BigDecimal calculateFromProfile(
                BigDecimal gradePoints,
                BigDecimal profileIntensityIndex
        ) {
        Objects.requireNonNull(
                gradePoints,
                "Grade points are required"
        );
        Objects.requireNonNull(
                profileIntensityIndex,
                "Profile intensity index is required"
        );

        if (gradePoints.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Grade points must be positive"
                );
        }

        if (profileIntensityIndex.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Profile intensity index must be positive"
                );
        }

        return gradePoints
                .multiply(profileIntensityIndex)
                .divide(
                        new BigDecimal("100"),
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );
        }
}
