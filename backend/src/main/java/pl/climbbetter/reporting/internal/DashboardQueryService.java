package pl.climbbetter.reporting.internal;

import java.util.List;

import org.springframework.stereotype.Service;

import pl.climbbetter.reporting.DashboardQuery;
import pl.climbbetter.reporting.DashboardSessionSummary;

@Service 
class DashboardQueryService implements DashboardQuery{

    @Override
    public List<DashboardSessionSummary> findRecentSessions() {
        return List.of();
    }

}
