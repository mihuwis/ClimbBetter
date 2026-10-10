package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassicLoadCalculatorTest {

    private final ClassicLoadCalculator calculator =
            new ClassicLoadCalculator();

    @Test
    void calculatesLoadForCompletedRouteLength() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("100.00"),
                new BigDecimal("1.20"),
                new MoveCount(12, 12)
        );

        assertEquals(new BigDecimal("120.0000"), result);
    }

    @Test
    void includesRepeatedMovesAfterFalling() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("100.00"),
                new BigDecimal("1.20"),
                new MoveCount(12, 20)
        );

        assertEquals(new BigDecimal("200.0000"), result);
    }
}