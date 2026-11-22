import type { EffortProfile } from "../../catalog/model/catalog.types";
import type { ClimbType, EntryCommand } from "./session.types";

export function isCruxProfile(code: string | null | undefined): boolean {
  return code === "MEDIUM_CRUX" || code === "LONG_CRUX";
}

export function supportsEffortProfile(
  code: string,
  climbType: ClimbType,
): boolean {
  return climbType !== "BOULDER" || !isCruxProfile(code);
}

export function effortProfileIssue(
  entry: EntryCommand,
  profiles: EffortProfile[],
): string | null {
  if (entry.resultType === "WARMUP") return null;
  if (!entry.effortProfileCode) return "Choose an effort profile.";
  if (!profiles.some((profile) => profile.code === entry.effortProfileCode)) {
    return "This effort profile is no longer available. Choose an active profile.";
  }
  if (
    !supportsEffortProfile(
      entry.effortProfileCode,
      entry.climbType ?? "BOULDER",
    )
  ) {
    return "Crux profiles require a route or circuit. Choose another profile for this boulder.";
  }
  return null;
}
