package pl.climbbetter.training;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PyramidLevelEstimatorTest {
    private static final Map<Integer, String> ROUTE_LABELS = IntStream.range(0, 19)
            .boxed()
            .collect(Collectors.toUnmodifiableMap(
                    index -> index,
                    index -> new String[]{
                            "1", "2", "3", "4a", "4b", "4c", "5a", "5b", "5c",
                            "6a", "6a+", "6b", "6b+", "6c", "6c+", "7a", "7a+", "7b", "7b+"
                    }[index]
            ));

    @Test
    void emptyAchievementHistoryHasNoEstimatedLevel() {
        var assessment = PyramidLevelEstimator.estimate(Map.of(), ROUTE_LABELS);

        assertEquals("NO_ASCENTS", assessment.method());
        assertEquals("UNASSESSED", assessment.confidence());
        assertEquals(0, assessment.sampleSize());
        assertNull(assessment.gradeLabel());
        assertNull(assessment.gradeIndex());
        assertNull(assessment.supportScore());
        assertEquals("pyramid-support-v1", assessment.algorithmVersion());
    }

    @Test
    void nineDistinctAscentsUsePeakWithLowConfidenceEvenIfSupportWouldReachThreshold() {
        var assessment = PyramidLevelEstimator.estimate(Map.of(15, 1, 14, 8), ROUTE_LABELS);

        assertEquals("7a", assessment.gradeLabel());
        assertEquals("LOW_SAMPLE_MAX", assessment.method());
        assertEquals("LOW", assessment.confidence());
        assertEquals(9, assessment.sampleSize());
        assertNull(assessment.supportScore());
        assertNull(assessment.supportAtOrAboveCount());
    }

    @Test
    void documentedSeasonSupportsAnIntermediateGradeWithoutAnAscentAtThatExactGrade() {
        var assessment = PyramidLevelEstimator.estimate(
                Map.of(15, 1, 13, 3, 12, 2, 11, 5), ROUTE_LABELS
        );

        assertEquals("6c+", assessment.gradeLabel());
        assertEquals(14, assessment.gradeIndex());
        assertEquals("PYRAMID_SUPPORT", assessment.method());
        assertEquals("MEDIUM", assessment.confidence());
        assertEquals(11, assessment.sampleSize());
        assertScore("3.00", assessment);
        assertEquals(1, assessment.supportAtOrAboveCount());
        assertEquals(3, assessment.supportMinusOneCount());
        assertEquals(2, assessment.supportMinusTwoCount());
    }

    @Test
    void secondApprovedExampleSelectsHighestSupportedGradeRatherThanHighestSupportScore() {
        var assessment = PyramidLevelEstimator.estimate(
                Map.of(13, 5, 15, 3, 16, 1, 17, 2, 18, 1), ROUTE_LABELS
        );

        assertEquals("7b", assessment.gradeLabel());
        assertEquals("PYRAMID_SUPPORT", assessment.method());
        assertEquals("MEDIUM", assessment.confidence());
        assertEquals(12, assessment.sampleSize());
        assertScore("3.25", assessment);
        // Three climbs at/above 7b are kept as evidence; only two contribute to support.
        assertEquals(3, assessment.supportAtOrAboveCount());
        assertEquals(1, assessment.supportMinusOneCount());
        assertEquals(3, assessment.supportMinusTwoCount());
    }

    @Test
    void highConfidenceStartsAtFourAndAHalfWithAtLeastTenAscents() {
        var assessment = PyramidLevelEstimator.estimate(Map.of(15, 2, 14, 2, 13, 6), ROUTE_LABELS);

        assertEquals("7a", assessment.gradeLabel());
        assertEquals("HIGH", assessment.confidence());
        assertEquals(10, assessment.sampleSize());
        assertScore("4.50", assessment);
    }

    @Test
    void capsLimitAllTiersButRawCountsRemainAvailable() {
        var assessment = PyramidLevelEstimator.estimate(Map.of(15, 5, 14, 10, 13, 20), ROUTE_LABELS);

        assertEquals("7a", assessment.gradeLabel());
        assertEquals("HIGH", assessment.confidence());
        assertScore("6.00", assessment);
        assertEquals(5, assessment.supportAtOrAboveCount());
        assertEquals(10, assessment.supportMinusOneCount());
        assertEquals(20, assessment.supportMinusTwoCount());
    }

    @Test
    void moreEasyClimbsDoNotKeepRaisingSupportAndMissingTierHasMediumConfidence() {
        var first = PyramidLevelEstimator.estimate(Map.of(13, 1, 9, 50), ROUTE_LABELS);
        var second = PyramidLevelEstimator.estimate(Map.of(13, 1, 9, 500), ROUTE_LABELS);

        assertEquals("6b", first.gradeLabel());
        assertEquals(first.gradeLabel(), second.gradeLabel());
        assertEquals("MEDIUM", first.confidence());
        assertScore("3.00", first);
        assertScore("3.00", second);
        assertEquals(0, first.supportMinusOneCount());
        assertEquals(50, first.supportMinusTwoCount());
        assertEquals(500, second.supportMinusTwoCount());
    }

    @Test
    void sufficientSampleWithoutPyramidCandidateFallsBackToPeakWithLowConfidence() {
        var assessment = PyramidLevelEstimator.estimate(Map.of(15, 10), ROUTE_LABELS);

        assertEquals("7a", assessment.gradeLabel());
        assertEquals("PEAK_FALLBACK", assessment.method());
        assertEquals("LOW", assessment.confidence());
        assertEquals(10, assessment.sampleSize());
        assertNull(assessment.supportScore());
    }

    @Test
    void bottomOfScaleHasNoInventedLowerGrades() {
        var assessment = PyramidLevelEstimator.estimate(Map.of(1, 1, 0, 10), ROUTE_LABELS);

        assertEquals("2", assessment.gradeLabel());
        assertEquals("MEDIUM", assessment.confidence());
        assertScore("3.00", assessment);
        assertEquals(0, assessment.supportMinusTwoCount());
    }

    @Test
    void zeroCountsDoNotCreateAPeakOrRequireAMissingGradeLabel() {
        var assessment = PyramidLevelEstimator.estimate(Map.of(25, 0), Map.of());

        assertEquals("NO_ASCENTS", assessment.method());
        assertEquals(0, assessment.sampleSize());
        assertNull(assessment.gradeLabel());
    }

    @Test
    void rejectsInvalidCountsAndMissingOrBlankLabels() {
        assertThrows(IllegalArgumentException.class,
                () -> PyramidLevelEstimator.estimate(Map.of(15, -1), ROUTE_LABELS));
        assertThrows(IllegalArgumentException.class,
                () -> PyramidLevelEstimator.estimate(Map.of(-1, 1), ROUTE_LABELS));
        assertThrows(IllegalArgumentException.class,
                () -> PyramidLevelEstimator.estimate(Map.of(25, 1), ROUTE_LABELS));
        assertThrows(IllegalArgumentException.class,
                () -> PyramidLevelEstimator.estimate(Map.of(1, 1), Map.of(1, " ")));
        assertThrows(IllegalArgumentException.class,
                () -> PyramidLevelEstimator.estimate(Map.of(1, Integer.MAX_VALUE, 2, 1), ROUTE_LABELS));
    }

    private static void assertScore(String expected, PyramidLevelEstimator.Assessment assessment) {
        assertEquals(0, new BigDecimal(expected).compareTo(assessment.supportScore()));
    }
}
