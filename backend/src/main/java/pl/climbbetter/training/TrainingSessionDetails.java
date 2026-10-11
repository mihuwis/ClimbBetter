package pl.climbbetter.training;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TrainingSessionDetails(
        UUID sessionId,
        String sessionName,
        LocalDate sessionDate,
        String timeZoneId,
        Instant startedAt,
        Instant endedAt,
        int durationMinutes,
        UUID areaId,
        String areaName,
        int totalMoves,
        BigDecimal classicLoad,
        BigDecimal adjustedLoad,
        BigDecimal averageMoveIntensity,
        String maxBoulderGrade,
        String maxRouteGrade,
        List<EntryDetails> entries
) {

    public record EntryDetails(
            UUID entryId,
            UUID clientEntryId,
            int entryOrder,
            String resultType,
            UUID climbId,
            String climbName,
            String climbType,
            String gradeScaleCode,
            String gradeLabel,
            String effortProfileCode,
            String effortProfileName,
            String ascentMode,
            String protectionMode,
            Integer totalMoves,
            int executedMoves,
            String notes,
            CalculationDetails calculation,
            HistoryDetails history
    ) {
    }

    public record CalculationDetails(
            BigDecimal edlCount,
            BigDecimal moveIntensity,
            BigDecimal styleMultiplier,
            BigDecimal relativeEffortMultiplier,
            BigDecimal classicLoad,
            BigDecimal adjustedLoad,
            String currentLevelGradeLabel,
            Integer currentLevelIndex,
            Integer relativeGradeDifference,
            Boolean outsideCalibratedRange,
            String calculationModelVersion
    ) {
    }

    public record HistoryDetails(
            Integer contactCountBeforeTwoYears,
            Integer attemptOrdinalAllTime,
            String familiarityBand,
            String familiaritySource,
            Boolean fastRp,
            Boolean conflictDetected,
            Boolean conflictConfirmed
    ) {
    }
}
