export type PersonalBest = { gradeLabel: string; achievedOn: string; sessionId: string };
export type ScaleBests = { overall: PersonalBest | null; os: PersonalBest | null; flash: PersonalBest | null; rp: PersonalBest | null; rpVsOsGap: number | null };
export type PersonalBests = { boulder: ScaleBests; route: ScaleBests };
