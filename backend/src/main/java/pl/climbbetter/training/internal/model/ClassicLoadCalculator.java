package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

final class ClassicLoadCalculator {

    private static final int CALCULATION_SCALE = 10;
    private static final int RESULT_SCALE = 4;

    BigDecimal calculate(
            BigDecimal gradePoints,
            BigDecimal styleMultiplier,
            MoveCount moveCount
    ) {
        Objects.requireNonNull(gradePoints, "Grade points are required");
        Objects.requireNonNull(styleMultiplier, "Style multiplier is required");
        Objects.requireNonNull(moveCount, "Move count is required");

        if (gradePoints.signum() < 0) {
            throw new IllegalArgumentException(
                    "Grade points cannot be negative"
            );
        }

        if (styleMultiplier.signum() < 0) {
            throw new IllegalArgumentException(
                    "Style multiplier cannot be negative"
            );
        }

        BigDecimal executedMoveRatio = BigDecimal
                .valueOf(moveCount.executedMoves())
                .divide(
                        BigDecimal.valueOf(moveCount.totalMoves()),
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );

        return gradePoints
                .multiply(styleMultiplier)
                .multiply(executedMoveRatio)
                .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }
}