import type { Page } from '@playwright/test';
import type { PeriodAscent, PeriodMetrics, PeriodSummaryFilters, PeriodTrainingSummary } from '../../src/features/statistics/model/period-summary.types';

const emptyMetrics = (): PeriodMetrics => ({
  sessionCount: 0, activeDays: 0, durationMinutes: 0, averageDurationMinutes: null,
  executedMoves: 0, classicLoad: 0, adjustedLoad: 0,
  averageClassicLoad: null, averageAdjustedLoad: null, maxClassicLoad: null, maxAdjustedLoad: null,
});
const noAchievements = () => ({ hardest: null, highestOs: null, highestFlash: null, highestRp: null });
const iso = (year: number, month: number, day: number) => `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

export function summaryFixture(filters: PeriodSummaryFilters, empty = false): PeriodTrainingSummary {
  const { period, year, month, discipline } = filters;
  const start = iso(year, period === 'YEAR' ? 1 : month, 1);
  const endMonth = period === 'YEAR' ? 12 : month;
  const endDay = new Date(year, endMonth, 0).getDate();
  const end = iso(year, endMonth, endDay);
  const metrics: PeriodMetrics = empty ? emptyMetrics() : {
    sessionCount: 3, activeDays: 2, durationMinutes: 240, averageDurationMinutes: 80,
    executedMoves: discipline === 'ALL' ? 240 : discipline === 'BOULDER' ? 80 : 120,
    classicLoad: 300, adjustedLoad: 600, averageClassicLoad: 100, averageAdjustedLoad: 200,
    maxClassicLoad: 150, maxAdjustedLoad: 300,
  };
  const sessionDate = iso(year, period === 'YEAR' ? 2 : month, 2);
  const session = {
    sessionId: 'source-summary-session', sessionName: 'Power and endurance', sessionDate,
    areaName: 'Bronx', durationMinutes: 90, executedMoves: 100, classicLoad: 150, adjustedLoad: 300,
  };
  const first: PeriodMetrics = empty ? emptyMetrics() : { ...metrics, sessionCount: 2, activeDays: 1,
    durationMinutes: 150, averageDurationMinutes: 75, executedMoves: metrics.executedMoves - 60,
    classicLoad: 200, adjustedLoad: 400, averageClassicLoad: 100, averageAdjustedLoad: 200 };
  const second: PeriodMetrics = empty ? emptyMetrics() : { ...metrics, sessionCount: 1, activeDays: 1,
    durationMinutes: 90, averageDurationMinutes: 90, executedMoves: 60, classicLoad: 100,
    adjustedLoad: 200, averageClassicLoad: 100, averageAdjustedLoad: 200, maxClassicLoad: 100, maxAdjustedLoad: 200 };
  const count = period === 'YEAR' ? 12 : endDay;
  const buckets = Array.from({ length: count }, (_, i) => {
    const bucketMonth = period === 'YEAR' ? i + 1 : month;
    const bucketDay = period === 'YEAR' ? 1 : i + 1;
    const bucketEnd = period === 'YEAR' ? iso(year, bucketMonth, new Date(year, bucketMonth, 0).getDate()) : iso(year, month, bucketDay);
    const active = i === 1 ? first : i === 3 ? second : emptyMetrics();
    const day = iso(year, bucketMonth, period === 'YEAR' ? 2 : bucketDay);
    const firstMoves = Math.ceil(first.executedMoves * 0.6);
    const sessions = empty ? [] : i === 1 ? [
      { ...session, sessionDate: day, executedMoves: firstMoves },
      { ...session, sessionId: 'second-summary-session', sessionName: 'Short technique session', sessionDate: day,
        durationMinutes: 60, executedMoves: first.executedMoves - firstMoves, classicLoad: 50, adjustedLoad: 100 },
    ] : i === 3 ? [{ ...session, sessionId: 'third-summary-session', sessionName: 'Evening climbing',
      sessionDate: day, executedMoves: 60, classicLoad: 100, adjustedLoad: 200 }] : [];
    return { windowStart: iso(year, bucketMonth, bucketDay), windowEnd: bucketEnd, metrics: active, sessions };
  });
  const ascent: PeriodAscent = {
    sessionId: session.sessionId, entryId: 'entry-os', climbId: 'climb-os', climbName: 'First light',
    climbType: 'BOULDER', gradeLabel: '6C', gradeScaleCode: 'BOULDER_FONT', gradeIndex: 11,
    ascentMode: 'OS', sessionDate, areaName: 'Bronx',
  };
  return {
    filters: { ...filters, month: period === 'YEAR' ? null : month }, windowStart: start, windowEnd: end,
    summary: metrics, uniqueClimbCount: empty ? 0 : 6, warmupMoves: empty || discipline !== 'ALL' ? 0 : 40,
    buckets,
    disciplineBreakdown: [
      { discipline: 'BOULDER', executedMoves: empty || discipline === 'ROUTE_AND_CIRCUIT' ? 0 : 80 },
      { discipline: 'ROUTE_AND_CIRCUIT', executedMoves: empty || discipline === 'BOULDER' ? 0 : 120 },
    ],
    styleBreakdown: [{ style: 'OS', climbCount: empty ? 0 : 2 }, { style: 'FLASH', climbCount: empty ? 0 : 1 }, { style: 'RP', climbCount: empty ? 0 : 3 }],
    highlights: { maxClassicSession: empty ? null : session, maxAdjustedSession: empty ? null : session,
      boulders: empty || discipline === 'ROUTE_AND_CIRCUIT' ? noAchievements() : { hardest: ascent, highestOs: ascent,
        highestFlash: { ...ascent, entryId: 'entry-flash', ascentMode: 'FLASH', gradeLabel: '6B+', gradeIndex: 10 },
        highestRp: { ...ascent, entryId: 'entry-rp', ascentMode: 'RP', gradeLabel: '6B', gradeIndex: 9 } },
      routesAndCircuits: noAchievements() },
  };
}

export async function periodApi(page: Page, options: { empty?: boolean; fail?: boolean; delay?: number } = {}) {
  const requests: URL[] = [];
  const state = { ...options };
  await page.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url());
    requests.push(url);
    if (url.pathname.endsWith('/identity/me')) return route.fulfill({ json: { userId: 'mihu-1', displayName: 'Mihu' } });
    if (url.pathname.endsWith('/statistics/summary')) {
      if (state.delay) await new Promise(resolve => setTimeout(resolve, state.delay));
      if (state.fail) return route.fulfill({ status: 503, json: { detail: 'Period statistics unavailable.' } });
      const filters: PeriodSummaryFilters = { period: url.searchParams.get('period') as 'MONTH' | 'YEAR',
        year: Number(url.searchParams.get('year')), month: Number(url.searchParams.get('month') ?? 1),
        discipline: url.searchParams.get('discipline') as PeriodSummaryFilters['discipline'] };
      return route.fulfill({ json: summaryFixture(filters, state.empty) }).catch(() => {});
    }
    if (url.pathname.includes('/training/sessions/')) return route.fulfill({ json: {
      sessionId: 'source-summary-session', sessionName: 'Power and endurance', sessionDate: '2026-10-02',
      areaName: 'Bronx', areaId: 'area-1', clientSessionId: 'source-client', version: 0,
      timeZoneId: 'Europe/Warsaw', durationMinutes: 90, totalMoves: 100, classicLoad: 150,
      adjustedLoad: 300, averageMoveIntensity: 6, maxBoulderGrade: '6C', maxRouteGrade: null,
      calculationModelVersion: 'v3', entries: [],
    } });
    return route.fulfill({ json: [] });
  });
  return { requests, state };
}
