package pl.climbbetter.catalog;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClimbMoveSuggestionTest {

    @Test
    void derivesMovesFromMetersAndLetsExplicitCountOverrideTheEstimate() {
        assertEquals(30, ClimbMoveSuggestion.resolve(null, new BigDecimal("15"), "ROUTE"));
        assertEquals(44, ClimbMoveSuggestion.resolve(null, new BigDecimal("22"), "ROUTE"));
        assertEquals(50, ClimbMoveSuggestion.resolve(null, new BigDecimal("25"), "ROUTE"));
        assertEquals(35, ClimbMoveSuggestion.resolve(35, new BigDecimal("15"), "ROUTE"));
        assertEquals(8, ClimbMoveSuggestion.resolve(null, null, "BOULDER"));
    }

    @Test
    void roundsFractionalMovesUpAndRejectsNonpositiveLength() {
        assertEquals(31, ClimbMoveSuggestion.resolve(null, new BigDecimal("15.25"), "ROUTE"));
        assertThrows(IllegalArgumentException.class,
                () -> ClimbMoveSuggestion.resolve(null, BigDecimal.ZERO, "ROUTE"));
    }
}
