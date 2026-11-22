package pl.climbbetter.reporting;

import java.time.LocalDate;
import java.time.YearMonth;

public record PeriodSummaryRequest(Period period, int year, Integer month, Discipline discipline) {
    public PeriodSummaryRequest {
        if (period == null || discipline == null) {
            throw new IllegalArgumentException("Period and discipline are required");
        }
        if (year < 1 || year > 9998) {
            throw new IllegalArgumentException("Year must be between 1 and 9998");
        }
        if (period == Period.MONTH && (month == null || month < 1 || month > 12)) {
            throw new IllegalArgumentException("A month between 1 and 12 is required for a monthly summary");
        }
        if (period == Period.YEAR && month != null) {
            throw new IllegalArgumentException("A yearly summary does not accept a month");
        }
    }

    public LocalDate windowStart() {
        return LocalDate.of(year, period == Period.MONTH ? month : 1, 1);
    }

    public LocalDate windowEndExclusive() {
        return period == Period.MONTH ? windowStart().plusMonths(1) : windowStart().plusYears(1);
    }

    public LocalDate windowEnd() {
        return windowEndExclusive().minusDays(1);
    }

    public LocalDate bucketStart(LocalDate date) {
        return period == Period.MONTH ? date : YearMonth.from(date).atDay(1);
    }

    public enum Period { MONTH, YEAR }
    public enum Discipline { ALL, BOULDER, ROUTE_AND_CIRCUIT }
}
