package pl.climbbetter.reporting;

import pl.climbbetter.training.PyramidLevelEstimator.Assessment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AchievementPyramid(
        PyramidRequest filters,
        LocalDate windowStart,
        LocalDate windowEnd,
        int uniqueClimbCount,
        List<Layer> layers,
        Peak peak,
        Assessment levelAssessment,
        List<Integer> gradeCalibrationVersions,
        String assessmentUnavailableReason,
        String peakUnavailableReason
) {
    public record Layer(
            String bucketKey, String gradeLabel, Integer gradeIndex,
            BigDecimal gradePoints, Integer gradeCalibrationVersion, int climbCount
    ) { }

    public record Peak(
            String gradeLabel, Integer gradeIndex,
            BigDecimal gradePoints, Integer gradeCalibrationVersion
    ) { }
}
