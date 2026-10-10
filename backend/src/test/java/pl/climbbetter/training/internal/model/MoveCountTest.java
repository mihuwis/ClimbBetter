package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoveCountTest {

    @Test
    void acceptsExecutedMovesGreaterThanRouteLength() {
        assertDoesNotThrow(() -> new MoveCount(12, 20));
    }

    @Test
    void rejectsNonPositiveTotalMoves() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MoveCount(0, 0)
        );
    }

    @Test
    void rejectsNegativeExecutedMoves() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MoveCount(12, -1)
        );
    }
}