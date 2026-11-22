package pl.climbbetter.training;

/** Qualification for climbing achievements, independent of training workload. */
public final class AchievementEligibility {
    private AchievementEligibility() {
    }

    public static boolean isEligible(
            String resultType,
            String climbType,
            String protectionMode
    ) {
        if (!"ASCENT".equals(resultType)) {
            return false;
        }
        if ("ROUTE".equals(climbType)) {
            return "LEAD".equals(protectionMode);
        }
        return ("BOULDER".equals(climbType) || "CIRCUIT".equals(climbType))
                && protectionMode == null;
    }
}
