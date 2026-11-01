export type ClimbType = 'BOULDER' | 'ROUTE' | 'CIRCUIT';
export type SessionSummary = {
  sessionId: string; sessionName: string; sessionDate: string; areaName: string;
  durationMinutes: number; totalMoves: number; classicLoad: number; adjustedLoad: number;
  averageMoveIntensity: number | null; maxBoulderGrade: string | null; maxRouteGrade: string | null;
};
export type EntryCommand = {
  clientEntryId: string; entryOrder: number; resultType: 'WARMUP' | 'ASCENT' | 'ATTEMPT';
  climbId?: string | null; clientClimbId?: string | null; climbName?: string | null;
  climbType?: ClimbType | null; gradeLabel?: string | null; effortProfileCode?: string | null;
  ascentMode?: 'OS' | 'FLASH' | 'RP' | null; protectionMode?: 'LEAD' | 'TOP_ROPE' | null;
  totalMoves?: number | null; executedMoves: number; notes?: string | null; historyConflictConfirmed?: boolean;
};
export type SessionCommand = {
  clientSessionId: string; sessionName: string | null; sessionDate: string; timeZoneId: string;
  startedAt: string; endedAt: string; areaId?: string | null; clientAreaId?: string | null;
  areaName?: string | null; entries: EntryCommand[];
};
export type SessionEntry = EntryCommand & {
  entryId: string; climbName: string | null; effortProfileName: string | null;
  calculation: {
    edlCount: number | null; moveIntensity: number | null; classicLoad: number; adjustedLoad: number;
    styleMultiplier: number | null; relativeEffortMultiplier: number | null;
    currentLevelGradeLabel: string | null; currentLevelIndex: number | null;
    relativeGradeDifference: number | null; calculationModelVersion: string;
  };
  history: { conflictConfirmed: boolean | null; attemptOrdinalAllTime: number | null; familiarityBand: string | null };
};
export type SessionDetails = SessionSummary & {
  clientSessionId: string; version: number; timeZoneId: string; startedAt: string; endedAt: string;
  areaId: string; entries: SessionEntry[];
};
export type CurrentUser = { userId: string; displayName: string };
