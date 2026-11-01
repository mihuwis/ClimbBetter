ALTER TABLE catalog.areas
    ADD COLUMN country_code VARCHAR(2),
    ADD COLUMN region TEXT;

CREATE TABLE catalog.sectors (
    id UUID PRIMARY KEY,
    area_id UUID NOT NULL REFERENCES catalog.areas(id),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    is_archived BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (id, area_id)
);
CREATE INDEX sectors_area_name_index ON catalog.sectors(area_id, name);

CREATE TABLE catalog.grade_aliases (
    id UUID PRIMARY KEY,
    system_code TEXT NOT NULL,
    label TEXT NOT NULL,
    canonical_grade_id UUID NOT NULL REFERENCES catalog.grades(id),
    mapping_version INTEGER NOT NULL CHECK (mapping_version > 0),
    is_active BOOLEAN NOT NULL DEFAULT true,
    source_url TEXT NOT NULL,
    UNIQUE (system_code, label, mapping_version)
);

-- Approximate conversion; preserve the original label. IV covers 4a-4b in
-- the source chart; v1 selects the lower canonical grade 4a.
WITH aliases(label, french_label) AS (
    VALUES ('IV', '4a'), ('V-', '5a'), ('V', '5b'),
           ('VI', '6a'), ('VI+', '6a+'), ('VI.1', '6b'), ('VI.1+', '6b+'),
           ('VI.2', '6c'), ('VI.2+', '6c+'), ('VI.3', '7a'), ('VI.3+', '7a+'),
           ('VI.4', '7b'), ('VI.4+', '7b+'), ('VI.4+/5', '7c'),
           ('VI.5', '7c+'), ('VI.5+', '8a'), ('VI.5+/6', '8a+')
)
INSERT INTO catalog.grade_aliases (
    id, system_code, label, canonical_grade_id, mapping_version, source_url
)
SELECT md5('GRADE_ALIAS:KURTYKA:v1:' || aliases.label)::uuid,
       'KURTYKA', aliases.label, grade.id, 1,
       CASE WHEN grade.grade_index >= 15
            THEN 'https://www.ranking.pza.org.pl/info/regulamin'
            ELSE 'https://topo.uka.pl/content/topo/skala.html' END
FROM aliases
JOIN catalog.grades grade
  ON grade.scale_code = 'ROUTE_FRENCH' AND grade.label = aliases.french_label;

ALTER TABLE catalog.climbs
    ALTER COLUMN grade_id DROP NOT NULL,
    ADD COLUMN sector_id UUID,
    ADD COLUMN length_meters NUMERIC(8, 2) CHECK (length_meters > 0),
    ADD COLUMN input_grade_system_code TEXT,
    ADD COLUMN input_grade_label TEXT,
    ADD COLUMN grade_mapping_version INTEGER CHECK (grade_mapping_version > 0),
    ADD CONSTRAINT climbs_sector_area_fk
        FOREIGN KEY (sector_id, area_id) REFERENCES catalog.sectors(id, area_id);

CREATE INDEX climbs_sector_index ON catalog.climbs(sector_id) WHERE sector_id IS NOT NULL;
