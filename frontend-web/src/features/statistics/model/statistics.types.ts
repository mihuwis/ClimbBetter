export type PyramidView = 'BOULDER' | 'ROUTE_AND_CIRCUIT' | 'COMBINED';
export type PyramidPeriod = 'LAST_TWO_YEARS' | 'ALL_TIME';
export type PyramidStyle = 'RP' | 'OS';

export type PyramidFilters = {
  view: PyramidView;
  period: PyramidPeriod;
  style: PyramidStyle;
  asOf: string;
};

export type PyramidPeak = {
  gradeLabel: string | null;
  gradeIndex: number | null;
  gradePoints: number | null;
  gradeCalibrationVersion: number | null;
};

export type PyramidLayer = PyramidPeak & {
  bucketKey: string;
  climbCount: number;
};

export type PyramidLevelAssessment = {
  gradeLabel: string | null;
  gradeIndex: number | null;
  method: 'NO_ASCENTS' | 'LOW_SAMPLE_MAX' | 'PYRAMID_SUPPORT' | 'PEAK_FALLBACK';
  confidence: 'UNASSESSED' | 'LOW' | 'MEDIUM' | 'HIGH';
  sampleSize: number;
  supportScore: number | null;
  supportAtOrAboveCount: number | null;
  supportMinusOneCount: number | null;
  supportMinusTwoCount: number | null;
  algorithmVersion: string;
};

export type AchievementPyramid = {
  filters: PyramidFilters;
  windowStart: string | null;
  windowEnd: string;
  uniqueClimbCount: number;
  layers: PyramidLayer[];
  peak: PyramidPeak | null;
  levelAssessment: PyramidLevelAssessment | null;
  gradeCalibrationVersions: number[];
  assessmentUnavailableReason: 'COMBINED_SCALE' | null;
  peakUnavailableReason: 'MIXED_POINT_CALIBRATIONS' | null;
};

export type PyramidClimb = {
  climbId: string;
  climbName: string;
  areaId: string;
  areaName: string;
  climbType: 'BOULDER' | 'ROUTE' | 'CIRCUIT';
  gradeScaleCode: 'BOULDER_FONT' | 'ROUTE_FRENCH';
  gradeLabel: string;
  gradePoints: number;
  gradeCalibrationVersion: number;
  achievedOn: string;
  ascentMode: 'RP' | 'OS' | 'FLASH';
  sessionId: string;
  entryId: string;
};

export type PyramidClimbsPage = {
  filters: PyramidFilters;
  bucketKey: string;
  totalCount: number;
  offset: number;
  limit: number;
  nextOffset: number | null;
  items: PyramidClimb[];
};
