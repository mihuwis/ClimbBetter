package pl.climbbetter.training.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import pl.climbbetter.training.internal.model.EffortIntensityProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Component
class IntensityProfileResolver {

    private static final Set<String> CRUX_PROFILES = Set.of("MEDIUM_CRUX", "LONG_CRUX");
    private static final BigDecimal POINTS_DIVISOR = new BigDecimal("100");
    private static final BigDecimal HALF_REFERENCE_INTERVAL = new BigDecimal("2.5");
    private static final int FULL_GRADE_OFFSET = 2;
    private static final int CALCULATION_SCALE = 10;

    private final JdbcClient jdbcClient;

    IntensityProfileResolver(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    EffortIntensityProfile resolve(
            GradeResolver.ResolvedGrade grade,
            EffortProfileResolver.ResolvedEffortProfile profile
    ) {
        BigDecimal index = profile.profileIntensityIndex();
        if (index == null || index.signum() <= 0) {
            throw new IllegalArgumentException(
                    "No v2 intensity index configured for profile " + profile.code()
            );
        }
        if (!CRUX_PROFILES.contains(profile.code())) {
            return new EffortIntensityProfile(index, null, null, null, null, null);
        }
        if (!"ROUTE_FRENCH".equals(grade.scaleCode())) {
            throw new IllegalArgumentException("Crux route profiles require a route grade");
        }

        List<MoveGrade> grades = jdbcClient.sql("""
                        SELECT label, grade_index, grade_points
                        FROM catalog.grades
                        WHERE scale_code = 'BOULDER_FONT' AND is_active = true
                        ORDER BY grade_index
                        """)
                .query((rs, row) -> new MoveGrade(
                        rs.getString("label"), rs.getInt("grade_index"),
                        rs.getBigDecimal("grade_points")
                ))
                .list();
        if (grades.isEmpty()) {
            throw new IllegalStateException("Boulder calibration grades are missing");
        }

        // Font points include midpoints of French ranges. Use the upper equivalent
        // within half a reference interval, e.g. French 7a -> Font 6B.
        BigDecimal upperEquivalent = grade.gradePoints().add(HALF_REFERENCE_INTERVAL);
        int basePosition = 0;
        for (int position = 0; position < grades.size(); position++) {
            if (grades.get(position).points().compareTo(upperEquivalent) <= 0) {
                basePosition = position;
            }
        }
        MoveGrade base = grades.get(basePosition);
        MoveGrade hard = grades.get(Math.min(grades.size() - 1, basePosition + FULL_GRADE_OFFSET));
        MoveGrade easy = grades.get(Math.max(0, basePosition - FULL_GRADE_OFFSET));
        BigDecimal baseIntensity = grade.gradePoints().multiply(index)
                .divide(POINTS_DIVISOR, CALCULATION_SCALE, RoundingMode.HALF_UP);

        return new EffortIntensityProfile(
                index, base.label(), hard.label(), easy.label(),
                relativeIntensity(baseIntensity, hard.points(), base.points()),
                relativeIntensity(baseIntensity, easy.points(), base.points())
        );
    }

    private BigDecimal relativeIntensity(
            BigDecimal baseIntensity, BigDecimal sectionPoints, BigDecimal basePoints
    ) {
        return baseIntensity.multiply(sectionPoints)
                .divide(basePoints, CALCULATION_SCALE, RoundingMode.HALF_UP);
    }

    private record MoveGrade(String label, int index, BigDecimal points) { }
}
