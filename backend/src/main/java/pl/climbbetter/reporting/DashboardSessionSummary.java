package pl.climbbetter.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


public record DashboardSessionSummary(
        UUID sessionId,
        String sessionName,
        LocalDate sessionDate,
        String areaName,
        int durationMinutes,
        int totalMoves,
        BigDecimal classicLoad,
        BigDecimal adjustedLoad,
        String maxBoulderGrade,
        String maxRouteGrade
) {

}
