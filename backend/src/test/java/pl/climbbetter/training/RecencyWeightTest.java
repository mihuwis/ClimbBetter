package pl.climbbetter.training;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecencyWeightTest {

    @ParameterizedTest
    @CsvSource({
            "0, 720, 1.0000000000",
            "180, 720, 0.5625000000",
            "360, 720, 0.2500000000",
            "540, 720, 0.0625000000",
            "720, 720, 0.0000000000",
            "721, 720, 0.0000000000",
            "365, 730, 0.2500000000",
            "366, 732, 0.2500000000",
            "729, 730, 0.0000018765"
    })
    void weightsEvidenceUsingTheSuppliedHorizon(long ageDays, int horizonDays, String expected) {
        assertEquals(new BigDecimal(expected), RecencyWeight.weight(ageDays, horizonDays));
    }

    @Test
    void approachesZeroMonotonicallyAtTheBoundary() {
        BigDecimal previous = BigDecimal.ONE;
        for (int age = 0; age <= 730; age++) {
            BigDecimal current = RecencyWeight.weight(age, 730);
            assertTrue(current.signum() >= 0 && current.compareTo(previous) <= 0);
            previous = current;
        }
        assertTrue(RecencyWeight.weight(729, 730).compareTo(new BigDecimal("0.000002")) < 0);
        assertEquals(0, RecencyWeight.weight(730, 730).signum());
    }

    @Test
    void handlesVeryOldObservationsWithoutOverflow() {
        assertEquals(new BigDecimal("0.0000000000"), RecencyWeight.weight(Long.MAX_VALUE, 730));
        assertEquals(new BigDecimal("1.0000000000"), RecencyWeight.weight(0, Integer.MAX_VALUE));
        assertEquals(new BigDecimal("0.0000000000"), RecencyWeight.weight(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void rejectsFutureObservationsAndInvalidHorizons() {
        assertThrows(IllegalArgumentException.class, () -> RecencyWeight.weight(-1, 730));
        assertThrows(IllegalArgumentException.class, () -> RecencyWeight.weight(0, 0));
        assertThrows(IllegalArgumentException.class, () -> RecencyWeight.weight(0, -1));
    }
}
