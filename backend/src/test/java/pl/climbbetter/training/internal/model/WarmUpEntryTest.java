package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WarmUpEntryTest {

    @Test
    void createsWarmUpWithPositiveExecutedMoves() {
        WarmUpEntry entry = new WarmUpEntry(30);

        assertEquals(30, entry.executedMoves());
    }

    @Test
    void rejectsZeroExecutedMoves() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WarmUpEntry(0)
        );
    }

    @Test
    void rejectsNegativeExecutedMoves() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WarmUpEntry(-1)
        );
    }

    @Test
    void contributesMovesWithoutLoad() {
        WarmUpEntry entry = new WarmUpEntry(30);

        assertEquals(30, entry.executedMoves());
        assertEquals(
                new BigDecimal("0.0000"),
                entry.classicLoad()
        );
        assertEquals(
                new BigDecimal("0.0000"),
                entry.adjustedLoad()
        );
    }

    @Test
    void createsSessionContribution() {
        WarmUpEntry entry = new WarmUpEntry(30);

        EntryContribution contribution = entry.contribution();

        assertEquals(30, contribution.executedMoves());
        assertEquals(
                new BigDecimal("0.0000"),
                contribution.classicLoad()
        );
        assertEquals(
                new BigDecimal("0.0000"),
                contribution.adjustedLoad()
        );
    }
}