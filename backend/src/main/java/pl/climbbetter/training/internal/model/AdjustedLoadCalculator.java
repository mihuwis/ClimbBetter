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

        BigDecimal moveWork = BigDecimal
                .valueOf(moveCount.executedMoves())
                .multiply(moveIntensity);

        return calculateFromWork(
                moveWork,
                styleMultiplier,
                relativeEffortMultiplier
        );
    }

    BigDecimal calculateFromWork(
            BigDecimal moveWork,
            BigDecimal styleMultiplier,
            BigDecimal relativeEffortMultiplier
    ) {
        Objects.requireNonNull(moveWork, "Move work is required");
        Objects.requireNonNull(styleMultiplier, "Style multiplier is required");
        Objects.requireNonNull(
                relativeEffortMultiplier,
                "Relative effort multiplier is required"
        );

        requireNonNegative(moveWork, "Move work");
        requireNonNegative(styleMultiplier, "Style multiplier");
        requireNonNegative(
                relativeEffortMultiplier,
                "Relative effort multiplier"
        );

        return moveWork
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
