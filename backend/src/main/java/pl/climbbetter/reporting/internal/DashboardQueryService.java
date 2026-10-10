package pl.climbbetter.reporting.internal;

import org.springframework.stereotype.Service;

import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.reporting.DashboardQuery;
import pl.climbbetter.reporting.DashboardSessionSummary;

import java.util.List;
@Service 
class DashboardQueryService implements DashboardQuery{

    private final DashboardSessionRepository sessionRepository;
    private final CurrentUserProvider currentUserProvider;

    DashboardQueryService(
            DashboardSessionRepository sessionRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.sessionRepository = sessionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<DashboardSessionSummary> findRecentSessions() {
        return sessionRepository.findRecentSessions(currentUserProvider.currentUserId());
    }

}
