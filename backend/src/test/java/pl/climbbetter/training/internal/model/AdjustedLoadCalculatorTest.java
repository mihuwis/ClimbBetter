package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdjustedLoadCalculatorTest {

    private final AdjustedLoadCalculator calculator =
            new AdjustedLoadCalculator();

    @Test
    void includesAllExecutedMoves() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("5.00"),
                new BigDecimal("1.20"),
                new BigDecimal("1.50"),
                new MoveCount(12, 20)
        );

        assertEquals(new BigDecimal("180.0000"), result);
    }

    @Test
    void returnsZeroForZeroStyleMultiplier() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                new BigDecimal("1.50"),
                new MoveCount(12, 20)
        );

        assertEquals(new BigDecimal("0.0000"), result);
    }
}