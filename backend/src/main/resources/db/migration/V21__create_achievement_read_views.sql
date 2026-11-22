-- Read projections only: historical calculations, fingerprints and revisions are unchanged.
CREATE VIEW reporting.achievement_ascents AS
SELECT
    entry.*,
    session.user_id AS achievement_user_id,
    session.session_date AS achievement_session_date,
    COALESCE(session.started_at, session.created_at) AS achievement_session_started_at,
    session.created_at AS achievement_session_created_at,
    session.area_id AS achievement_area_id,
    session.location_name_snapshot AS achievement_area_name
FROM training.training_entries entry
JOIN training.training_sessions session ON session.id = entry.session_id
WHERE session.deleted_at IS NULL
  AND entry.result_type = 'ASCENT'
  AND (
      (entry.climb_type IN ('BOULDER', 'CIRCUIT') AND entry.protection_mode IS NULL)
      OR (entry.climb_type = 'ROUTE' AND entry.protection_mode = 'LEAD')
  );

CREATE VIEW reporting.session_achievement_maxima AS
SELECT
    session.id AS session_id,
    (ARRAY_AGG(
        entry.grade_label
        ORDER BY entry.grade_index DESC NULLS LAST, entry.grade_points DESC NULLS LAST,
                 entry.entry_order, entry.id
    ) FILTER (WHERE entry.climb_type = 'BOULDER'))[1] AS max_boulder_grade,
    (ARRAY_AGG(
        entry.grade_label
        ORDER BY entry.grade_index DESC NULLS LAST, entry.grade_points DESC NULLS LAST,
                 entry.entry_order, entry.id
    ) FILTER (WHERE entry.climb_type IN ('ROUTE', 'CIRCUIT')))[1] AS max_route_grade
FROM training.training_sessions session
LEFT JOIN reporting.achievement_ascents entry ON entry.session_id = session.id
WHERE session.deleted_at IS NULL
GROUP BY session.id;
