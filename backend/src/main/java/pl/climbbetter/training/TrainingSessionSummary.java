package pl.climbbetter.training;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TrainingSessionSummary(
        UUID sessionId, String sessionName, LocalDate sessionDate, String areaName,
        int durationMinutes, int totalMoves, BigDecimal classicLoad, BigDecimal adjustedLoad,
        BigDecimal averageMoveIntensity, String maxBoulderGrade, String maxRouteGrade
) { }
