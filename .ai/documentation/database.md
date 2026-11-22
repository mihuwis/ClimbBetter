# Baza danych

Aktualizacja: 2026-11-15. Źródło schematu: [migracje Flyway](../../backend/src/main/resources/db/migration). Kod zawiera V1–V25; lokalne wdrożenie V25 potwierdzono 2026-11-15. Porównanie sum kontrolnych przed migracją i po niej potwierdziło zachowanie danych.

## Model wdrożony

| Tabela / widok                                           | Zawartość                                                                      |
| -------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `identity.users`                                         | wewnętrzny UUID, nazwa, znacznik ukończenia profilu                            |
| `identity.user_identities`                               | powiązanie użytkownika z issuer/subject Firebase                               |
| `catalog.areas`, `catalog.sectors`                       | miejsca, opcjonalne sektory, właściciel/widoczność i metadane                  |
| `catalog.climbs`                                         | stabilne wspinaczki, skala/wycena, profil, metry i sugestia ruchów             |
| `catalog.grades`, `catalog.grade_aliases`                | kanoniczne skale/punkty oraz wersjonowane aliasy wejściowe                     |
| `training.effort_profiles`                               | aktywne profile i `profile_intensity_index`                                    |
| `training.style_rules`, `training.relative_effort_rules` | wersjonowane mnożniki                                                          |
| `training.training_sessions`                             | właściciel, Area, data/strefa/czasy, summary, fingerprint, version, deleted_at |
| `training.training_entries`                              | uporządkowane fakty i płaski snapshot obliczeń każdego wpisu                   |
| `training.training_session_revisions`                    | audyt edycji/usunięcia, powód, stan przed/po                                   |
| `reporting.achievement_ascents`                          | aktywne kwalifikujące osiągnięcia, bez wędki                                   |
| `reporting.session_achievement_maxima`                   | maksima sesji do odczytów, także dawnej historii                               |

`CalculationSnapshot` i `SessionSummary` opisują role danych, nie osobne tabele. `UserGradeReference` oraz moduł planowania nie mają wdrożonych tabel. [ERD](diagrams/data-model.md).

## Snapshoty i wersje

- Wpis zachowuje wycenę/punkty/kalibrację, profil, rezultat/styl/znajomość, kontakty, ruchy, mnożniki, poziom i oba loady. Zmiana katalogu nie przepisuje tych wartości.
- V16–V20: waga profilu, crux, wycofanie krótkiej ciągowej. V1 zachowuje pola EDL; nowe v2/v3 mają je puste i zapisują `profile_intensity_index`. Crux zapisuje liczniki, intensywności oraz etykiety części.
- V21–V22: widoki osiągnięć i optymalizacja maksimów. Odczyt eliminuje historyczne TOP_ROPE z osiągnięć bez zmiany surowych snapshotów.
- V23: `effective_current_level_index`, `effective_relative_grade_diff`, `level_reference_source`, `level_reference_algorithm_version`, `level_reference_horizon_days`, `level_reference_sample_size`, `level_reference_history_weight`. Wymagane dla ocenianego v3; NULL dla starszych wersji i WARMUP.
- V24: `profile_completed_at` i `user_identities`; unikalne `(issuer, subject)` oraz `(user_id, issuer)`. Dotychczasowe UUID i właściciele historii pozostają.

Load to `numeric(16,4)`, intensywność `numeric(16,10)`, mnożniki mają 4 miejsca; efektywne indeksy i wagi v3 — 10. Szczegóły obliczeń i zaokrągleń: [model](../business/grading-and-scoring.md).

## Integralność i operacje

Sesja jest zapisywana atomowo z wpisami i summary. Unikalność `(user_id, client_session_id)` i fingerprint obsługują retry; version chroni korekty. PUT zastępuje wpisy i zapisuje rewizję. DELETE ustawia `deleted_at`, zachowując wpisy i rezerwację client ID. Odczyty biznesowe pomijają usunięte sesje; nowy POST nie przywraca ich przez retry.

V25 egzekwuje [DEC-053](decisions.md): `started_at` i `ended_at` są obowiązkowe, a ograniczenie `training_sessions_user_time_no_overlap` łączy równość `user_id` z nakładaniem `tstzrange(started_at, ended_at, '[)')` przez `EXCLUDE USING gist` i rozszerzenie `btree_gist`. Obejmuje rekordy z `deleted_at IS NULL`, także przy zapisie poza API. Backend serializuje dodawanie i edycję użytkownika przez `pg_advisory_xact_lock`; błąd ograniczenia `23P01` tłumaczy na `409 SESSION_TIME_CONFLICT`. Migracja wymaga kompletnej, wolnej od kolizji historii i nie zmienia jej godzin. Lokalna kontrola przed wdrożeniem: 53 sesje z kompletnym czasem, w tym 18 aktywnych bez kolizji; 424 wpisy zachowane.

Walidacje rozróżniają WARMUP, oceniane wpisy, asekurację, wersję modelu i crux. Nie ma ogólnego zakazu `executedMoves > totalMoves`: ATTEMPT dopuszcza powtórzenia, a równość dla nowego ASCENT sprawdza aplikacja bez migracji dawnych danych.

Raport miesiąca/roku agreguje obecne snapshoty w odczycie REPEATABLE_READ, bez nowych tabel i migracji. Migracje nie przeliczają historii; [SQL restore i replay](training-history-export.md) są różnymi operacjami. `R__local_data.sql` pozostaje do oddzielenia od produkcyjnych migracji przed wydaniem.

[Uruchomienie PostgreSQL](development.md) · [Firebase](firebase-authentication-local.md) · [plan ochrony produkcji](Environments_plan.md).
