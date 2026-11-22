package pl.climbbetter.reporting;

import java.util.List;

public record DashboardSessionsPage(List<DashboardSessionSummary> items, String nextCursor) {
}
