package pl.climbbetter.reporting;

import java.time.LocalDate;

public record PyramidRequest(View view, Period period, Style style, LocalDate asOf) {
    public PyramidRequest {
        if (view == null || period == null || style == null || asOf == null) {
            throw new IllegalArgumentException("View, period, style and reference date are required");
        }
        if (asOf.getYear() < 1 || asOf.getYear() > 9999) {
            throw new IllegalArgumentException("Reference date year must be between 1 and 9999");
        }
    }

    public LocalDate windowStart() {
        return period == Period.ALL_TIME ? null : asOf.minusYears(2);
    }

    public String scaleCode() {
        return switch (view) {
            case BOULDER -> "BOULDER_FONT";
            case ROUTE_AND_CIRCUIT -> "ROUTE_FRENCH";
            case COMBINED -> throw new IllegalArgumentException("Combined points have no single grade scale");
        };
    }

    public enum View { BOULDER, ROUTE_AND_CIRCUIT, COMBINED }
    public enum Period { ALL_TIME, LAST_TWO_YEARS }
    public enum Style { RP, OS }
}
