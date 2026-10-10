package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

final class EdlCalculator {

    private static final BigDecimal TOTAL_MOVES_FACTOR =
            new BigDecimal("0.2");

    private static final int CALCULATION_SCALE = 10;

    BigDecimal calculate(
            BigDecimal baseEdl,
            MoveCount moveCount
    ) {
        Objects.requireNonNull(baseEdl, "Base EDL is required");
        Objects.requireNonNull(moveCount, "Move count is required");

        if (baseEdl.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Base EDL must be positive"
            );
        }

        return baseEdl
                .add(
                        BigDecimal
                                .valueOf(moveCount.totalMoves())
                                .multiply(TOTAL_MOVES_FACTOR)
                )
                .setScale(
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );
    }
}