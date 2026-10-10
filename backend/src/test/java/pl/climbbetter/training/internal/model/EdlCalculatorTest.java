package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EdlCalculatorTest {

    private final EdlCalculator calculator = new EdlCalculator();

    @Test
    void calculatesEdlFromBaseEdlAndRouteLength() {
        BigDecimal expected = new BigDecimal("48.0000000000");

        assertAll(
                () -> assertEquals(
                        expected,
                        calculator.calculate(
                                new BigDecimal("40"),
                                new MoveCount(40, 40)
                        )
                ),
                () -> assertEquals(
                        expected,
                        calculator.calculate(
                                new BigDecimal("40"),
                                new MoveCount(40, 80)
                        )
                )
        );
    }
}