import type { 
    ProfileSummary,
    ActivityFeedItem,
    DisciplineSummaryItem,
    SessionDetails
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
    sessionDate: '2026-05-20',
    discipline: 'Bouldering',
    location: 'Bronx',
    score: 125,
    maxGrade: '6B+',
    moves: 42,
    achievements: ['Best score this month', 'Hardest RP'],
    notes:
      'Good indoor bouldering session. Solid volume with a few harder attempts on steeper problems.',
  },
  {
    id: '2',
    sessionName: 'Evening Sport Climbing',
    sessionDate: '2026-05-18',
    discipline: 'Sport climbing',
    location: 'Avatar',
    score: 210,
    maxGrade: '6c+',
    moves: 96,
    achievements: ['Highest volume this week'],
    notes:
      'Endurance-oriented rope session with several routes in the 6a–6c range.',
  },
  {
    id: '3',
    sessionName: 'Garage Boulder Volume',
    sessionDate: '2026-05-16',
    discipline: 'Bouldering',
    location: 'Garaż',
    score: 155,
    maxGrade: '6C',
    moves: 64,
    achievements: ['Good volume'],
    notes:
      'Volume-focused bouldering session. Mostly moderate problems, short rests, steady pacing.',
  },
  {
    id: '4',
    sessionName: 'Outdoor Bouldering',
    sessionDate: '2026-05-14',
    discipline: 'Bouldering',
    location: 'Zimny Dół',
    score: 170,
    maxGrade: '7A',
    moves: 58,
    achievements: ['Hardest boulder this month'],
    notes:
      'Short outdoor session with good quality attempts on harder boulders.',
  },
  {
    id: '5',
    sessionName: 'Bolechowicka Sport Session',
    sessionDate: '2026-05-12',
    discipline: 'Sport climbing',
    location: 'Dolina Bolechowicka',
    score: 190,
    maxGrade: '6c',
    moves: 88,
    achievements: ['Outdoor climbing day'],
    notes:
      'Outdoor rope climbing session with mixed route lengths and moderate intensity.',
  },
  {
    id: '6',
    sessionName: 'Sadystówka Circuits',
    sessionDate: '2026-05-10',
    discipline: 'Circuit training',
    location: 'Zakrzówek - Sadystówka',
    score: 145,
    maxGrade: '6b+',
    moves: 120,
    achievements: ['High move count'],
    notes:
      'Circuit-style endurance session. Lower peak difficulty, but high movement volume.',
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

export const sessionDetailsMock: SessionDetails[] = [
  {
    ...activityFeedMock[0],
    duration: '2h 15min',
    entries: [
      {
        id: '1',
        climbName: 'Skrzat',
        grade: '6B',
        style: 'Flash',
        executedMoves: 4,
        totalMoves: 4,
        points: 52,
      },
      {
        id: '2',
        climbName: 'Dach',
        grade: '7A',
        style: 'Attempt',
        executedMoves: 3,
        totalMoves: 8,
        points: 35,
        notes: 'Good attempt, failed near the top.',
      },
      {
        id: '3',
        climbName: 'Zielony projekt',
        grade: '7A+',
        style: 'Attempt',
        executedMoves: 5,
        totalMoves: 10,
        points: 38,
      },
    ],
  },
];