package pl.climbbetter.training;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** Estimates support from already filtered, distinct completed climbs in one grade scale. */
public final class PyramidLevelEstimator {
    public static final String ALGORITHM_VERSION = "pyramid-support-v1";

    private static final BigDecimal HALF_WEIGHT = new BigDecimal("0.50");
    private static final BigDecimal QUARTER_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal SUPPORT_THRESHOLD = new BigDecimal("3.00");
    private static final BigDecimal HIGH_CONFIDENCE_THRESHOLD = new BigDecimal("4.50");

    private PyramidLevelEstimator() {
    }

    public static Assessment estimate(
            Map<Integer, Integer> uniqueCountsByGrade,
            Map<Integer, String> gradeLabels
    ) {
        Objects.requireNonNull(uniqueCountsByGrade, "Grade counts are required");
        Objects.requireNonNull(gradeLabels, "Grade labels are required");

        NavigableMap<Integer, String> labels = validatedLabels(gradeLabels);
        NavigableMap<Integer, Integer> counts = new TreeMap<>();
        long sampleSize = 0;
        for (Map.Entry<Integer, Integer> entry : uniqueCountsByGrade.entrySet()) {
            Integer index = entry.getKey();
            Integer count = entry.getValue();
            if (index == null || index < 0 || count == null || count < 0) {
                throw new IllegalArgumentException("Grade indexes and counts must be nonnegative");
            }
            if (count == 0) {
                continue;
            }
            if (!labels.containsKey(index)) {
                throw new IllegalArgumentException("A grade label is required for index " + index);
            }
            counts.put(index, count);
            sampleSize += count;
        }
        if (sampleSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Sample size exceeds the supported range");
        }
        int size = (int) sampleSize;
        if (counts.isEmpty()) {
            return new Assessment(
                    null, null, "NO_ASCENTS", "UNASSESSED", 0,
                    null, null, null, null, ALGORITHM_VERSION
            );
        }

        int peakIndex = counts.lastKey();
        if (size < 10) {
            return peakFallback(peakIndex, labels, size, "LOW_SAMPLE_MAX");
        }

        for (int candidate : labels.headMap(peakIndex, true).descendingKeySet()) {
            int atOrAbove = counts.tailMap(candidate, true).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
            int minusOne = counts.getOrDefault(candidate - 1, 0);
            int minusTwo = counts.getOrDefault(candidate - 2, 0);
            BigDecimal support = BigDecimal.valueOf(Math.min(atOrAbove, 2))
                    .add(BigDecimal.valueOf(Math.min(minusOne, 4)).multiply(HALF_WEIGHT))
                    .add(BigDecimal.valueOf(Math.min(minusTwo, 8)).multiply(QUARTER_WEIGHT));

            if (support.compareTo(SUPPORT_THRESHOLD) >= 0) {
                boolean highConfidence = support.compareTo(HIGH_CONFIDENCE_THRESHOLD) >= 0
                        && atOrAbove > 0 && minusOne > 0 && minusTwo > 0;
                return new Assessment(
                        labels.get(candidate), candidate, "PYRAMID_SUPPORT",
                        highConfidence ? "HIGH" : "MEDIUM", size,
                        support, atOrAbove, minusOne, minusTwo, ALGORITHM_VERSION
                );
            }
        }
        return peakFallback(peakIndex, labels, size, "PEAK_FALLBACK");
    }

    private static NavigableMap<Integer, String> validatedLabels(Map<Integer, String> gradeLabels) {
        NavigableMap<Integer, String> labels = new TreeMap<>();
        for (Map.Entry<Integer, String> entry : gradeLabels.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 0
                    || entry.getValue() == null || entry.getValue().isBlank()) {
                throw new IllegalArgumentException("Grade labels require nonnegative indexes and nonblank labels");
            }
            labels.put(entry.getKey(), entry.getValue());
        }
        return labels;
    }

    private static Assessment peakFallback(
            int peakIndex,
            Map<Integer, String> labels,
            int sampleSize,
            String method
    ) {
        return new Assessment(
                labels.get(peakIndex), peakIndex, method, "LOW", sampleSize,
                null, null, null, null, ALGORITHM_VERSION
        );
    }

    public record Assessment(
            String gradeLabel,
            Integer gradeIndex,
            String method,
            String confidence,
            int sampleSize,
            BigDecimal supportScore,
            Integer supportAtOrAboveCount,
            Integer supportMinusOneCount,
            Integer supportMinusTwoCount,
            String algorithmVersion
    ) {
    }
}
