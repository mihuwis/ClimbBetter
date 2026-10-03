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

- `TrainingSession.Date` jest `DateTime` z UTC, a docelowo potrzebujemy lokalnego `SessionDate` oraz `TimeZoneId` sesji,
- brak `UserId`, `ClientSessionId`, `CreatedAtUtc`, `UpdatedAtUtc`,
- `TrainingSession` nie ma jeszcze cache podsumowan: `TotalMoves`, `ClassicLoad`, `AdjustedLoad`, `AverageMoveIntensity`, `EntryCount`, `CompletedClimbsCount`,
- `TrainingEntry.ClimbId` jest wymagane, a docelowo potrzebujemy trybu `ClimbId`/`ClientClimbId` oraz automatycznego tworzenia prywatnych drog dla offline,
- obecne `Difficulty` trzeba zastapic lub przemodelowac jako `Grade`,
- obecne `Quality` trzeba zastapic lub przemodelowac jako `EntryStyle`,
- brakuje `EffortProfile`, `RelativeEffortBand`, `UserDisciplineLevel`,
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

1. `Area`/`Location` oznacza miejsce, w ktorym odbyl sie trening albo wspinaczka. Nie jest to tylko prosta nazwa obiektu. Model musi obsluzyc dwa glowne typy miejsc:
   - rejony skalne: `kontynent -> kraj -> duzy rejon -> podrejon -> dolina/rejon lokalny -> sektor/skala -> droga/bald`,
   - sztuczne sciany: `kontynent -> kraj -> miasto -> obiekt`, gdzie obiekt moze byc boulderownia, sciana z lina albo obiekt mieszany.
2. W API i bazie trzymamy lokalna date treningowa oraz strefe czasowa sesji. `SessionDate` pozostaje kluczem kalendarza, ale sesja musi miec tez `TimeZoneId`, np. `Europe/Warsaw` albo `Asia/Tokyo`. Kalendarz grupuje po `SessionDate`, a nie po aktualnej strefie urzadzenia. Dzieki temu sesja z Japonii nie przesunie sie po powrocie do Europy.
3. Domyslny czas trwania sesji to `60` minut. Uzytkownik moze go nadpisac recznie, a przy rejestrowaniu na zywo API moze wyliczyc czas z `StartedAtUtc` i `EndedAtUtc`.
4. `TrainingEntry.ClimbId` nie powinno byc traktowane jako zwykle opcjonalne pole bez konsekwencji. Docelowo chcemy precyzyjnej historii drogi/balda: liczba prob, data przejscia, statystyki wycen i powtarzalnosc prob. Kompromis dla offline/szybkiego logowania:
   - jezeli aplikacja zna droge, wpis zapisuje stabilne `ClimbId`,
   - jezeli mobile jest offline, wpis moze uzyc `ClientClimbId` albo lokalnego klucza z cache,
   - jezeli drogi nie ma w katalogu, API po synchronizacji powinno utworzyc prywatna, niezweryfikowana droge uzytkownika z podanych danych zamiast wymagac recznego potwierdzania po fakcie,
   - pozniejsze laczenie/porzadkowanie takich drog jest opcjonalnym narzedziem, nie obowiazkowym krokiem po kazdej sesji.
5. Snapshoty sa obowiazkowe. Zmiana wyceny, stylu, poziomu uzytkownika lub nazwy drogi nie zmienia historii automatycznie.
6. Musi istniec kontrolowana reewaluacja historii. Jezeli uzytkownik przez kilka miesiecy logowal droge jako `6C`, a potem okazalo sie, ze realnie byla `7A` albo `6B`, powinien moc zaktualizowac droge i przeliczyc wybrany zakres historii. To nie jest automatyczna reakcja na kazda zmiane slownika, tylko jawna operacja uzytkownika/admina z zachowaniem informacji, ze wpis zostal przeliczony.
7. Drogi i miejsca archiwizujemy przez `IsArchived`, nie usuwamy fizycznie.
8. Frontend jest warstwa prezentacyjna. Nie przyjmuje i nie wylicza zrodlowych punktow, loadu, intensywnosci ruchu ani mnoznikow. Moze jednak przechowywac lokalnie cache slownikow i tymczasowe identyfikatory, zeby mobile dzialal szybko offline.
9. Po kazdej zmianie wpisu API przelicza wpis oraz cache podsumowan sesji.
10. `RelativeEffort` nie powinien byc zaszyty jako sztywny clamp w kodzie. Drogi bardzo latwe moga nie miec realnego wplywu treningowego, a drogi skrajnie trudne moga byc praktycznie niemozliwe. Rekomendacja: trzymac progi i mnozniki w tabeli/bandach konfiguracyjnych, tak aby mozna bylo testowac taperowanie do zera, plateau albo limit gorny bez zmiany kodu.
11. Uzywamy obecnej lokalnej bazy SQL/PostgreSQL jako glownej bazy developerskiej i bedziemy ja stopniowo zapelniac wpisami testowymi oraz migracjami. QA/Prod projektujemy pozniej.
12. Do czasu auth pracujemy na jednym seedowanym uzytkowniku developerskim, ale od razu z trwałym `UserId`. Docelowo trzeba przewidziec .NET Identity, OAuth oraz logowanie przez Google/Apple.

## 6. Proponowany model bazy danych

Nazwy tabel ponizej sa nazwami docelowymi w PostgreSQL. W C# mozna trzymac PascalCase dla klas, ale mapowanie EF powinno byc jawne i stabilne.

### `auth.users`

Minimalna tabela potrzebna do wlasciciela danych. Pelne logowanie moze przyjsc pozniej.

| Pole | Typ | Uwagi |
|---|---|---|
| `id` | uuid | PK |
| `display_name` | text | np. Michal |
| `email` | text nullable | przyszle auth |
| `identity_user_id` | text nullable | przyszle powiazanie z .NET Identity |
| `created_at_utc` | timestamptz | |

Na etapie developerskim seedujemy jednego uzytkownika z trwalym UUID. Pozniej ten rekord powinien zostac spiety z .NET Identity i providerami OAuth, np. Google/Apple.

### `training.training_sessions`

| Pole | Typ | Uwagi |
|---|---|---|
| `id` | uuid | PK |
| `user_id` | uuid | FK do `auth.users` |
| `session_date` | date | lokalna data treningowa, klucz kalendarza |
| `time_zone_id` | text | IANA time zone, np. `Europe/Warsaw`, `Asia/Tokyo` |
| `area_id` | uuid | FK do `areas`, wymagane w MVP |
| `cycle_id` | uuid nullable | FK do `training_cycles`, moze byc null |
| `session_name` | text | nazwa/cel sesji |
| `primary_discipline` | text | `Boulder`, `Route`, `Circuit`, `Mixed`, `Training` |
| `duration_minutes` | int | default `60`, uzytkownik moze nadpisac |
| `started_at_utc` | timestamptz nullable | dla sesji rejestrowanych na zywo |
| `ended_at_utc` | timestamptz nullable | dla sesji rejestrowanych na zywo |
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
| `climb_id` | uuid nullable | FK do `climbs`; docelowo wypelniane przez API po synchronizacji |
| `client_climb_id` | uuid nullable | lokalny identyfikator drogi z mobile/offline cache |
| `climb_resolution_status` | text | `Catalog`, `UserPrivate`, `UnresolvedDraft`, `Merged` |
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

### `training.entry_calculation_revisions`

Tabela audytowa dla jawnej reewaluacji historii. Nie jest potrzebna do kazdego zwyklego przeliczenia aktualnie edytowanego wpisu, ale jest potrzebna, gdy zmiana wyceny drogi lub korekta danych ma przeliczyc starsze wpisy.

| Pole | Typ | Uwagi |
|---|---|---|
| `id` | uuid | PK |
| `entry_id` | uuid | FK do `training_entries` |
| `previous_calculation_model_version` | int | |
| `new_calculation_model_version` | int | |
| `previous_classic_points` | numeric(12,2) | |
| `new_classic_points` | numeric(12,2) | |
| `previous_adjusted_load` | numeric(12,2) | |
| `new_adjusted_load` | numeric(12,2) | |
| `reason` | text | np. korekta wyceny drogi |
| `created_by_user_id` | uuid | |
| `created_at_utc` | timestamptz | |

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

### `training.relative_effort_bands`

| Pole | Typ |
|---|---|
| `id` | uuid |
| `min_grade_difference` | int nullable |
| `max_grade_difference` | int nullable |
| `multiplier` | numeric(10,4) |
| `training_effect_policy` | text |
| `is_active` | boolean |

Zamiast twardego clampowania do `-6` i `5`, trzymamy bandy konfiguracyjne. Dzieki temu mozna eksperymentowac z tym, czy bardzo latwe drogi maja mnoznik bliski `0`, a skrajnie trudne proby maja plateau, limit albo rowniez taperowanie. Seed startowy moze odwzorowac obecna tabele `-6..5`, ale kod kalkulatora nie powinien zakladac, ze ten zakres jest ostateczny.

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

Zastepuje obecne `Locations`. To jest hierarchiczne drzewo miejsc. API moze nadal zwracac uproszczone `AreaSummary`, ale baza musi umiec zapisac zarowno strukture skalna, jak i sztuczne obiekty.

| Pole | Typ |
|---|---|
| `id` | uuid |
| `parent_area_id` | uuid nullable |
| `name` | text |
| `area_type` | text | 
| `area_kind` | text |
| `facility_type` | text nullable |
| `continent` | text nullable |
| `country` | text nullable |
| `region` | text nullable |
| `subregion` | text nullable |
| `city` | text nullable |
| `owner_id` | uuid nullable |
| `is_public` | boolean |
| `is_official` | boolean |
| `is_archived` | boolean |
| `created_at_utc` | timestamptz |
| `updated_at_utc` | timestamptz |

`area_type`: `Rock`, `Artificial`, `Other`.

`area_kind` dla skal: `Continent`, `Country`, `RockRegion`, `RockSubregion`, `RockArea`, `Valley`, `Sector`, `RockFormation`.

`area_kind` dla sztucznych scian: `Continent`, `Country`, `City`, `ArtificialFacility`.

`facility_type`: `BoulderGym`, `RopeGym`, `MixedGym`, `HomeWall`, `Board`, `Other`.

Przyklad skalny:

```text
Europa -> Polska -> Jura -> Jura Poludniowa -> Dolina Bolechowicka -> Filar Abazego -> Ryski nad tablica
```

Przyklad sztucznej sciany:

```text
Europa -> Polska -> Krakow -> Bronx
```

### `training.sectors`

| Pole | Typ |
|---|---|
| `id` | uuid |
| `area_id` | uuid |
| `name` | text |
| `sector_type` | text |
| `is_archived` | boolean |

Sektor jest opcjonalny dla drogi. W modelu skalnym moze oznaczac sektor albo konkretna skale, np. `Filar Abazego`. Przy bardziej rozbudowanej hierarchii ten poziom mozna tez reprezentowac jako child w `training.areas`; w tej iteracji wazne jest, zeby API potrafilo zwrocic pelny breadcrumb miejsca.

### `training.climbs`

| Pole | Typ |
|---|---|
| `id` | uuid |
| `client_climb_id` | uuid nullable |
| `area_id` | uuid |
| `sector_id` | uuid nullable |
| `name` | text |
| `climb_type` | text |
| `grade_id` | uuid |
| `default_effort_profile_id` | uuid nullable |
| `suggested_move_count` | int nullable |
| `owner_id` | uuid nullable |
| `is_public` | boolean |
| `is_verified` | boolean |
| `is_archived` | boolean |
| `created_at_utc` | timestamptz |
| `updated_at_utc` | timestamptz |

Droga utworzona automatycznie z wpisu offline powinna byc prywatna (`owner_id = user_id`, `is_public = false`) i niezweryfikowana (`is_verified = false`). Nadal ma stabilne `id`, zeby statystyki po tej drodze dzialaly od pierwszego zapisu.

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
- `RelativeEffortBand.Multiplier`
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
GET /api/v1/relative-effort-bands
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
  "timeZoneId": "Europe/Warsaw",
  "areaId": "11111111-1111-1111-1111-111111111111",
  "cycleId": null,
  "sessionName": "Bronx - boulder volume",
  "primaryDiscipline": "Boulder",
  "durationMinutes": 60,
  "startedAtUtc": null,
  "endedAtUtc": null,
  "notes": "Good base session."
}
```

`GET /training-sessions` response item powinien pokrywac feed i kalendarz:

```json
{
  "id": "bronx-2026-06-03",
  "sessionDate": "2026-06-03",
  "timeZoneId": "Europe/Warsaw",
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
  "clientClimbId": "b825f40c-1a33-4be6-84a5-8f85b5e4ed5a",
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
  "climbId": "server-created-or-existing-climb-id",
  "climbResolutionStatus": "UserPrivate",
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
  "areaType": "Artificial",
  "facilityType": "BoulderGym",
  "location": "Krakow",
  "breadcrumb": ["Europa", "Polska", "Krakow", "Bronx"],
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
| `Location` | hierarchiczne `Area` | Miejsce treningu/wspinaczki: skaly albo sztuczna sciana |
| `Location.Area/Sector/SubArea` | `Area.parent_area_id` + opcjonalny `Sector` | Pelny breadcrumb miejsca, np. Jura -> Bolechowicka -> Filar Abazego |
| `Difficulty` | `Grade` | Dodac `GradeIndex`, `ScaleType`, `SortOrder`, `IsActive` |
| `Quality` | `EntryStyle` | Obecne wartosci nie sa pelnym modelem stylow |
| `TrainingSession.Date` | `SessionDate` + `TimeZoneId` | Lokalna data kalendarza plus strefa sesji |
| `TrainingSession.Goal/Method` | `SessionName`, `Notes`, opcjonalnie tagi/metoda pozniej | Obecny model mozna czesciowo przeniesc |
| `TrainingEntry.ClimbId` wymagane | `ClimbId` lub `ClientClimbId`/auto private climb | Offline szybkie logowanie bez utraty statystyk per droga |
| `LengthMultiplierSnapshot` | `LengthDivisorSnapshot`, `MoveIntensitySnapshot` | Nowy model obliczen |
| `PointsSnapshot` | `ClassicPoints`, `AdjustedLoad` | Rozdzielenie klasycznego i skorygowanego obciazenia |

## 10. Walidacje API

Minimalny zestaw:

- `SessionDate` wymagane,
- `TimeZoneId` wymagane,
- `AreaId` wymagane dla sesji,
- `SessionName` wymagane,
- `EntryOrder > 0`,
- `TotalMoves > 0`,
- `ExecutedMoves >= 0`,
- `ExecutedMoves <= TotalMoves`,
- `EffortProfileId` wymagane,
- `StyleId` wymagane,
- `GradeId` wymagane poza wpisem rozgrzewkowym,
- wpis musi miec jeden z identyfikatorow: `ClimbId`, `ClientClimbId` albo dane wystarczajace do utworzenia prywatnej drogi (`ClimbName`, `GradeId`, `AreaId`/`SectorId`, `ClimbType`),
- `ClimbId`, jesli podane, musi wskazywac niearchiwalna droge dostepna dla uzytkownika,
- slowniki musza byc aktywne,
- frontend nie moze wyslac `ClassicPoints`, `AdjustedLoad`, `MoveIntensity` ani mnoznikow jako zrodel prawdy.

Bledy walidacji powinny wracac jako `400` z czytelnym `ProblemDetails`.

## 11. Lokalny PostgreSQL

Obecnie uzywamy lokalnej bazy SQL/PostgreSQL jako glownej bazy developerskiej. Bedziemy ja stopniowo zapelniac wpisami testowymi i migracjami, a strategie QA/Prod ustalimy pozniej.

Proponowany setup:

- baza: `climbbetter`,
- schematy: `auth`, `training`,
- user lokalny: np. `climbbetter_dev`,
- connection string w `backend-net/ClimbBetter.Api/appsettings.Development.json` albo User Secrets,
- opcjonalnie `infra/docker-compose.postgres.yml` dla lokalnego Postgresa.

Minimalne komendy developerskie po implementacji:

```bash
dotnet ef database update --project backend-net/ClimbBetter.Infrastructure --startup-project backend-net/ClimbBetter.Api
dotnet run --project backend-net/ClimbBetter.Api
```

Na etapie pustej bazy mozna jeszcze odtwarzac schemat agresywniej. Gdy zaczniemy zbierac testowe i rzeczywiste wpisy treningowe, migracje powinny isc do przodu bez niszczenia historii, bo te dane beda materialem do walidacji modelu.

## 12. Definition of Done tej iteracji

Iteracja jest skonczona, gdy:

- lokalny PostgreSQL startuje i przyjmuje migracje,
- mozna utworzyc sesje przez API,
- sesja ma lokalne `SessionDate`, `TimeZoneId` i domyslne `DurationMinutes = 60`,
- mozna dodac wpis z `ClimbId`,
- mozna dodac wpis offline/tymczasowy z `ClientClimbId` albo nazwa, a backend tworzy lub wiaze prywatna droge bez wymagania recznego potwierdzania po kazdej sesji,
- API liczy `MoveIntensity`, `ClassicPoints`, `AdjustedLoad`,
- sesja ma przeliczone podsumowania po kazdej zmianie wpisu,
- da sie jawnie przeliczyc wybrana historie po korekcie wyceny drogi,
- `GET /training-sessions/{id}` zwraca dane do Session Details,
- `GET /calendar/month` zwraca dane do Calendar,
- `GET /areas?scope=my` zwraca dane do Areas,
- Dashboard nie potrzebuje juz mockow dla glownego feedu i podsumowan,
- stare wpisy maja snapshoty i nie zaleza od aktualnego stanu slownikow,
- testy kalkulatora przechodza.

## 13. Decyzje otwarte

1. Czy `primary_discipline` w sesji ma byc wybierane przez uzytkownika, czy wyliczane z wpisow? Rekomendacja: pole edytowalne z domyslem z pierwszego wpisu.
2. Jak dokladnie nazwac poziomy hierarchii skalnej w API: `RockRegion`, `RockSubregion`, `Valley`, `RockFormation`, czy inny zestaw? Rekomendacja: zaczac od jawnych enumow i testowac na Jurze.
3. Jak szeroka ma byc startowa skala wycen powyzej `7B`? Rekomendacja: dodac wiecej danych seed niz minimum, ale nie blokowac iteracji.
4. Jak ustawic pierwsze bandy `RelativeEffort`, zeby bardzo latwe i skrajnie trudne drogi nie falszowaly obciazenia? Rekomendacja: trzymac to w danych i porownac na realnych sesjach testowych.
5. Czy cele dashboardowe maja miec w tej iteracji tabele `user_goals`? Rekomendacja: jeszcze nie; zaczac od agregatow i wartosci domyslnych.
6. Czy edycja slownikow ma byc dostepna przez API admina w MVP? Rekomendacja: nie; seed + migracje wystarcza na lokalny etap.

