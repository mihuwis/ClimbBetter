package pl.climbbetter.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PyramidClimbsPage(
        PyramidRequest filters, String bucketKey, long totalCount,
        int offset, int limit, Integer nextOffset, List<Climb> items
) {
    public record Climb(
            UUID climbId, String climbName, UUID areaId, String areaName,
            String climbType, String gradeScaleCode, String gradeLabel,
            BigDecimal gradePoints, int gradeCalibrationVersion,
            LocalDate achievedOn, String ascentMode, UUID sessionId, UUID entryId
    ) { }
}
