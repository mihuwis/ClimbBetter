package pl.climbbetter.reporting.internal;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.climbbetter.reporting.PyramidClimbsPage.Climb;
import pl.climbbetter.reporting.PyramidRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Repository
class PyramidRepository {
    // Filter first, then select a representative ascent. Repeats outside the window or style
    // must never hide the first qualifying ascent inside the requested view.
    private static final String DISTINCT_ASCENTS = """
            WITH ranked_ascents AS (
                SELECT ascent.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY climb_id
                           ORDER BY achievement_session_date, achievement_session_started_at,
                                    session_id, entry_order, id
                       ) AS ascent_rank
                FROM reporting.achievement_ascents ascent
                WHERE achievement_user_id = :userId
                  AND achievement_session_date <= :asOf
                  AND (:allTime OR achievement_session_date >= :windowStart)
                  AND (
                      :view = 'COMBINED'
                      OR (:view = 'BOULDER' AND climb_type = 'BOULDER')
                      OR (:view = 'ROUTE_AND_CIRCUIT' AND climb_type IN ('ROUTE', 'CIRCUIT'))
                  )
                  AND (
                      (:style = 'RP' AND ascent_mode IN ('RP', 'OS', 'FLASH'))
                      OR (:style = 'OS' AND ascent_mode = 'OS')
                  )
            ), first_ascents AS (
                SELECT * FROM ranked_ascents WHERE ascent_rank = 1
            )
            """;

    private final JdbcClient jdbcClient;

    PyramidRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<GradeCount> findCounts(UUID userId, PyramidRequest request) {
        String grouping = request.view() == PyramidRequest.View.COMBINED
                ? """
                  SELECT NULL::integer AS grade_index, NULL::text AS grade_label,
                         grade_points, grade_calibration_version, COUNT(*)::integer AS climb_count
                  FROM first_ascents
                  GROUP BY grade_calibration_version, grade_points
                  ORDER BY grade_calibration_version, grade_points DESC
                  """
                : """
                  SELECT grade_index, MIN(grade_label) AS grade_label,
                         NULL::numeric AS grade_points, NULL::integer AS grade_calibration_version,
                         COUNT(*)::integer AS climb_count
                  FROM first_ascents
                  GROUP BY grade_index
                  ORDER BY grade_index DESC
                  """;
        return query(DISTINCT_ASCENTS + grouping, userId, request)
                .query((rs, row) -> new GradeCount(
                        rs.getObject("grade_index", Integer.class), rs.getString("grade_label"),
                        rs.getBigDecimal("grade_points"),
                        rs.getObject("grade_calibration_version", Integer.class),
                        rs.getInt("climb_count")
                )).list();
    }

    List<Integer> findCalibrationVersions(UUID userId, PyramidRequest request) {
        return query(DISTINCT_ASCENTS + """
                SELECT DISTINCT grade_calibration_version FROM first_ascents
                ORDER BY grade_calibration_version
                """, userId, request).query(Integer.class).list();
    }

    Map<Integer, String> findGradeLabels(String scaleCode) {
        Map<Integer, String> labels = new TreeMap<>();
        jdbcClient.sql("""
                        SELECT grade_index, label FROM catalog.grades
                        WHERE scale_code = :scaleCode ORDER BY grade_index
                        """)
                .param("scaleCode", scaleCode)
                .query((rs, row) -> Map.entry(rs.getInt("grade_index"), rs.getString("label")))
                .list().forEach(entry -> labels.put(entry.getKey(), entry.getValue()));
        return labels;
    }

    long countClimbs(UUID userId, PyramidRequest request, Bucket bucket) {
        return bucketQuery(DISTINCT_ASCENTS + "SELECT COUNT(*) FROM first_ascents WHERE "
                        + bucket.predicate(), userId, request, bucket)
                .query(Long.class).single();
    }

    List<Climb> findClimbs(UUID userId, PyramidRequest request, Bucket bucket, int offset, int limit) {
        return bucketQuery(DISTINCT_ASCENTS + """
                        SELECT climb_id, climb_name_snapshot, achievement_area_id, achievement_area_name,
                               climb_type, grade_scale_code, grade_label, grade_points,
                               grade_calibration_version, achievement_session_date, ascent_mode, session_id, id
                        FROM first_ascents WHERE
                        """ + bucket.predicate() + """
                         ORDER BY achievement_session_date, achievement_session_started_at,
                                  session_id, entry_order, id
                         LIMIT :limit OFFSET :offset
                        """, userId, request, bucket)
                .param("limit", limit).param("offset", offset)
                .query((rs, row) -> new Climb(
                        rs.getObject("climb_id", UUID.class), rs.getString("climb_name_snapshot"),
                        rs.getObject("achievement_area_id", UUID.class), rs.getString("achievement_area_name"),
                        rs.getString("climb_type"), rs.getString("grade_scale_code"), rs.getString("grade_label"),
                        rs.getBigDecimal("grade_points"), rs.getInt("grade_calibration_version"),
                        rs.getObject("achievement_session_date", LocalDate.class), rs.getString("ascent_mode"),
                        rs.getObject("session_id", UUID.class), rs.getObject("id", UUID.class)
                )).list();
    }

    private JdbcClient.StatementSpec query(String sql, UUID userId, PyramidRequest request) {
        return jdbcClient.sql(sql).param("userId", userId).param("asOf", request.asOf())
                .param("allTime", request.period() == PyramidRequest.Period.ALL_TIME)
                .param("windowStart", request.windowStart() == null ? request.asOf() : request.windowStart())
                .param("view", request.view().name()).param("style", request.style().name());
    }

    private JdbcClient.StatementSpec bucketQuery(
            String sql, UUID userId, PyramidRequest request, Bucket bucket
    ) {
        JdbcClient.StatementSpec query = query(sql, userId, request);
        return bucket.gradeIndex() != null
                ? query.param("bucketGradeIndex", bucket.gradeIndex())
                : query.param("bucketPoints", bucket.points()).param("bucketCalibration", bucket.calibration());
    }

    record GradeCount(Integer gradeIndex, String gradeLabel, BigDecimal gradePoints,
                      Integer calibrationVersion, int climbCount) { }

    record Bucket(Integer gradeIndex, BigDecimal points, Integer calibration) {
        static Bucket parse(PyramidRequest request, String key) {
            try {
                if (key == null) throw new IllegalArgumentException();
                String[] parts = key.split(":", -1);
                if (parts.length != 3) throw new IllegalArgumentException();
                if (request.view() != PyramidRequest.View.COMBINED
                        && "grade".equals(parts[0]) && request.scaleCode().equals(parts[1])
                        && parts[2].matches("[0-9]{1,9}")) {
                    return new Bucket(Integer.parseInt(parts[2]), null, null);
                }
                if (request.view() == PyramidRequest.View.COMBINED && "points".equals(parts[0])
                        && parts[1].matches("[0-9]{1,9}") && parts[2].matches("[0-9]{1,12}(\\.[0-9]{1,4})?")) {
                    int calibration = Integer.parseInt(parts[1]);
                    BigDecimal points = new BigDecimal(parts[2]);
                    if (calibration > 0 && points.signum() > 0) return new Bucket(null, points, calibration);
                }
            } catch (IllegalArgumentException ignored) {
                // Report invalid input without reflecting arbitrary request contents in errors.
            }
            throw new IllegalArgumentException("Bucket key does not match the selected pyramid view");
        }

        String predicate() {
            return gradeIndex != null ? "grade_index = :bucketGradeIndex"
                    : "grade_points = :bucketPoints AND grade_calibration_version = :bucketCalibration";
        }
    }
}
