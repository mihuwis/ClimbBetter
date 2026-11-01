-- Local import from the table supplied by the user on 2026-10-17.
-- Requires Flyway V15. Original Kurtyka labels are retained alongside v1 aliases.
SET client_encoding = 'UTF8';
BEGIN;

INSERT INTO catalog.areas (
    id, name, area_type, resolution_status, is_public, is_archived,
    country_code, region, created_at, updated_at, version
) VALUES (
    md5('PUBLIC_CATALOG:AREA:BOLECHOWICKA')::uuid,
    'Dolina Bolechowicka', 'ROCK', 'RESOLVED', true, false,
    'PL', 'Jura Krakowsko-Częstochowska', now(), now(), 0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO catalog.sectors (id, area_id, name, created_at, updated_at)
VALUES (
    md5('PUBLIC_CATALOG:SECTOR:FILAR_ABAZEGO')::uuid,
    md5('PUBLIC_CATALOG:AREA:BOLECHOWICKA')::uuid,
    'Filar Abazego', now(), now()
)
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE bolechowicka_import (
    portal_id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    input_grade_label TEXT,
    length_meters NUMERIC(8, 2) NOT NULL
) ON COMMIT DROP;

INSERT INTO bolechowicka_import VALUES
    (405, 'Zacięcie w Abazym', 'VI.2+', 22),
    (8571, 'Abazy Pasza', 'VI.3', 22),
    (406, 'Zacięcie w Abazym wprost', 'VI.3', 22),
    (407, 'Wariant Iwony', 'VI.5+', 24),
    (409, 'Przybycie Tytanów', 'VI.5+', 24),
    (410, 'Montokwas 212', 'VI.5', 24),
    (411, 'Skalne sztafety', 'VI.5', 24),
    (412, 'Abazy', 'VI.3+', 24),
    (414, 'Abazy wprost', 'VI.4', 24),
    (12743, 'Abazy w prawo', 'VI.3+', 24),
    (12741, 'Wariacje', 'VI.5+', 24),
    (12740, 'Klops', 'VI.5+/6', 24),
    (413, 'Void where prohibited', 'VI.3+', 24),
    (12574, 'Maska', 'VI.5+', 25),
    (415, 'Ryski Myszkowskiego', 'VI.2+', 22),
    (416, 'Akslop', 'VI.5+', 25),
    (417, 'Ryski nad tablicą', 'VI+', 22),
    (418, 'Ryski erektusa', 'VI.2', 23),
    (419, 'Warianty Emila Kamikadze', 'VI.3', 22),
    (12744, 'Emil Erectus', 'VI.3', 24),
    (420, 'Rysa Wallischa', 'V-', 20),
    (421, 'Filar Wallischa', 'VI.1', 20),
    (8572, 'Ostatni ranałt na Jurze', 'VI.1+', 20),
    (422, 'Depresja Malczyka', 'VI', 15),
    (12745, 'projekt', NULL, 20),
    (423, 'Marzenia Sprocketa', 'VI.2+', 8),
    (424, 'Mały Wallisch', 'IV', 15),
    (425, 'Krwawa pięść', 'V', 15);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM bolechowicka_import source
        WHERE source.input_grade_label IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM catalog.grade_aliases alias
              WHERE alias.system_code = 'KURTYKA'
                AND alias.label = source.input_grade_label
                AND alias.mapping_version = 1 AND alias.is_active
          )
    ) THEN
        RAISE EXCEPTION 'Missing Kurtyka mapping; import rolled back';
    END IF;
END $$;

INSERT INTO catalog.climbs (
    id, area_id, sector_id, name, climb_type, grade_id,
    input_grade_system_code, input_grade_label, grade_mapping_version,
    length_meters, suggested_move_count,
    is_public, is_verified, is_archived, created_at, updated_at, version
)
SELECT
    md5('PUBLIC_CATALOG:PORTAL_GORSKI:CLIMB:' || source.portal_id)::uuid,
    md5('PUBLIC_CATALOG:AREA:BOLECHOWICKA')::uuid,
    md5('PUBLIC_CATALOG:SECTOR:FILAR_ABAZEGO')::uuid,
    source.name, 'ROUTE', alias.canonical_grade_id,
    'KURTYKA', source.input_grade_label, alias.mapping_version,
    source.length_meters, ceil(source.length_meters * 2)::integer,
    true, false, false, now(), now(), 0
FROM bolechowicka_import source
LEFT JOIN catalog.grade_aliases alias
  ON alias.system_code = 'KURTYKA' AND alias.label = source.input_grade_label
 AND alias.mapping_version = 1 AND alias.is_active
ON CONFLICT (id) DO NOTHING;

COMMIT;
