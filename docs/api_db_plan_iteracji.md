# ClimbBetter - plan API i DB dla iteracji treningowej

## 1. Cel iteracji

Celem tej iteracji jest zbudowanie lokalnej bazy PostgreSQL i API, ktore zastapia mocki oraz obecny uproszczony model treningowy. API ma stac sie zrodlem prawdy dla:

- tworzenia i edycji sesji treningowych,
- szybkiego dodawania wpisow/prob,
- katalogu miejsc, sektorow i drog,
- wycen, stylow, profili wysilku i mnoznikow,
- automatycznych obliczen `Classic Load`, `Adjusted Load` i intensywnosci ruchu,
- danych potrzebnych przez aktualny `frontend-web`: Dashboard, Calendar, Areas i Session Details.

Najwazniejsza zasada domenowa pozostaje bez zmian: frontend wysyla dane treningowe, a API liczy punkty, mnozniki, podsumowania i snapshoty.

## 2. Zrodla przeanalizowane

Dokumenty:

- `docs/climbbetter_wymagania_api_baza.md`
- `docs/project_goals_02.md`
- `docs/frontend-product-design.md`

Backend:

- .NET 9, Minimal APIs, MediatR/CQRS, EF Core + Npgsql.
- Obecny schemat domyslny: `training`.
- Obecne encje: `TrainingSession`, `TrainingEntry`, `Location`, `Climb`, `Difficulty`, `Quality`.
- Obecne endpointy: sesje, dodawanie wpisow, slowniki trudnosci/jakosci, lista drog.

Frontend web:

- Widoki: Dashboard, Calendar, Areas, Session Details.
- Mocki i typy: `ProfileSummary`, `ActivityFeedItem`, `DisciplineSummaryItem`, `SessionDetails`, `SessionEntry`, `AreaSummary`.
- Brak realnej warstwy API, wszystkie widoki pracuja jeszcze na mockach.

## 3. Aktualny stan backendu

Obecny backend jest dobrym szkieletem technicznym, ale model domenowy jest jeszcze zbyt prosty wzgledem wymagan.

Co juz jest przydatne:

- struktura solution: `Api`, `Application`, `Domain`, `Infrastructure`,
- EF Core z PostgreSQL,
- CQRS przez MediatR,
- minimalne endpointy `/api/v1`,
- migracje i seed podstawowych lokacji/drog,
- relacja `TrainingSession -> TrainingEntry`.

Najwazniejsze braki:

- `TrainingSession.Date` jest `DateTime` z UTC, a docelowo potrzebujemy `SessionDate` jako sama date (`date` w PostgreSQL, `DateOnly` w C#),
- brak `UserId`, `ClientSessionId`, `CreatedAtUtc`, `UpdatedAtUtc`,
- `TrainingSession` nie ma jeszcze cache podsumowan: `TotalMoves`, `ClassicLoad`, `AdjustedLoad`, `AverageMoveIntensity`, `EntryCount`, `CompletedClimbsCount`,
- `TrainingEntry.ClimbId` jest wymagane, a powinno byc opcjonalne,
- obecne `Difficulty` trzeba zastapic lub przemodelowac jako `Grade`,
- obecne `Quality` trzeba zastapic lub przemodelowac jako `EntryStyle`,
- brakuje `EffortProfile`, `RelativeEffort`, `UserDisciplineLevel`,
- snapshoty wpisu sa za waskie,
- obecny wzor punktow rozni sie od wymagan,
- nie ma modelu `Area -> Sector -> Climb`,
- nie ma read modeli pod Dashboard, Calendar i Areas.

## 4. Potrzeby wynikajace z frontend-web

Frontend nie potrzebuje tylko CRUD-a na tabelach. Potrzebuje gotowych read modeli.

### Dashboard

Potrzebne dane:

- profil uzytkownika: nazwa, liczba sesji, aktualna seria, liczba sesji w tygodniu,
- ostatnia aktywnosc,
- feed aktywnosci: nazwa sesji, data, dyscyplina, miejsce, score/load, max grade, ruchy, odznaki/osiagniecia, notatki,
- podsumowanie dyscyplin: sesje, punkty/load, ruchy,
- proste cele: liczba treningow w tygodniu, ruchy w miesiacu.

Na MVP feed moze byc read modelem z sesji treningowych, bez prawdziwych funkcji spolecznosciowych.

### Calendar

Potrzebne dane:

- miesieczny widok dni,
- sesje w konkretnym dniu po `SessionDate`,
- suma obciazenia i ruchow per dzien,
- podsumowanie tygodnia,
- szczegoly wybranego dnia.

Kalendarz musi pracowac na `SessionDate`, bez przesuwania dat przez strefy czasowe.

### Areas

Potrzebne dane:

- lista "My Areas",
- typ miejsca: gym/crag/home wall/board/other,
- lokalizacja tekstowa,
- liczba drog,
- liczba sektorow,
- czy miejsce jest oficjalne,
- czy jest ulubione,
- czy bylo ostatnio uzywane,
- liczba projektow uzytkownika w danym miejscu.

Do tego potrzebny jest nie tylko katalog `Area`, ale tez status uzytkownika dla miejsc i drog.

### Session Details

Potrzebne dane:

- metadane sesji,
- podsumowanie sesji,
- lista wpisow w kolejnosci,
- nazwa drogi lub nazwa tymczasowa,
- wycena, styl, ruchy, punkty/load,
- notatki wpisu i notatki sesji,
- odznaki/osiagniecia wyliczane z danych sesji.

## 5. Decyzje domenowe dla tej iteracji

1. W kodzie docelowym glownym pojeciem miejsca jest `Area`, bo frontend i dokument produktowy mowia o module `Areas`. Jezeli w DTO pojawia sie jeszcze slowo `location`, traktujemy je jako pole prezentacyjne, np. `locationName`.
2. `SessionDate` to data treningowa bez czasu i strefy. W bazie: `date`. W C#: `DateOnly`.
3. `TrainingEntry.ClimbId` jest opcjonalne. Uzytkownik moze wpisac tymczasowa nazwe problemu bez tworzenia drogi w katalogu.
4. Snapshoty sa obowiazkowe. Zmiana wyceny, stylu, poziomu uzytkownika lub nazwy drogi nie zmienia historii.
5. Drogi i miejsca archiwizujemy przez `IsArchived`, nie usuwamy fizycznie.
6. Backend nie przyjmuje z frontendu wartosci obliczonych: punktow, loadu, intensywnosci ruchu ani mnoznikow.
7. Po kazdej zmianie wpisu API przelicza wpis oraz cache podsumowan sesji.
8. Na MVP dla `RelativeEffort` poza zakresem stosujemy clamp do najblizszej wartosci skrajnej. Czyli diff mniejszy niz `-6` uzywa `-6`, a wiekszy niz `5` uzywa `5`. Decyzje zapisujemy w snapshotach przez faktycznie uzyty mnoznik.
9. Lokalnie uzywamy PostgreSQL jako glownej bazy juz teraz, nie osobnej bazy tymczasowej.
10. Do czasu auth mozemy pracowac na jednym seedowanym uzytkowniku developerskim, ale model DB i API musza miec `UserId`.

## 6. Proponowany model bazy danych

Nazwy tabel ponizej sa nazwami docelowymi w PostgreSQL. W C# mozna trzymac PascalCase dla klas, ale mapowanie EF powinno byc jawne i stabilne.

### `auth.users`

Minimalna tabela potrzebna do wlasciciela danych. Pelne logowanie moze przyjsc pozniej.

| Pole | Typ | Uwagi |
|---|---|---|
| `id` | uuid | PK |
| `display_name` | text | np. Michal |
| `email` | text nullable | przyszle auth |
| `created_at_utc` | timestamptz | |

### `training.training_sessions`

| Pole | Typ | Uwagi |
|---|---|---|
| `id` | uuid | PK |
| `user_id` | uuid | FK do `auth.users` |
| `session_date` | date | data treningowa |
| `area_id` | uuid | FK do `areas`, wymagane w MVP |
| `cycle_id` | uuid nullable | FK do `training_cycles`, moze byc null |
| `session_name` | text | nazwa/cel sesji |
| `primary_discipline` | text | `Boulder`, `Route`, `Circuit`, `Mixed`, `Training` |
| `duration_minutes` | int nullable | dla obecnego UI; `StartedAtUtc/EndedAtUtc` pozniej |
| `notes` | text nullable | notatki sesji |
| `client_session_id` | uuid | pod synchronizacje offline |
| `total_moves` | int | cache |
| `classic_load` | numeric(12,2) | cache |
| `adjusted_load` | numeric(12,2) | cache |
| `average_move_intensity` | numeric(12,4) nullable | cache |
| `entry_count` | int | cache |
| `completed_climbs_count` | int | cache |
| `max_grade_name_snapshot` | text nullable | cache dla feedu |
| `created_at_utc` | timestamptz | |
| `updated_at_utc` | timestamptz | |

Indeksy:

- `(user_id, session_date desc)`,
- `(user_id, client_session_id)` unique,
- `(area_id)`,
- `(cycle_id)`.

### `training.training_entries`

| Pole | Typ | Uwagi |
|---|---|---|
| `id` | uuid | PK |
| `session_id` | uuid | FK do `training_sessions` |
| `client_entry_id` | uuid nullable | offline sync |
| `entry_order` | int | kolejnosc w sesji |
| `attempt_block_id` | uuid nullable | grupa prob na tym samym problemie |
| `attempt_number` | int nullable | numer proby w grupie |
| `climb_id` | uuid nullable | FK do `climbs`, opcjonalne |
| `climb_name` | text | nazwa wpisana/uzyta przez usera |
| `climb_type` | text | `Boulder`, `Route`, `Circuit` |
| `effort_profile_id` | uuid | FK |
| `grade_id` | uuid nullable | wymagane poza warmup |
| `style_id` | uuid | FK |
| `total_moves` | int | calkowite ruchy |
| `executed_moves` | int | wykonane ruchy |
| `is_completed` | boolean | czy ukonczono |
| `notes` | text nullable | |
| `created_at_utc` | timestamptz | |
| `updated_at_utc` | timestamptz | |

Snapshoty i wyniki:

| Pole | Typ | Uwagi |
|---|---|---|
| `climb_name_snapshot` | text | |
| `area_name_snapshot` | text | |
| `grade_name_snapshot` | text nullable | |
| `grade_points_snapshot` | int | |
| `grade_index_snapshot` | int nullable | |
| `current_level_index_snapshot` | int nullable | poziom uzytkownika w dniu sesji |
| `base_edl_snapshot` | numeric(10,2) | |
| `style_name_snapshot` | text | |
| `style_multiplier_snapshot` | numeric(10,4) | |
| `relative_grade_diff_snapshot` | int nullable | |
| `relative_effort_multiplier_snapshot` | numeric(10,4) | |
| `length_divisor_snapshot` | numeric(12,4) | |
| `move_intensity_snapshot` | numeric(12,4) | |
| `classic_points` | numeric(12,2) | |
| `adjusted_load` | numeric(12,2) | |
| `calculation_model_version` | int | np. `1` |

Indeksy:

- `(session_id, entry_order)` unique,
- `(climb_id)`,
- `(grade_id)`,
- `(style_id)`,
- `(effort_profile_id)`,
- `(session_id, client_entry_id)` unique where `client_entry_id is not null`.

### `training.grades`

Zastepuje obecne `Difficulties`.

| Pole | Typ |
|---|---|
| `id` | uuid |
| `name` | text |
| `points` | int |
| `grade_index` | int |
| `scale_type` | text |
| `sort_order` | int |
| `is_active` | boolean |

Seed startowy zgodny z dokumentem wymagan: `4+`, `5`, `5+`, `6A`, `6A+`, `6B`, `6B+`, `6C`, `6C+`, `7A`, `7A+`, `7B`. Skala ma byc danymi w bazie, nie kodem.

### `training.relative_efforts`

| Pole | Typ |
|---|---|
| `grade_difference` | int |
| `multiplier` | numeric(10,4) |
| `is_active` | boolean |

PK: `grade_difference`.

Seed: zakres `-6..5` z dokumentu wymagan.

### `training.effort_profiles`

| Pole | Typ |
|---|---|
| `id` | uuid |
| `name` | text |
| `base_edl` | numeric(10,2) |
| `description` | text nullable |
| `is_warmup` | boolean |
| `is_active` | boolean |

Seed: Bald, Krotka droga/baldowa, Ciagowa, Wytrzymalosciowa, Obwod, Rozgrzewka.

### `training.entry_styles`

Zastepuje obecne `Qualities`.

| Pole | Typ |
|---|---|
| `id` | uuid |
| `name` | text |
| `multiplier` | numeric(10,4) |
| `description` | text nullable |
| `entry_result_type` | text |
| `is_warmup` | boolean |
| `is_active` | boolean |

`entry_result_type`: `Ascent`, `Attempt`, `Warmup`.

### `training.user_discipline_levels`

| Pole | Typ |
|---|---|
| `id` | uuid |
| `user_id` | uuid |
| `discipline` | text |
| `grade_id` | uuid |
| `grade_index` | int |
| `valid_from` | date |
| `valid_to` | date nullable |

Na start walidujemy brak nakladajacych sie zakresow w aplikacji. Pozniej mozna dodac constraint PostgreSQL na zakresach dat.

### `training.areas`

Zastepuje obecne `Locations`.

| Pole | Typ |
|---|---|
| `id` | uuid |
| `name` | text |
| `location_type` | text |
| `country` | text nullable |
| `region` | text nullable |
| `city` | text nullable |
| `owner_id` | uuid nullable |
| `is_public` | boolean |
| `is_official` | boolean |
| `is_archived` | boolean |
| `created_at_utc` | timestamptz |
| `updated_at_utc` | timestamptz |

`location_type`: `Gym`, `Crag`, `HomeWall`, `Board`, `Other`.

### `training.sectors`

| Pole | Typ |
|---|---|
| `id` | uuid |
| `area_id` | uuid |
| `name` | text |
| `is_archived` | boolean |

Sektor jest opcjonalny dla drogi.

### `training.climbs`

| Pole | Typ |
|---|---|
| `id` | uuid |
| `area_id` | uuid |
| `sector_id` | uuid nullable |
| `name` | text |
| `climb_type` | text |
| `grade_id` | uuid |
| `default_effort_profile_id` | uuid nullable |
| `suggested_move_count` | int nullable |
| `owner_id` | uuid nullable |
| `is_public` | boolean |
| `is_archived` | boolean |
| `created_at_utc` | timestamptz |
| `updated_at_utc` | timestamptz |

### `training.user_climb_statuses`

| Pole | Typ |
|---|---|
| `user_id` | uuid |
| `climb_id` | uuid |
| `is_favorite` | boolean |
| `is_project` | boolean |
| `last_used_at_utc` | timestamptz nullable |

PK: `(user_id, climb_id)`.

### `training.user_area_statuses`

Potrzebne dla `AreaSummary` w frontend-web.

| Pole | Typ |
|---|---|
| `user_id` | uuid |
| `area_id` | uuid |
| `is_favorite` | boolean |
| `last_used_at_utc` | timestamptz nullable |

PK: `(user_id, area_id)`.

### `training.training_cycles`

Minimalny fundament pod przyszly Cycle View.

| Pole | Typ |
|---|---|
| `id` | uuid |
| `user_id` | uuid |
| `name` | text |
| `start_date` | date |
| `end_date` | date |
| `notes` | text nullable |
| `created_at_utc` | timestamptz |
| `updated_at_utc` | timestamptz |

W MVP `cycle_id` w sesji moze byc null.

## 7. Model obliczen

Obliczenia powinny byc wydzielone do serwisu aplikacyjnego, np. `TrainingLoadCalculator`, z testami jednostkowymi.

### Dane wejsciowe wpisu

- `Grade.Points`
- `Grade.GradeIndex`
- aktualny `UserDisciplineLevel` dla `SessionDate` i dyscypliny
- `EffortProfile.BaseEdl`
- `EntryStyle.Multiplier`
- `RelativeEffort.Multiplier`
- `TotalMoves`
- `ExecutedMoves`
- `IsWarmup`

### Wzory

```text
LengthDivisor = BaseEdl + 0.2 * TotalMoves
MoveIntensity = GradePoints / LengthDivisor
ClassicPoints = GradePoints * StyleMultiplier * ExecutedMoves / TotalMoves
RelativeGradeDiff = GradeIndex - CurrentLevelIndex
AdjustedLoad = ExecutedMoves * MoveIntensity * StyleMultiplier * RelativeEffortMultiplier
```

Dla rozgrzewki:

```text
ClassicPoints = 0
AdjustedLoad = 0
MoveIntensity = 0
```

Ruchy rozgrzewkowe nadal wchodza do `TotalMoves` sesji, ale rozgrzewka nie wchodzi do `AverageMoveIntensity`.

### Przeliczanie sesji

Po kazdym `create/update/delete` wpisu API przelicza:

- `total_moves`,
- `classic_load`,
- `adjusted_load`,
- `average_move_intensity`,
- `entry_count`,
- `completed_climbs_count`,
- `max_grade_name_snapshot`.

## 8. Proponowane API v1

Wszystkie endpointy ponizej zakladaja `UserId` z auth/claims. Do czasu auth mozemy uzyc jednego dev-usera albo naglowka developerskiego tylko lokalnie.

### Slowniki

```http
GET /api/v1/grades
GET /api/v1/entry-styles
GET /api/v1/effort-profiles
GET /api/v1/relative-efforts
```

Minimalne DTO:

- `id`,
- `name`,
- pola sortowania,
- mnozniki/punkty potrzebne do UI,
- `isActive`.

Frontend moze wyswietlac punkty/mnozniki informacyjnie, ale nie moze ich wysylac jako wynik obliczen.

### Sesje treningowe

```http
GET /api/v1/training-sessions?from=2026-05-01&to=2026-05-31&discipline=Boulder&areaId={id}
POST /api/v1/training-sessions
GET /api/v1/training-sessions/{sessionId}
PATCH /api/v1/training-sessions/{sessionId}
```

`POST /training-sessions` request:

```json
{
  "clientSessionId": "8f075498-2037-42e5-9b54-67a54d3d7b34",
  "sessionDate": "2026-07-05",
  "areaId": "11111111-1111-1111-1111-111111111111",
  "cycleId": null,
  "sessionName": "Bronx - boulder volume",
  "primaryDiscipline": "Boulder",
  "durationMinutes": 90,
  "notes": "Good base session."
}
```

`GET /training-sessions` response item powinien pokrywac feed i kalendarz:

```json
{
  "id": "bronx-2026-06-03",
  "sessionDate": "2026-06-03",
  "sessionName": "Bronx - boulder volume",
  "primaryDiscipline": "Boulder",
  "areaId": "11111111-1111-1111-1111-111111111111",
  "areaName": "Bronx",
  "classicLoad": 587.48,
  "adjustedLoad": 640.12,
  "totalMoves": 141,
  "maxGradeName": "6B+",
  "entryCount": 15,
  "completedClimbsCount": 7,
  "notes": "Many different boulders.",
  "achievements": ["High volume session", "New RP"]
}
```

### Wpisy treningowe

```http
POST /api/v1/training-sessions/{sessionId}/entries
PATCH /api/v1/training-sessions/{sessionId}/entries/{entryId}
DELETE /api/v1/training-sessions/{sessionId}/entries/{entryId}
```

Request:

```json
{
  "clientEntryId": "5a87d90e-2274-4d87-b73f-cb49c20a62fb",
  "entryOrder": 6,
  "attemptBlockId": "850a622e-ec1e-4c8b-b8d6-408c5b157462",
  "attemptNumber": 2,
  "climbId": null,
  "climbName": "Zolte gliszy techn",
  "climbType": "Boulder",
  "effortProfileId": "effort-boulder-id",
  "gradeId": "grade-6a-plus-id",
  "styleId": "attempt-weak-knowledge-id",
  "totalMoves": 8,
  "executedMoves": 5,
  "isCompleted": false,
  "notes": ""
}
```

Response powinien zwracac zapisany wpis i wynik obliczen:

```json
{
  "id": "entry-id",
  "entryOrder": 6,
  "climbName": "Zolte gliszy techn",
  "gradeName": "6A+",
  "styleName": "Attempt slaba znajomosc",
  "totalMoves": 8,
  "executedMoves": 5,
  "lengthDivisor": 5.6,
  "moveIntensity": 8.0357,
  "classicPoints": 33.75,
  "adjustedLoad": 26.5179,
  "sessionSummary": {
    "totalMoves": 141,
    "classicLoad": 587.48,
    "adjustedLoad": 640.12,
    "averageMoveIntensity": 7.91,
    "entryCount": 15,
    "completedClimbsCount": 7
  }
}
```

### Dashboard

```http
GET /api/v1/dashboard/summary
GET /api/v1/activity-feed?scope=my&limit=20
```

`dashboard/summary` powinien zwracac:

- `profileSummary`,
- `latestActivity`,
- `disciplineSummary`,
- `weeklyGoalProgress`,
- `monthlyMovesProgress`.

Na MVP cele moga byc prosta konfiguracja uzytkownika albo wartosci domyslne. Nie trzeba jeszcze budowac pelnego systemu challenge'y.

### Calendar

```http
GET /api/v1/calendar/month?year=2026&month=5
GET /api/v1/calendar/week?date=2026-05-20
GET /api/v1/calendar/day?date=2026-05-20
```

`calendar/month` powinien zwracac dni z agregatami:

```json
{
  "year": 2026,
  "month": 5,
  "days": [
    {
      "date": "2026-05-20",
      "sessionCount": 1,
      "classicLoad": 125.0,
      "adjustedLoad": 140.4,
      "totalMoves": 42,
      "primaryDiscipline": "Boulder",
      "sessions": [
        {
          "id": "session-id",
          "sessionName": "Bronx Boulder Session",
          "areaName": "Bronx",
          "maxGradeName": "6B+"
        }
      ]
    }
  ]
}
```

### Areas i climbs

```http
GET /api/v1/areas?scope=my
POST /api/v1/areas
GET /api/v1/areas/{areaId}
PATCH /api/v1/areas/{areaId}
GET /api/v1/areas/{areaId}/climbs
POST /api/v1/areas/{areaId}/climbs
PATCH /api/v1/climbs/{climbId}
PATCH /api/v1/climbs/{climbId}/status
PATCH /api/v1/areas/{areaId}/status
```

`GET /areas?scope=my` response item:

```json
{
  "id": "bronx",
  "name": "Bronx",
  "type": "Gym",
  "location": "Krakow",
  "climbsCount": 42,
  "sectorsCount": 0,
  "isOfficial": false,
  "isFavorite": true,
  "recentlyUsed": true,
  "projectsCount": 3
}
```

## 9. Mapowanie obecnego backendu na model docelowy

| Obecnie | Docelowo | Komentarz |
|---|---|---|
| `Location` | `Area` | Zgodne z modulem Areas w UI |
| `Location.Area/Sector/SubArea` | `Area` + `Sector` | Sektor jako osobna tabela |
| `Difficulty` | `Grade` | Dodac `GradeIndex`, `ScaleType`, `SortOrder`, `IsActive` |
| `Quality` | `EntryStyle` | Obecne wartosci nie sa pelnym modelem stylow |
| `TrainingSession.Date` | `TrainingSession.SessionDate` | `DateOnly`/PostgreSQL `date` |
| `TrainingSession.Goal/Method` | `SessionName`, `Notes`, opcjonalnie tagi/metoda pozniej | Obecny model mozna czesciowo przeniesc |
| `TrainingEntry.ClimbId` wymagane | `ClimbId` nullable | Kluczowe dla szybkiego logowania |
| `LengthMultiplierSnapshot` | `LengthDivisorSnapshot`, `MoveIntensitySnapshot` | Nowy model obliczen |
| `PointsSnapshot` | `ClassicPoints`, `AdjustedLoad` | Rozdzielenie klasycznego i skorygowanego obciazenia |

## 10. Walidacje API

Minimalny zestaw:

- `SessionDate` wymagane,
- `AreaId` wymagane dla sesji,
- `SessionName` wymagane,
- `EntryOrder > 0`,
- `TotalMoves > 0`,
- `ExecutedMoves >= 0`,
- `ExecutedMoves <= TotalMoves`,
- `EffortProfileId` wymagane,
- `StyleId` wymagane,
- `GradeId` wymagane poza wpisem rozgrzewkowym,
- `ClimbName` wymagane, jesli `ClimbId` jest null,
- `ClimbId`, jesli podane, musi wskazywac niearchiwalna droge dostepna dla uzytkownika,
- slowniki musza byc aktywne,
- frontend nie moze wyslac `ClassicPoints`, `AdjustedLoad`, `MoveIntensity` ani mnoznikow jako zrodel prawdy.

Bledy walidacji powinny wracac jako `400` z czytelnym `ProblemDetails`.

## 11. Lokalny PostgreSQL

Proponowany setup:

- baza: `climbbetter`,
- schematy: `auth`, `training`,
- user lokalny: np. `climbbetter_dev`,
- connection string w `backend/ClimbBetter.Api/appsettings.Development.json` albo User Secrets,
- opcjonalnie `infra/docker-compose.postgres.yml` dla lokalnego Postgresa.

Minimalne komendy developerskie po implementacji:

```bash
dotnet ef database update --project backend/ClimbBetter.Infrastructure --startup-project backend/ClimbBetter.Api
dotnet run --project backend/ClimbBetter.Api
```

Do czasu pierwszych realnych danych mozna lokalnie dropnac i odtworzyc baze. Po rozpoczeciu rzeczywistego logowania treningow migracje musza byc kompatybilne wstecz i nie moga niszczyc historii.

## 12. Kolejnosc prac implementacyjnych

### Etap 1 - fundament DB

1. Ustalic finalne nazwy encji (`Area`, `Grade`, `EntryStyle`, `EffortProfile`).
2. Dodac minimalnego `User`/dev-usera.
3. Przemodelowac encje i konfiguracje EF.
4. Dodac seedy slownikow z deterministycznymi UUID.
5. Dodac migracje PostgreSQL.
6. Uruchomic lokalna baze i sprawdzic migracje.

### Etap 2 - obliczenia i zapis sesji

1. Wydzielic `TrainingLoadCalculator`.
2. Dodac testy jednostkowe wzorow.
3. Zmienic `CreateTrainingSession`.
4. Przepisac `AddTrainingEntry` na nowy kontrakt.
5. Dodac `UpdateTrainingEntry` i `DeleteTrainingEntry`.
6. Po kazdej zmianie wpisu przeliczac cache sesji.

### Etap 3 - API pod frontend-web

1. `GET /training-sessions` jako feed/lista.
2. `GET /training-sessions/{id}` jako Session Details.
3. `GET /calendar/month`, `/week`, `/day`.
4. `GET /dashboard/summary` i `/activity-feed`.
5. `GET /areas?scope=my` oraz lista climbs w area.
6. DTO dopasowane do typow frontendowych.

### Etap 4 - integracja frontend-web

1. Dodac warstwe API client w `frontend-web`.
2. Zastapic mocki w Dashboard.
3. Zastapic mocki w Calendar.
4. Zastapic mocki w Areas.
5. Zastapic mocki w Session Details.
6. Dodac podstawowe loading/error states.

### Etap 5 - utwardzenie

1. Testy integracyjne API dla create session + add entry + recalculation.
2. Indeksy i constraints.
3. ProblemDetails dla walidacji.
4. Dokumentacja endpointow w OpenAPI/Scalar.
5. Przygotowanie pod mobile offline sync: `ClientSessionId`, `ClientEntryId`, idempotencja.

## 13. Definition of Done tej iteracji

Iteracja jest skonczona, gdy:

- lokalny PostgreSQL startuje i przyjmuje migracje,
- mozna utworzyc sesje przez API,
- mozna dodac wpis bez `ClimbId`, tylko z tymczasowa nazwa,
- API liczy `MoveIntensity`, `ClassicPoints`, `AdjustedLoad`,
- sesja ma przeliczone podsumowania po kazdej zmianie wpisu,
- `GET /training-sessions/{id}` zwraca dane do Session Details,
- `GET /calendar/month` zwraca dane do Calendar,
- `GET /areas?scope=my` zwraca dane do Areas,
- Dashboard nie potrzebuje juz mockow dla glownego feedu i podsumowan,
- stare wpisy maja snapshoty i nie zaleza od aktualnego stanu slownikow,
- testy kalkulatora przechodza.

## 14. Decyzje otwarte

1. Czy `primary_discipline` w sesji ma byc wybierane przez uzytkownika, czy wyliczane z wpisow? Rekomendacja: pole edytowalne z domyslem z pierwszego wpisu.
2. Czy trzymamy `duration_minutes` juz teraz, czy czekamy na `StartedAtUtc/EndedAtUtc`? Rekomendacja: trzymac `duration_minutes` jako proste pole MVP.
3. Jak szeroka ma byc startowa skala wycen powyzej `7B`? Rekomendacja: dodac wiecej danych seed niz minimum, ale nie blokowac iteracji.
4. Czy cele dashboardowe maja miec w tej iteracji tabele `user_goals`? Rekomendacja: jeszcze nie; zaczac od agregatow i wartosci domyslnych.
5. Czy edycja slownikow ma byc dostepna przez API admina w MVP? Rekomendacja: nie; seed + migracje wystarcza na lokalny etap.

