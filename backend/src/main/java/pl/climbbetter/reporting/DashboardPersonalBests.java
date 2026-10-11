package pl.climbbetter.reporting;

import java.time.LocalDate;
import java.util.UUID;

public record DashboardPersonalBests(
        ScalePersonalBests boulder,
        ScalePersonalBests route
) {

    public record ScalePersonalBests(
            GradePersonalBest overall,
            GradePersonalBest os,
            GradePersonalBest flash,
            GradePersonalBest rp,
            Integer rpVsOsGap
    ) {
    }

    public record GradePersonalBest(
            String gradeLabel,
            LocalDate achievedOn,
            UUID sessionId
    ) {
    }
}
