export type ProfileSummary = {
  userName: string;
  sessionsCount: number;
  currentStreak: number;
  sessionsThisWeek: number;
};

export type ActivityFeedItem = {
  id: string;
  sessionName: string;
  date: string;
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