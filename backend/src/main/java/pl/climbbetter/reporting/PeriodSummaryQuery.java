package pl.climbbetter.reporting;

public interface PeriodSummaryQuery {
    PeriodTrainingSummary findSummary(PeriodSummaryRequest request);
}
