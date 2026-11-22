package pl.climbbetter.reporting.internal;

import org.springframework.stereotype.Service;

import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.reporting.DashboardPersonalBests;
import pl.climbbetter.reporting.DashboardPersonalBests.GradePersonalBest;
import pl.climbbetter.reporting.DashboardPersonalBests.ScalePersonalBests;
import pl.climbbetter.reporting.DashboardQuery;
import pl.climbbetter.reporting.DashboardSessionSummary;
import pl.climbbetter.reporting.DashboardSessionsPage;

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
        return findSessionsPage(20, null).items();
    }

    @Override
    public DashboardSessionsPage findSessionsPage(int limit, String cursor) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Limit must be between 1 and 100");
        var rows = sessionRepository.findSessionsPage(currentUserProvider.currentUserId(), limit + 1,
                DashboardSessionCursor.decode(cursor));
        var items = rows.stream().limit(limit).map(DashboardSessionRepository.SessionRow::summary).toList();
        String next = null;
        if (rows.size() > limit) {
            var last = rows.get(limit - 1);
            next = new DashboardSessionCursor(last.summary().sessionDate(), last.createdAt(), last.summary().sessionId()).encode();
        }
        return new DashboardSessionsPage(items, next);
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
