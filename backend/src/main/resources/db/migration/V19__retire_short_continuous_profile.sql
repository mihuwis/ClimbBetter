-- Keep historical references and legacy calculations, but stop offering the
-- profile omitted from the revised list when creating new climbs/entries.
UPDATE training.effort_profiles
SET is_active = false
WHERE code = 'SHORT_CONTINUOUS_ROUTE';
