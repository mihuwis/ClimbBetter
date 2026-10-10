package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionMetricsCalculatorTest {

    private final SessionMetricsCalculator calculator =
            new SessionMetricsCalculator();

    @Test
    void aggregatesAssessedEntryAndWarmUp() {
        EntryContribution assessedEntry =
                new EntryContribution(
                        80,
                        new BigDecimal("576.0000"),
                        new BigDecimal("720.0000")
                );

        EntryContribution warmUp =
                new WarmUpEntry(30).contribution();

        SessionMetrics result = calculator.calculate(
                List.of(assessedEntry, warmUp)
        );

        assertAll(
                () -> assertEquals(110, result.totalMoves()),
                () -> assertEquals(
                        new BigDecimal("576.0000"),
                        result.classicLoad()
                ),
                () -> assertEquals(
                        new BigDecimal("720.0000"),
                        result.adjustedLoad()
                )
        );
    }
}