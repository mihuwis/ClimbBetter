export type AreaType = 'Gym' | 'Crag' | 'HomeWall' | 'Board' | 'Other';

export type AreaSummary = {
  id: string;
  name: string;
  type: AreaType;
  location: string;
  climbsCount: number;
  sectorsCount?: number;
  isOfficial: boolean;
  isFavorite: boolean;
  recentlyUsed: boolean;
  projectsCount: number;
};