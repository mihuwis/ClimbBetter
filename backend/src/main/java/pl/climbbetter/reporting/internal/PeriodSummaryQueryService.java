package pl.climbbetter.reporting.internal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pl.climbbetter.identity.CurrentUserProvider;
import pl.climbbetter.reporting.PeriodSummaryQuery;
import pl.climbbetter.reporting.PeriodSummaryRequest;
import pl.climbbetter.reporting.PeriodTrainingSummary;
import pl.climbbetter.reporting.PeriodTrainingSummary.Ascent;
import pl.climbbetter.reporting.PeriodTrainingSummary.Bucket;
import pl.climbbetter.reporting.PeriodTrainingSummary.DisciplineAchievements;
import pl.climbbetter.reporting.PeriodTrainingSummary.DisciplineShare;
import pl.climbbetter.reporting.PeriodTrainingSummary.Highlights;
import pl.climbbetter.reporting.PeriodTrainingSummary.Metrics;
import pl.climbbetter.reporting.PeriodTrainingSummary.Session;
import pl.climbbetter.reporting.PeriodTrainingSummary.StyleShare;
import pl.climbbetter.reporting.internal.PeriodSummaryRepository.AscentRow;
import pl.climbbetter.reporting.internal.PeriodSummaryRepository.SessionRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
class PeriodSummaryQueryService implements PeriodSummaryQuery {
    private static final int RESULT_SCALE = 4;
    private final PeriodSummaryRepository repository;
    private final CurrentUserProvider currentUserProvider;

    PeriodSummaryQueryService(PeriodSummaryRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public PeriodTrainingSummary findSummary(PeriodSummaryRequest request) {
        if (request == null) throw new IllegalArgumentException("Summary filters are required");
        UUID userId = currentUserProvider.currentUserId();
        List<SessionRow> sessions = repository.findSessions(
                userId, request.windowStart(), request.windowEndExclusive(), request.discipline().name());
        List<AscentRow> ascents = repository.findAscents(
                userId, request.windowStart(), request.windowEndExclusive(), request.discipline().name());

        Map<LocalDate, List<SessionRow>> byBucket = new LinkedHashMap<>();
        for (LocalDate date = request.windowStart(); date.isBefore(request.windowEndExclusive()); date = nextBucket(request, date)) {
            byBucket.put(date, new ArrayList<>());
        }
        for (SessionRow session : sessions) byBucket.get(request.bucketStart(session.sessionDate())).add(session);
        List<Bucket> buckets = byBucket.entrySet().stream().map(entry -> new Bucket(
                entry.getKey(), nextBucket(request, entry.getKey()).minusDays(1), metrics(entry.getValue()),
                entry.getValue().stream().map(PeriodSummaryQueryService::session).toList()
        )).toList();

        // One climb can earn several styles in a year. Recompute the best style for this entire window,
        // rather than adding monthly unique counts or allowing an out-of-window OS to dominate.
        Map<UUID, Integer> bestStyles = new HashMap<>();
        for (AscentRow ascent : ascents) bestStyles.merge(ascent.climbId(), styleRank(ascent.ascentMode()), Math::max);
        long[] styleCounts = new long[4];
        for (int rank : bestStyles.values()) styleCounts[rank]++;

        return new PeriodTrainingSummary(
                request, request.windowStart(), request.windowEnd(), metrics(sessions), bestStyles.size(),
                sumMoves(sessions, SessionRow::warmupMoves), buckets,
                List.of(new DisciplineShare(PeriodSummaryRequest.Discipline.BOULDER, sumMoves(sessions, SessionRow::boulderMoves)),
                        new DisciplineShare(PeriodSummaryRequest.Discipline.ROUTE_AND_CIRCUIT, sumMoves(sessions, SessionRow::routeMoves))),
                List.of(new StyleShare("OS", styleCounts[3]), new StyleShare("FLASH", styleCounts[2]), new StyleShare("RP", styleCounts[1])),
                new Highlights(bestSession(sessions, SessionRow::classicLoad), bestSession(sessions, SessionRow::adjustedLoad),
                        achievements(ascents, true), achievements(ascents, false))
        );
    }

    private static LocalDate nextBucket(PeriodSummaryRequest request, LocalDate date) {
        return request.period() == PeriodSummaryRequest.Period.MONTH ? date.plusDays(1) : date.plusMonths(1);
    }

    private static Metrics metrics(List<SessionRow> sessions) {
        long duration = 0;
        long moves = 0;
        BigDecimal classic = BigDecimal.ZERO.setScale(RESULT_SCALE);
        BigDecimal adjusted = BigDecimal.ZERO.setScale(RESULT_SCALE);
        BigDecimal maxClassic = null;
        BigDecimal maxAdjusted = null;
        Set<LocalDate> days = new HashSet<>();
        for (SessionRow session : sessions) {
            duration = Math.addExact(duration, session.durationMinutes());
            moves = Math.addExact(moves, session.executedMoves());
            classic = classic.add(session.classicLoad());
            adjusted = adjusted.add(session.adjustedLoad());
            maxClassic = maxClassic == null ? session.classicLoad() : maxClassic.max(session.classicLoad());
            maxAdjusted = maxAdjusted == null ? session.adjustedLoad() : maxAdjusted.max(session.adjustedLoad());
            days.add(session.sessionDate());
        }
        return new Metrics(sessions.size(), days.size(), duration, average(BigDecimal.valueOf(duration), sessions.size()),
                moves, classic, adjusted, average(classic, sessions.size()), average(adjusted, sessions.size()),
                maxClassic, maxAdjusted);
    }

    private static BigDecimal average(BigDecimal sum, int count) {
        return count == 0 ? null : sum.divide(BigDecimal.valueOf(count), RESULT_SCALE, RoundingMode.HALF_UP);
    }

    private static long sumMoves(List<SessionRow> sessions, Function<SessionRow, Long> getter) {
        return sessions.stream().mapToLong(getter::apply).reduce(0L, Math::addExact);
    }

    private static int styleRank(String style) {
        return switch (style) {
            case "OS" -> 3;
            case "FLASH" -> 2;
            case "RP" -> 1;
            default -> throw new IllegalArgumentException("Unsupported successful ascent style");
        };
    }

    private static Session bestSession(List<SessionRow> sessions, Function<SessionRow, BigDecimal> load) {
        SessionRow best = null;
        for (SessionRow candidate : sessions) {
            if (best == null || load.apply(candidate).compareTo(load.apply(best)) > 0) best = candidate;
        }
        return best == null ? null : session(best);
    }

    private static DisciplineAchievements achievements(List<AscentRow> ascents, boolean boulders) {
        Ascent hardest = null;
        Ascent os = null;
        Ascent flash = null;
        Ascent rp = null;
        // The repository orders each scale by descending grade, then stable chronological ties.
        for (AscentRow candidate : ascents) {
            if ("BOULDER".equals(candidate.climbType()) != boulders) continue;
            if (hardest == null) hardest = ascent(candidate);
            if (os == null && "OS".equals(candidate.ascentMode())) os = ascent(candidate);
            if (flash == null && "FLASH".equals(candidate.ascentMode())) flash = ascent(candidate);
            if (rp == null && "RP".equals(candidate.ascentMode())) rp = ascent(candidate);
        }
        return new DisciplineAchievements(hardest, os, flash, rp);
    }

    private static Session session(SessionRow row) {
        return new Session(row.sessionId(), row.sessionName(), row.sessionDate(), row.areaName(),
                row.durationMinutes(), row.executedMoves(), row.classicLoad(), row.adjustedLoad());
    }

    private static Ascent ascent(AscentRow row) {
        return new Ascent(row.sessionId(), row.entryId(), row.climbId(), row.climbName(), row.climbType(),
                row.gradeLabel(), row.gradeScaleCode(), row.gradeIndex(), row.ascentMode(), row.sessionDate(), row.areaName());
    }
}
