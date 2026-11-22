package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoveIntensityCalculatorTest {

    private final MoveIntensityCalculator calculator =
            new MoveIntensityCalculator();

    @Test
void calculatesMoveIntensityFromProfile() {
    assertAll(
            () -> assertEquals(
                    new BigDecimal("9.2500000000"),
                    calculator.calculateFromProfile(
                            new BigDecimal("92.5"),
                            new BigDecimal("10")
                    )
            ),
            () -> assertEquals(
                    new BigDecimal("6.3000000000"),
                    calculator.calculateFromProfile(
                            new BigDecimal("90"),
                            new BigDecimal("7")
                    )
            ),
            () -> assertEquals(
                    new BigDecimal("4.5000000000"),
                    calculator.calculateFromProfile(
                            new BigDecimal("90"),
                            new BigDecimal("5")
                    )
            )
    );
}
}
