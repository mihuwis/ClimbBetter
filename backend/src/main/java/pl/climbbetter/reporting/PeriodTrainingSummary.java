package pl.climbbetter.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read-only aggregates from saved training snapshots for one calendar period. */
public record PeriodTrainingSummary(
        PeriodSummaryRequest filters,
        LocalDate windowStart,
        LocalDate windowEnd,
        Metrics summary,
        long uniqueClimbCount,
        long warmupMoves,
        List<Bucket> buckets,
        List<DisciplineShare> disciplineBreakdown,
        List<StyleShare> styleBreakdown,
        Highlights highlights
) {
    public record Metrics(
            long sessionCount,
            int activeDays,
            long durationMinutes,
            BigDecimal averageDurationMinutes,
            long executedMoves,
            BigDecimal classicLoad,
            BigDecimal adjustedLoad,
            BigDecimal averageClassicLoad,
            BigDecimal averageAdjustedLoad,
            BigDecimal maxClassicLoad,
            BigDecimal maxAdjustedLoad
    ) { }

    public record Bucket(LocalDate windowStart, LocalDate windowEnd, Metrics metrics, List<Session> sessions) { }

    /** Time is always the full session time; moves and load match the discipline filter. */
    public record Session(
            UUID sessionId,
            String sessionName,
            LocalDate sessionDate,
            String areaName,
            long durationMinutes,
            long executedMoves,
            BigDecimal classicLoad,
            BigDecimal adjustedLoad
    ) { }

    public record DisciplineShare(PeriodSummaryRequest.Discipline discipline, long executedMoves) { }
    public record StyleShare(String style, long climbCount) { }

    public record Highlights(
            Session maxClassicSession,
            Session maxAdjustedSession,
            DisciplineAchievements boulders,
            DisciplineAchievements routesAndCircuits
    ) { }

    public record DisciplineAchievements(Ascent hardest, Ascent highestOs, Ascent highestFlash, Ascent highestRp) { }

    public record Ascent(
            UUID sessionId,
            UUID entryId,
            UUID climbId,
            String climbName,
            String climbType,
            String gradeLabel,
            String gradeScaleCode,
            int gradeIndex,
            String ascentMode,
            LocalDate sessionDate,
            String areaName
    ) { }
}
