package pl.climbbetter.training.internal.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelativeEffortInterpolatorTest {
    private static final Map<Integer, BigDecimal> CALIBRATION = Map.ofEntries(
            Map.entry(-6, new BigDecimal("0.20")), Map.entry(-5, new BigDecimal("0.25")),
            Map.entry(-4, new BigDecimal("0.35")), Map.entry(-3, new BigDecimal("0.45")),
            Map.entry(-2, new BigDecimal("0.55")), Map.entry(-1, new BigDecimal("0.80")),
            Map.entry(0, new BigDecimal("1.00")), Map.entry(1, new BigDecimal("1.30")),
            Map.entry(2, new BigDecimal("1.80")), Map.entry(3, new BigDecimal("2.60")),
            Map.entry(4, new BigDecimal("3.50")), Map.entry(5, new BigDecimal("5.00"))
    );

    @Test
    void preservesEveryExistingIntegerCalibrationExactly() {
        CALIBRATION.forEach((difference, multiplier) -> {
            var result = RelativeEffortInterpolator.interpolate(10 + difference, BigDecimal.TEN, CALIBRATION);
            assertEquals(0, BigDecimal.valueOf(difference).compareTo(result.relativeGradeDifference()));
            assertEquals(multiplier, result.multiplier());
            assertFalse(result.outsideCalibratedRange());
        });
    }

    @Test
    void interpolatesOnBothSidesOfZeroWithoutRoundingTheReferenceGrade() {
        var harder = RelativeEffortInterpolator.interpolate(10, new BigDecimal("9.5"), CALIBRATION);
        var easier = RelativeEffortInterpolator.interpolate(10, new BigDecimal("10.5"), CALIBRATION);
        assertAll(
                () -> assertEquals(new BigDecimal("0.5"), harder.relativeGradeDifference()),
                () -> assertEquals(new BigDecimal("1.1500"), harder.multiplier()),
                () -> assertEquals(new BigDecimal("-0.5"), easier.relativeGradeDifference()),
                () -> assertEquals(new BigDecimal("0.9000"), easier.multiplier()),
                () -> assertFalse(harder.outsideCalibratedRange()),
                () -> assertFalse(easier.outsideCalibratedRange())
        );
    }

    @Test
    void usesTheCorrectIntervalForNegativeDifferencesAndRoundsOnlyTheMultiplier() {
        var result = RelativeEffortInterpolator.interpolate(10, new BigDecimal("11.333333333333"), CALIBRATION);
        assertAll(
                () -> assertEquals(new BigDecimal("-1.333333333333"), result.relativeGradeDifference()),
                () -> assertEquals(new BigDecimal("0.7167"), result.multiplier())
        );
    }

    @Test
    void clampsOnlyCalibrationLookupAndPreservesActualOutOfRangeDifferences() {
        var easier = RelativeEffortInterpolator.interpolate(0, new BigDecimal("10.25"), CALIBRATION);
        var harder = RelativeEffortInterpolator.interpolate(10, new BigDecimal("0.25"), CALIBRATION);
        assertAll(
                () -> assertEquals(new BigDecimal("-10.25"), easier.relativeGradeDifference()),
                () -> assertEquals(CALIBRATION.get(-6), easier.multiplier()),
                () -> assertTrue(easier.outsideCalibratedRange()),
                () -> assertEquals(new BigDecimal("9.75"), harder.relativeGradeDifference()),
                () -> assertEquals(CALIBRATION.get(5), harder.multiplier()),
                () -> assertTrue(harder.outsideCalibratedRange())
        );
    }

    @Test
    void doesNotReplaceMissingNeighboursWithTheNextAvailableRule() {
        var missing = new HashMap<>(CALIBRATION);
        missing.remove(0);
        assertThrows(IllegalArgumentException.class,
                () -> RelativeEffortInterpolator.interpolate(10, new BigDecimal("9.5"), missing));
        missing.put(0, CALIBRATION.get(0));
        missing.remove(1);
        assertThrows(IllegalArgumentException.class,
                () -> RelativeEffortInterpolator.interpolate(10, new BigDecimal("9.5"), missing));
        assertEquals(CALIBRATION.get(0), RelativeEffortInterpolator.interpolate(10, BigDecimal.TEN, missing).multiplier());
    }

    @Test
    void requiresTheSelectedBoundaryRuleAndPositiveMultipliers() {
        var rules = new HashMap<>(CALIBRATION);
        rules.remove(-6);
        assertThrows(IllegalArgumentException.class,
                () -> RelativeEffortInterpolator.interpolate(0, BigDecimal.TEN, rules));
        rules.put(0, BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> RelativeEffortInterpolator.interpolate(10, BigDecimal.TEN, rules));
    }

    @Test
    void rejectsInvalidInputsBeforeUsingTheCalibration() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> RelativeEffortInterpolator.interpolate(10, null, CALIBRATION)),
                () -> assertThrows(NullPointerException.class,
                        () -> RelativeEffortInterpolator.interpolate(10, BigDecimal.TEN, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RelativeEffortInterpolator.interpolate(-1, BigDecimal.TEN, CALIBRATION)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> RelativeEffortInterpolator.interpolate(10, new BigDecimal("-0.1"), CALIBRATION))
        );
    }
}
