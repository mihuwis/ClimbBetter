package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessedEntryCalculatorTest {

    private final AssessedEntryCalculator calculator =
            new AssessedEntryCalculator();

    @Test
    void calculatesMetricsIncludingRepeatedMoves() {
        MoveCount moveCount = new MoveCount(40, 80);

        EntryMetrics result = calculator.calculate(
                new BigDecimal("240"),
                new BigDecimal("40"),
                new BigDecimal("1.20"),
                new BigDecimal("1.50"),
                moveCount
        );

        assertAll(
                () -> assertEquals(
                        new BigDecimal("48.0000000000"),
                        result.edlCount()
                ),
                () -> assertEquals(
                        new BigDecimal("5.0000000000"),
                        result.moveIntensity()
                ),
                () -> assertEquals(
                        new BigDecimal("576.0000"),
                        result.classicLoad()
                ),
                () -> assertEquals(
                        new BigDecimal("720.0000"),
                        result.adjustedLoad()
                )
        );

        EntryContribution contribution =
        result.contribution(moveCount);

assertAll(
        () -> assertEquals(
                80,
                contribution.executedMoves()
        ),
        () -> assertEquals(
                new BigDecimal("576.0000"),
                contribution.classicLoad()
        ),
        () -> assertEquals(
                new BigDecimal("720.0000"),
                contribution.adjustedLoad()
        )
);
    }
}