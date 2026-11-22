package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffortIntensityProfileTest {

    @Test
    void distinguishesSimpleAndCompleteCruxProfiles() {
        assertAll(
                () -> assertFalse(new EffortIntensityProfile(
                        new BigDecimal("7"), null, null, null, null, null
                ).isCrux()),
                () -> assertTrue(new EffortIntensityProfile(
                        new BigDecimal("7"), "6B", "6C", "6A",
                        new BigDecimal("9.75"), new BigDecimal("8.5")
                ).isCrux())
        );
    }

    @Test
    void rejectsNonpositiveIndicesAndIncompleteCruxData() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> new EffortIntensityProfile(
                        BigDecimal.ZERO, null, null, null, null, null
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new EffortIntensityProfile(
                        new BigDecimal("7"), "6B", null, null, null, null
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new EffortIntensityProfile(
                        new BigDecimal("7"), "6B", "6C", "6A",
                        BigDecimal.ZERO, new BigDecimal("8.5")
                )),
                () -> assertThrows(IllegalArgumentException.class, () -> new EffortIntensityProfile(
                        new BigDecimal("7"), "6B", "6C", " ",
                        new BigDecimal("9.75"), new BigDecimal("8.5")
                ))
        );
    }
}
