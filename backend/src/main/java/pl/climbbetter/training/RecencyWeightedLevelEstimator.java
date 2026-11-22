package pl.climbbetter.training;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Estimates a reference index from the highest one effective unit of distinct-climb evidence. */
public final class RecencyWeightedLevelEstimator {

    public static final String ALGORITHM_VERSION = "recency-weighted-peak-v1";
    public static final String RECENCY_WEIGHTED_PEAK = "RECENCY_WEIGHTED_PEAK";
    public static final String RECENCY_BLENDED_FALLBACK = "RECENCY_BLENDED_FALLBACK";
    public static final String CURRENT_SESSION_FALLBACK = "CURRENT_SESSION_FALLBACK";

    private static final int SCALE = 10;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);
    private static final BigDecimal ONE = BigDecimal.ONE.setScale(SCALE);

    private RecencyWeightedLevelEstimator() {
    }

    public static Assessment estimate(
            List<Evidence> evidence,
            LocalDate asOf,
            int horizonDays,
            BigDecimal fallbackIndex
    ) {
        Objects.requireNonNull(evidence, "Evidence is required");
        Objects.requireNonNull(asOf, "Reference date is required");
        Objects.requireNonNull(fallbackIndex, "Fallback index is required");
        if (horizonDays <= 0) {
            throw new IllegalArgumentException("Recency horizon must be positive");
        }
        if (fallbackIndex.signum() < 0) {
            throw new IllegalArgumentException("Fallback index cannot be negative");
        }

        Map<UUID, LocalDate> latestDates = new HashMap<>();
        for (Evidence item : evidence) {
            Objects.requireNonNull(item, "Evidence item is required");
            if (item.achievedOn().isAfter(asOf)) {
                throw new IllegalArgumentException("Evidence cannot be after the reference date");
            }
            latestDates.merge(item.climbId(), item.achievedOn(),
                    (first, second) -> first.isAfter(second) ? first : second);
        }
        Map<UUID, Evidence> latestByClimb = new HashMap<>();
        for (Evidence item : evidence) {
            if (!item.achievedOn().equals(latestDates.get(item.climbId()))) {
                continue;
            }
            Evidence existing = latestByClimb.get(item.climbId());
            if (existing == null) {
                latestByClimb.put(item.climbId(), item);
            } else if (item.gradeIndex() != existing.gradeIndex()) {
                throw new IllegalArgumentException("Different grades for one climb on the same date require an explicit ordering");
            }
        }

        List<WeightedEvidence> weighted = latestByClimb.values().stream()
                .map(item -> new WeightedEvidence(item, RecencyWeight.weight(
                        ChronoUnit.DAYS.between(item.achievedOn(), asOf), horizonDays
                )))
                .filter(item -> item.weight().signum() > 0)
                .sorted(Comparator.comparingInt((WeightedEvidence item) -> item.evidence().gradeIndex()).reversed()
                        .thenComparing(item -> item.evidence().achievedOn(), Comparator.reverseOrder())
                        .thenComparing(item -> item.evidence().climbId().toString()))
                .toList();

        BigDecimal remaining = ONE;
        BigDecimal effectiveIndex = ZERO;
        for (WeightedEvidence item : weighted) {
            BigDecimal contribution = item.weight().min(remaining);
            effectiveIndex = effectiveIndex.add(contribution.multiply(BigDecimal.valueOf(item.evidence().gradeIndex())));
            remaining = remaining.subtract(contribution);
            if (remaining.signum() == 0) {
                break;
            }
        }
        effectiveIndex = effectiveIndex.add(remaining.multiply(fallbackIndex)).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal historyWeight = ONE.subtract(remaining);
        String source = historyWeight.signum() == 0 ? CURRENT_SESSION_FALLBACK
                : remaining.signum() == 0 ? RECENCY_WEIGHTED_PEAK : RECENCY_BLENDED_FALLBACK;
        return new Assessment(effectiveIndex, historyWeight, weighted.size(), source, ALGORITHM_VERSION, horizonDays);
    }

    /** Evidence grades are immutable observations, never discounted grade values. */
    public record Evidence(UUID climbId, int gradeIndex, LocalDate achievedOn) {
        public Evidence {
            Objects.requireNonNull(climbId, "Climb ID is required");
            Objects.requireNonNull(achievedOn, "Achievement date is required");
            if (gradeIndex < 0) {
                throw new IllegalArgumentException("Grade index cannot be negative");
            }
        }
    }

    public record Assessment(
            BigDecimal effectiveGradeIndex,
            BigDecimal historyWeight,
            int sampleSize,
            String source,
            String algorithmVersion,
            int horizonDays
    ) {
    }

    private record WeightedEvidence(Evidence evidence, BigDecimal weight) {
    }
}
