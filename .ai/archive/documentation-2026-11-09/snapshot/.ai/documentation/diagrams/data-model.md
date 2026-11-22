# ERD: planowany model danych

Data: 2026-10-03. Uproszczony model logiczny ze specyfikacji, **nie wygenerowany schemat istniejącej bazy**. Relacja jednego Area na sesję jest zatwierdzona w DEC-018 (zamknięte OPEN-03). Pola nie są kompletnym DDL.

```mermaid
erDiagram
    USER ||--o{ TRAINING_SESSION : owns
    USER ||--o{ USER_GRADE_REFERENCE : has
    GRADE ||--o{ USER_GRADE_REFERENCE : defines
    AREA ||--o{ SECTOR : contains
    AREA ||--o{ CLIMB : contains
    SECTOR o|--o{ CLIMB : groups
    AREA ||--o{ TRAINING_SESSION : locates
    GRADE ||--o{ CLIMB : grades
    TRAINING_SESSION ||--o{ TRAINING_ENTRY : contains
    CLIMB o|--o{ TRAINING_ENTRY : identifies
    GRADE o|--o{ TRAINING_ENTRY : grades
    EFFORT_PROFILE o|--o{ TRAINING_ENTRY : profiles
    STYLE_RULE o|--o{ TRAINING_ENTRY : classifies
    TRAINING_ENTRY ||--|| CALCULATION_SNAPSHOT : embeds
    TRAINING_SESSION ||--|| SESSION_SUMMARY : caches
    TRAINING_ENTRY ||--o{ CALCULATION_REVISION : audits

    USER {
        uuid id PK
    }
    AREA {
        uuid id PK
        uuid clientAreaId
        string resolutionStatus
        boolean isArchived
    }
    SECTOR {
        uuid id PK
        uuid areaId FK
    }
    CLIMB {
        uuid id PK
        uuid areaId FK
        uuid sectorId FK
        uuid clientClimbId
        string climbType
    }
    TRAINING_SESSION {
        uuid id PK
        uuid userId FK
        uuid areaId FK
        uuid clientSessionId
        date sessionDate
        string timeZoneId
        bigint version
    }
    TRAINING_ENTRY {
        uuid id PK
        uuid sessionId FK
        uuid climbId FK
        uuid clientEntryId
        int entryOrder
        string resultType
        string ascentMode
        string familiarityBand
        int totalMoves
        int executedMoves
    }
    CALCULATION_SNAPSHOT {
        string calculationModelVersion
        decimal edlCount
        decimal moveIntensity
        decimal classicLoad
        decimal adjustedLoad
    }
```

`CalculationSnapshot` i `SessionSummary` są obiektami wartości osadzonymi w kolumnach wpisu i sesji, a nie obowiązkowymi oddzielnymi tabelami. `CalculationRevision` jest wymaganiem dla funkcji zmieniających obliczenia historyczne, jeszcze bez implementacji.

Climb każdego wpisu i jego opcjonalny Sector muszą należeć do Area sesji. Diagram pokazuje relacje; spójność tych powiązań wymaga również walidacji backendu. Jedno Area może mieć wiele sesji, a wiele sektorów w obrębie jednego Area nie wymaga dzielenia treningu.

Opcjonalność Grade/EffortProfile/StyleRule w tym diagramie sygnalizuje nierozstrzygnięty kontrakt WARMUP (OPEN-09); wpis oceniany wymaga odpowiednich danych. Liczność wpisów w ukończonej sesji wymaga doprecyzowania w OPEN-08. Pełne tabele pól oraz planowane constraints są w [database](../database.md).
