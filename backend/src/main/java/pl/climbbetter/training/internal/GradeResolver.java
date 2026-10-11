package pl.climbbetter.training.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
class GradeResolver {

    private final JdbcClient jdbcClient;

    GradeResolver(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    ResolvedGrade resolve(String climbType, String gradeLabel) {
        String scaleCode = scaleCodeFor(climbType);

        return jdbcClient.sql("""
                        SELECT
                            id,
                            scale_code,
                            label,
                            grade_index,
                            grade_points,
                            calibration_version
                        FROM catalog.grades
                        WHERE scale_code = :scaleCode
                          AND label = :gradeLabel
                          AND is_active = true
                        """)
                .param("scaleCode", scaleCode)
                .param("gradeLabel", gradeLabel)
                .query((resultSet, rowNumber) -> new ResolvedGrade(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("scale_code"),
                        resultSet.getString("label"),
                        resultSet.getInt("grade_index"),
                        resultSet.getBigDecimal("grade_points"),
                        resultSet.getInt("calibration_version")
                ))
                .optional()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown active grade %s for scale %s"
                                .formatted(gradeLabel, scaleCode)
                ));
    }

    private String scaleCodeFor(String climbType) {
        if (climbType == null) {
            throw new IllegalArgumentException("Climb type is required");
        }

        return switch (climbType) {
            case "BOULDER" -> "BOULDER_FONT";
            case "ROUTE", "CIRCUIT" -> "ROUTE_FRENCH";
            default -> throw new IllegalArgumentException(
                    "Unsupported climb type: " + climbType
            );
        };
    }

    record ResolvedGrade(
            UUID id,
            String scaleCode,
            String label,
            int gradeIndex,
            BigDecimal gradePoints,
            int calibrationVersion
    ) {
    }
}