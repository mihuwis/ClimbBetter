package pl.climbbetter.reporting.internal;

import org.springframework.stereotype.Service;

import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.reporting.DashboardPersonalBests;
import pl.climbbetter.reporting.DashboardPersonalBests.GradePersonalBest;
import pl.climbbetter.reporting.DashboardPersonalBests.ScalePersonalBests;
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

    @Override
    public DashboardPersonalBests findPersonalBests() {
        List<DashboardSessionRepository.PersonalBestRow> rows =
                sessionRepository.findPersonalBests(
                        currentUserProvider.currentUserId()
                );

        return new DashboardPersonalBests(
                personalBestsForScale(rows, "BOULDER_FONT"),
                personalBestsForScale(rows, "ROUTE_FRENCH")
        );
    }

    private ScalePersonalBests personalBestsForScale(
            List<DashboardSessionRepository.PersonalBestRow> rows,
            String scaleCode
    ) {
        DashboardSessionRepository.PersonalBestRow overall =
                find(rows, scaleCode, "OVERALL");
        DashboardSessionRepository.PersonalBestRow os =
                find(rows, scaleCode, "OS");
        DashboardSessionRepository.PersonalBestRow flash =
                find(rows, scaleCode, "FLASH");
        DashboardSessionRepository.PersonalBestRow rp =
                find(rows, scaleCode, "RP");

        Integer rpVsOsGap = rp == null || os == null
                ? null
                : rp.gradeIndex() - os.gradeIndex();

        return new ScalePersonalBests(
                toPersonalBest(overall),
                toPersonalBest(os),
                toPersonalBest(flash),
                toPersonalBest(rp),
                rpVsOsGap
        );
    }

    private DashboardSessionRepository.PersonalBestRow find(
            List<DashboardSessionRepository.PersonalBestRow> rows,
            String scaleCode,
            String category
    ) {
        return rows.stream()
                .filter(row -> scaleCode.equals(row.scaleCode()))
                .filter(row -> category.equals(row.category()))
                .findFirst()
                .orElse(null);
    }

    private GradePersonalBest toPersonalBest(
            DashboardSessionRepository.PersonalBestRow row
    ) {
        if (row == null) {
            return null;
        }

        return new GradePersonalBest(
                row.gradeLabel(),
                row.achievedOn(),
                row.sessionId()
        );
    }

}
