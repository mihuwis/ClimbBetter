package pl.climbbetter.training;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecencyWeightedLevelEstimatorTest {

    private static final LocalDate AS_OF = LocalDate.of(2026, 11, 4);
    private static final int HORIZON = 730;
    private static final BigDecimal FALLBACK = new BigDecimal("13");

    @Test
    void freshHighestAchievementSuppliesTheReferenceWithoutAveragingEasyClimbs() {
        List<RecencyWeightedLevelEstimator.Evidence> evidence = new ArrayList<>();
        evidence.add(evidence(17, 0));
        for (int i = 0; i < 100; i++) {
            evidence.add(evidence(5, 0));
        }
        var result = estimate(evidence);
        assertAssessment(result, "17.0000000000", "1.0000000000", 101, "RECENCY_WEIGHTED_PEAK");
        assertEquals("recency-weighted-peak-v1", result.algorithmVersion());
        assertEquals(HORIZON, result.horizonDays());
    }

    @Test
    void staleHighAchievementTransfersItsMissingWeightToRecentLowerAchievements() {
        var high = evidence(17, 365);
        var recent = evidence(13, 0);
        var result = estimate(List.of(high, recent));
        assertAssessment(result, "14.0000000000", "1.0000000000", 2, "RECENCY_WEIGHTED_PEAK");
        assertEquals(17, high.gradeIndex());
        assertEquals(13, recent.gradeIndex());
    }

    @Test
    void fillsTheHighestOneEffectiveUnitAcrossSeveralGrades() {
        var result = estimate(List.of(evidence(17, 365), evidence(15, 365), evidence(13, 0), evidence(5, 0)));
        assertAssessment(result, "14.5000000000", "1.0000000000", 4, "RECENCY_WEIGHTED_PEAK");
    }

    @Test
    void sparseOldEvidenceBlendsWithTheSuppliedSessionFallbackRatherThanGradeZero() {
        var result = estimate(List.of(evidence(17, 365)));
        assertAssessment(result, "14.0000000000", "0.2500000000", 1, "RECENCY_BLENDED_FALLBACK");
    }

    @Test
    void emptyOrExpiredEvidenceUsesExactlyTheSuppliedFallback() {
        assertAssessment(estimate(List.of()), "13.0000000000", "0.0000000000", 0, "CURRENT_SESSION_FALLBACK");
        assertAssessment(estimate(List.of(evidence(17, 730), evidence(20, 731))),
                "13.0000000000", "0.0000000000", 0, "CURRENT_SESSION_FALLBACK");
    }

    @Test
    void staleReferenceApproachesTheFallbackBeforeTheHistoryBoundary() {
        var before = estimate(List.of(evidence(17, 729)));
        var atBoundary = estimate(List.of(evidence(17, 730)));
        assertEquals(new BigDecimal("13.0000075060"), before.effectiveGradeIndex());
        assertEquals(new BigDecimal("13.0000000000"), atBoundary.effectiveGradeIndex());
        assertTrue(before.effectiveGradeIndex().subtract(atBoundary.effectiveGradeIndex())
                .compareTo(new BigDecimal("0.00001")) < 0);
    }

    @Test
    void latestRepeatRenewsOneClimbsEvidenceWithoutAddingAnotherSample() {
        UUID climbId = UUID.randomUUID();
        var old = new RecencyWeightedLevelEstimator.Evidence(climbId, 17, AS_OF.minusDays(365));
        var recent = new RecencyWeightedLevelEstimator.Evidence(climbId, 17, AS_OF);
        assertAssessment(estimate(List.of(old, recent, recent)), "17.0000000000", "1.0000000000", 1, "RECENCY_WEIGHTED_PEAK");
        assertEquals(AS_OF.minusDays(365), old.achievedOn());
    }

    @Test
    void latestSnapshotWinsWhenTheSameClimbHasAnOlderDifferentGrade() {
        UUID climbId = UUID.randomUUID();
        var old = new RecencyWeightedLevelEstimator.Evidence(climbId, 17, AS_OF.minusDays(20));
        var recent = new RecencyWeightedLevelEstimator.Evidence(climbId, 13, AS_OF);
        assertAssessment(estimate(List.of(old, recent)), "13.0000000000", "1.0000000000", 1, "RECENCY_WEIGHTED_PEAK");
        assertEquals(17, old.gradeIndex());
    }

    @Test
    void inputOrderDoesNotChangeTheResultOrMutateTheInput() {
        List<RecencyWeightedLevelEstimator.Evidence> original = List.of(
                evidence(17, 365), evidence(15, 365), evidence(13, 0), evidence(5, 0)
        );
        var reversed = new ArrayList<>(original);
        Collections.reverse(reversed);
        assertEquals(estimate(original), estimate(reversed));
        assertEquals(17, original.getFirst().gradeIndex());
    }

    @Test
    void equalDateConflictingGradesRequireTheCallerToResolveTheirOrder() {
        UUID climbId = UUID.randomUUID();
        var first = new RecencyWeightedLevelEstimator.Evidence(climbId, 13, AS_OF);
        var conflicting = new RecencyWeightedLevelEstimator.Evidence(climbId, 17, AS_OF);
        assertThrows(IllegalArgumentException.class, () -> estimate(List.of(first, conflicting)));
        assertThrows(IllegalArgumentException.class, () -> estimate(List.of(conflicting, first)));
    }

    @Test
    void supersededOldGradeTiesDoNotMakeDeduplicationDependOnInputOrder() {
        UUID climbId = UUID.randomUUID();
        var old = new RecencyWeightedLevelEstimator.Evidence(climbId, 13, AS_OF.minusDays(20));
        var oldConflicting = new RecencyWeightedLevelEstimator.Evidence(climbId, 17, AS_OF.minusDays(20));
        var latest = new RecencyWeightedLevelEstimator.Evidence(climbId, 15, AS_OF);
        var result = estimate(List.of(old, oldConflicting, latest));
        assertEquals(result, estimate(List.of(latest, oldConflicting, old)));
        assertAssessment(result, "15.0000000000", "1.0000000000", 1, "RECENCY_WEIGHTED_PEAK");
    }

    @Test
    void rejectsInvalidHorizonFallbackAndFutureEvidence() {
        assertThrows(IllegalArgumentException.class, () -> RecencyWeightedLevelEstimator.estimate(List.of(), AS_OF, 0, FALLBACK));
        assertThrows(IllegalArgumentException.class, () -> RecencyWeightedLevelEstimator.estimate(List.of(), AS_OF, -1, FALLBACK));
        assertThrows(IllegalArgumentException.class, () -> RecencyWeightedLevelEstimator.estimate(List.of(), AS_OF, HORIZON, new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> estimate(List.of(evidence(17, -1))));
        assertThrows(IllegalArgumentException.class, () -> new RecencyWeightedLevelEstimator.Evidence(UUID.randomUUID(), -1, AS_OF));
    }

    @Test
    void rejectsMissingInputs() {
        assertThrows(NullPointerException.class, () -> RecencyWeightedLevelEstimator.estimate(null, AS_OF, HORIZON, FALLBACK));
        assertThrows(NullPointerException.class, () -> RecencyWeightedLevelEstimator.estimate(List.of(), null, HORIZON, FALLBACK));
        assertThrows(NullPointerException.class, () -> RecencyWeightedLevelEstimator.estimate(List.of(), AS_OF, HORIZON, null));
        assertThrows(NullPointerException.class, () -> estimate(Collections.singletonList(null)));
        assertThrows(NullPointerException.class, () -> new RecencyWeightedLevelEstimator.Evidence(null, 1, AS_OF));
        assertThrows(NullPointerException.class, () -> new RecencyWeightedLevelEstimator.Evidence(UUID.randomUUID(), 1, null));
    }

    private RecencyWeightedLevelEstimator.Assessment estimate(List<RecencyWeightedLevelEstimator.Evidence> evidence) {
        return RecencyWeightedLevelEstimator.estimate(evidence, AS_OF, HORIZON, FALLBACK);
    }

    private RecencyWeightedLevelEstimator.Evidence evidence(int gradeIndex, int ageDays) {
        return new RecencyWeightedLevelEstimator.Evidence(UUID.randomUUID(), gradeIndex, AS_OF.minusDays(ageDays));
    }

    private void assertAssessment(RecencyWeightedLevelEstimator.Assessment result, String index, String weight, int sampleSize, String source) {
        assertEquals(new BigDecimal(index), result.effectiveGradeIndex());
        assertEquals(new BigDecimal(weight), result.historyWeight());
        assertEquals(sampleSize, result.sampleSize());
        assertEquals(source, result.source());
    }
}
