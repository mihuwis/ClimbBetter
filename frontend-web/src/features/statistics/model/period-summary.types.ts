export type SummaryPeriod = 'MONTH' | 'YEAR';
export type SummaryDiscipline = 'ALL' | 'BOULDER' | 'ROUTE_AND_CIRCUIT';

export interface PeriodSummaryFilters {
  period: SummaryPeriod;
  year: number;
  month: number;
  discipline: SummaryDiscipline;
}

export interface PeriodMetrics {
  sessionCount: number;
  activeDays: number;
  durationMinutes: number;
  averageDurationMinutes: number | null;
  executedMoves: number;
  classicLoad: number;
  adjustedLoad: number;
  averageClassicLoad: number | null;
  averageAdjustedLoad: number | null;
  maxClassicLoad: number | null;
  maxAdjustedLoad: number | null;
}

export interface PeriodSession {
  sessionId: string;
  sessionName: string;
  sessionDate: string;
  areaName: string;
  durationMinutes: number;
  executedMoves: number;
  classicLoad: number;
  adjustedLoad: number;
}

export interface PeriodBucket {
  windowStart: string;
  windowEnd: string;
  metrics: PeriodMetrics;
  sessions: PeriodSession[];
}

export interface PeriodAscent {
  sessionId: string;
  entryId: string;
  climbId: string;
  climbName: string;
  climbType: 'BOULDER' | 'ROUTE' | 'CIRCUIT';
  gradeLabel: string;
  gradeScaleCode: string;
  gradeIndex: number;
  ascentMode: 'OS' | 'FLASH' | 'RP';
  sessionDate: string;
  areaName: string;
}

export interface PeriodDisciplineAchievements {
  hardest: PeriodAscent | null;
  highestOs: PeriodAscent | null;
  highestFlash: PeriodAscent | null;
  highestRp: PeriodAscent | null;
}

export interface PeriodTrainingSummary {
  filters: Omit<PeriodSummaryFilters, 'month'> & { month: number | null };
  windowStart: string;
  windowEnd: string;
  summary: PeriodMetrics;
  uniqueClimbCount: number;
  warmupMoves: number;
  buckets: PeriodBucket[];
  disciplineBreakdown: { discipline: SummaryDiscipline; executedMoves: number }[];
  styleBreakdown: { style: 'OS' | 'FLASH' | 'RP'; climbCount: number }[];
  highlights: {
    maxClassicSession: PeriodSession | null;
    maxAdjustedSession: PeriodSession | null;
    boulders: PeriodDisciplineAchievements;
    routesAndCircuits: PeriodDisciplineAchievements;
  };
}
