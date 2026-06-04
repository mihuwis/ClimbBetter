import type { 
    ProfileSummary,
    ActivityFeedItem,
    DisciplineSummaryItem
 } from '../types/dashboard.types';



export const profileSummaryMock: ProfileSummary = {
  userName: 'Michał Wiśniewski',
  sessionsCount: 47,
  currentStreak: 4,
  sessionsThisWeek: 2,
};


export const activityFeedMock: ActivityFeedItem[] = [
  {
    id: '1',
    sessionName: 'Bronx Boulder Session',
    date: '20 May 2026',
    discipline: 'Bouldering',
    location: 'Bronx',
    score: 125,
    maxGrade: '6B+',
    moves: 42,
    achievements: ['Best score this month', 'Hardest RP'],
    notes:
      'Solid session. Good volume, few harder attempts, felt strong on steeper problems.',
  },
  {
    id: '2',
    sessionName: 'Evening Sport Climbing',
    date: '18 May 2026',
    discipline: 'Sport climbing',
    location: 'Kobylany',
    score: 210,
    maxGrade: '6c+',
    moves: 96,
    achievements: ['Highest volume this week'],
    notes:
      'Endurance-oriented session with several routes in the 6a–6c range.',
  },
  {
    id: '3',
    sessionName: 'Hangboard Strength Session',
    date: '16 May 2026',
    discipline: 'Training',
    location: 'Home wall',
    score: 80,
    maxGrade: '-',
    moves: 0,
    achievements: ['Training consistency'],
    notes:
      'Finger strength session. Repeaters and max hangs, moderate intensity.',
  },
  {
    id: '4',
    sessionName: 'Outdoor Bouldering',
    date: '14 May 2026',
    discipline: 'Bouldering',
    location: 'Zimny Dół',
    score: 170,
    maxGrade: '7A',
    moves: 58,
    achievements: ['Hardest boulder this month'],
    notes:
      'Short session but good quality attempts on harder problems.',
  },
  {
    id: '5',
    sessionName: 'Mobility and Core',
    date: '12 May 2026',
    discipline: 'Mobility',
    location: 'Gym',
    score: 35,
    maxGrade: '-',
    moves: 0,
    achievements: [],
    notes:
      'Low intensity support session focused on hips, shoulders and trunk stability.',
  },
];

export const disciplineSummaryMock: DisciplineSummaryItem[] = [
  {
    id: 'bouldering',
    name: 'Bouldering',
    sessionsCount: 12,
    totalScore: 1480,
    totalMoves: 420,
  },
  {
    id: 'sport-climbing',
    name: 'Sport climbing',
    sessionsCount: 8,
    totalScore: 1320,
    totalMoves: 680,
  },
  {
    id: 'training',
    name: 'Training',
    sessionsCount: 5,
    totalScore: 520,
    totalMoves: 180,
  },
];