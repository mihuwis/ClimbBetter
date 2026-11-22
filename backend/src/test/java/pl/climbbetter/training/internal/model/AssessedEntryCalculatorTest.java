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

        EntryMetrics result = calculator.calculateV2(
                new BigDecimal("90"),
                new EffortIntensityProfile(
                        new BigDecimal("7"), null, null, null, null, null
                ),
                new BigDecimal("1.20"),
                new BigDecimal("1.50"),
                moveCount
        );

        assertAll(
                () -> assertEquals(
                        new BigDecimal("7"),
                        result.profileIntensityIndex()
                ),
                () -> assertEquals(
                        new BigDecimal("6.3000000000"),
                        result.moveIntensity()
                ),
                () -> assertEquals(
                        new BigDecimal("216.0000"),
                        result.classicLoad()
                ),
                () -> assertEquals(
                        new BigDecimal("907.2000"),
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
                new BigDecimal("216.0000"),
                contribution.classicLoad()
        ),
        () -> assertEquals(
                new BigDecimal("907.2000"),
                contribution.adjustedLoad()
        )
);
    }
}
