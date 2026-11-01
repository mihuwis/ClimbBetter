import type { ClimbType } from '../../training/model/session.types';
export type Area = { id: string; name: string; areaType: string; facilityType: string | null; isPublic: boolean; region: string | null; countryCode: string | null };
export type Sector = { id: string; areaId: string; name: string };
export type CatalogClimb = {
  id: string; areaId: string; name: string; climbType: ClimbType; gradeLabel: string | null;
  inputGradeLabel: string | null; effortProfileCode: string | null; suggestedMoveCount: number | null;
  lengthMeters: number | null; sectorId: string | null; sectorName: string | null; isPublic: boolean;
};
export type Grade = { id: string; scaleCode: string; label: string; gradeIndex: number };
export type EffortProfile = { code: string; displayName: string; description: string; baseEdl: number };
