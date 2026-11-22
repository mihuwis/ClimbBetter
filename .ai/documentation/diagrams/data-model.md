# ERD — schemat wdrożony

Aktualizacja: 2026-11-09. Uproszczenie relacji z migracji V1–V24, bez pełnej listy kolumn. [Opis bazy](../database.md).

```mermaid
erDiagram
    USERS ||--o{ USER_IDENTITIES : maps
    USERS ||--o{ TRAINING_SESSIONS : owns
    AREAS ||--o{ SECTORS : contains
    AREAS ||--o{ CLIMBS : contains
    SECTORS o|--o{ CLIMBS : groups
    AREAS ||--o{ TRAINING_SESSIONS : locates
    GRADES o|--o{ CLIMBS : grades
    GRADES ||--o{ GRADE_ALIASES : maps
    TRAINING_SESSIONS ||--|{ TRAINING_ENTRIES : contains
    CLIMBS o|--o{ TRAINING_ENTRIES : identifies
    GRADES o|--o{ TRAINING_ENTRIES : snapshots
    EFFORT_PROFILES o|--o{ TRAINING_ENTRIES : snapshots
    TRAINING_SESSIONS ||--o{ TRAINING_SESSION_REVISIONS : audits
    USER_IDENTITIES {
        uuid user_id FK
        text issuer
        text subject
    }
    TRAINING_SESSIONS {
        uuid id PK
        uuid user_id FK
        uuid area_id FK
        uuid client_session_id
        date session_date
        bigint version
        timestamptz deleted_at
    }
    TRAINING_ENTRIES {
        uuid id PK
        uuid session_id FK
        uuid climb_id FK
        uuid client_entry_id
        int entry_order
        text calculation_model_version
        numeric profile_intensity_index
        numeric effective_current_level_index
        numeric classic_load
        numeric adjusted_load
    }
```

Snapshot jest zapisany w kolumnach wpisu, summary w sesji. Rozgrzewka nie ma Grade/Climb/profilu. Revisions dotyczą sesji, a nie osobnej tabeli snapshotów wpisu. Widoki `reporting.achievement_ascents` i `session_achievement_maxima` nie są kopiami danych. Trwały UserGradeReference pozostaje planem, dlatego nie występuje w ERD.
