-- Keep maxima as a read projection, but evaluate only the requested sessions.
-- The non-aggregated outer view allows owner/date/session restrictions to pass
-- through to its session scan. Existing indexes beginning with session_id bound
-- each correlated lookup to that session's entries.
CREATE OR REPLACE VIEW reporting.session_achievement_maxima AS
SELECT
    session.id AS session_id,
    (
        SELECT entry.grade_label
        FROM reporting.achievement_ascents entry
        WHERE entry.session_id = session.id
          AND entry.climb_type = 'BOULDER'
        ORDER BY entry.grade_index DESC NULLS LAST, entry.grade_points DESC NULLS LAST,
                 entry.entry_order, entry.id
        LIMIT 1
    ) AS max_boulder_grade,
    (
        SELECT entry.grade_label
        FROM reporting.achievement_ascents entry
        WHERE entry.session_id = session.id
          AND entry.climb_type IN ('ROUTE', 'CIRCUIT')
        ORDER BY entry.grade_index DESC NULLS LAST, entry.grade_points DESC NULLS LAST,
                 entry.entry_order, entry.id
        LIMIT 1
    ) AS max_route_grade
FROM training.training_sessions session
WHERE session.deleted_at IS NULL;
