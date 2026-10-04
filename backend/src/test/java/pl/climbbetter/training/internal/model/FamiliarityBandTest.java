package pl.climbbetter.training.internal.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class FamiliarityBandTest {
    @ParameterizedTest
    @CsvSource({
            "0, FIRST_CONTACT",
            "1, LOW",
            "10, LOW",
            "11, NORMAL",
            "20, NORMAL",
            "21, ESTABLISHED"
    })

    void mapsPriorContactCountToFamiliarityBand(int priorContactCount, FamiliarityBand expected) {
                assertEquals(
                    expected,
                    FamiliarityBand.fromPriorContactCount(priorContactCount));

    }

    @Test
    void rejectsNegativePriorContactCount() {
        assertThrows(
            IllegalArgumentException.class,
            () -> FamiliarityBand.fromPriorContactCount(-1)
        );
    }
}
