export type ProfileSummary = {
  userName: string;
  sessionsCount: number;
  currentStreak: number;
  sessionsThisWeek: number;
};

export type ActivityFeedItem = {
  id: string;
  sessionName: string;
  sessionDate: string;
  discipline: string;
  location: string;
  score: number;
  maxGrade: string;
  moves: number;
  achievements: string[];
  notes: string;
};

export type DisciplineSummaryItem = {
  id: string;
  name: string;
  sessionsCount: number;
  totalScore: number;
  totalMoves: number;
};

export type SessionEntry = {
  id: string;
  climbType: string;
  climbName: string;
  grade: string;
  style: string;
  styleMultiplier: number;
  totalLength: number;
  executedMoves: number;
  fragmentMultiplier: number;
  gradePoints: number;
  points: number;
  notes?: string;
};

export type SessionDetails = ActivityFeedItem & {
  goal: string;
  method: string;
  duration?: string;
  entries: SessionEntry[];
};