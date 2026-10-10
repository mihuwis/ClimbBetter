package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

final class AdjustedLoadCalculator {

    private static final int RESULT_SCALE = 4;

    BigDecimal calculate(
            BigDecimal moveIntensity,
            BigDecimal styleMultiplier,
            BigDecimal relativeEffortMultiplier,
            MoveCount moveCount
    ) {
        Objects.requireNonNull(
                moveIntensity,
                "Move intensity is required"
        );
        Objects.requireNonNull(
                styleMultiplier,
                "Style multiplier is required"
        );
        Objects.requireNonNull(
                relativeEffortMultiplier,
                "Relative effort multiplier is required"
        );
        Objects.requireNonNull(moveCount, "Move count is required");

        requireNonNegative(moveIntensity, "Move intensity");
        requireNonNegative(styleMultiplier, "Style multiplier");
        requireNonNegative(
                relativeEffortMultiplier,
                "Relative effort multiplier"
        );

        return BigDecimal
                .valueOf(moveCount.executedMoves())
                .multiply(moveIntensity)
                .multiply(styleMultiplier)
                .multiply(relativeEffortMultiplier)
                .setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private void requireNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative"
            );
        }
    }
}