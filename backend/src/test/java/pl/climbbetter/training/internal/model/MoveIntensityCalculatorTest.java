package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoveIntensityCalculatorTest {

    private final MoveIntensityCalculator calculator =
            new MoveIntensityCalculator();

    @Test
    void calculatesMoveIntensity() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("240"),
                new BigDecimal("48")
        );

        assertEquals(
                new BigDecimal("5.0000000000"),
                result
        );
    }
}