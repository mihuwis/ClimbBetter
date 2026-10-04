package pl.climbbetter.reporting;

import java.util.List;

public interface DashboardQuery {
    List<DashboardSessionSummary> findRecentSessions();
    
}
