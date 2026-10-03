# ClimbBetter — model aplikacji

Status: specyfikacja modelu v1, iteracja 4 z 4 zakończona  
Data konsolidacji: 2026-08-15

## 1. Cel i granice dokumentu

Ten dokument opisuje **jakie dane istnieją w ClimbBetter**, jakie mają relacje, jak powinny być reprezentowane w PostgreSQL oraz jak zmienia się ich reprezentacja pomiędzy HTTP, warstwą aplikacji, domeną i persystencją.

Dokument nie opisuje endpointów, handlerów, serwisów ani kolejności implementacji. Te informacje należą do [CB_Backend.md](CB_Backend.md). Reguły liczenia wysiłku są opisane osobno w [CB_climbing_effort_valuation.md](CB_climbing_effort_valuation.md).

W dokumentacji używamy trzech oznaczeń:

- **ustalone** — wynika spójnie z najnowszego modelu i wcześniejszych decyzji;
- **propozycja v1** — rekomendowany model startowy, który może zostać poprawiony w następnych iteracjach;
- **otwarte** — wymaga odpowiedzi w [CB_project_decisions.md](CB_project_decisions.md), zanim utrwalimy odpowiedni fragment schematu.

## 2. Zasady nadrzędne modelu

1. Podstawowym zapisem treningu jest `TrainingSession` zawierająca uporządkowane `TrainingEntry`.
2. Jeden `TrainingEntry` opisuje jedno przejście, jedną próbę albo rozgrzewkę, a nie zbiorcze podsumowanie wielu różnych wspinaczek.
3. Dane faktyczne wprowadzone przez użytkownika są oddzielone od wartości słownikowych i wyników wyliczonych przez backend.
4. Historyczny wpis zachowuje snapshot danych i wyników użytych w chwili obliczenia.
5. Zmiana nazwy lub wyceny drogi, poziomu użytkownika albo konfiguracji modelu nie zmienia historii automatycznie.
6. Katalog drogi jest pomocny, ale jego brak nie może blokować szybkiego zapisu treningu ani działania mobile offline.
7. `ClimbType` i `EffortProfile` są różnymi pojęciami. Typ mówi, czym użytkownik się wspinał, a profil EDL opisuje charakter trudności.
8. `sessionDate` jest lokalną datą treningu. Nie wyliczamy jej ponownie ze strefy urządzenia podczas odczytu.
9. Sesja nie przechowuje `primaryDiscipline`; charakter sesji jest wyprowadzany z typów jej wpisów.
10. Encje JPA nie są kontraktem HTTP i nie są bezpośrednio serializowane do JSON.

## 3. Mapa modelu

```text
User
 ├── UserGradeReference ──> Grade
 ├── TrainingSession ─────> Area
 │    ├── TrainingEntry ──> Climb? / Grade / StyleRule / EffortProfile
 │    │    └── CalculationSnapshot
 │    └── SessionSummary
 ├── UserAreaStatus ──────> Area
 └── UserClimbStatus ─────> Climb

Area (pełne albo szybki prywatny draft)
 └── Sector (opcjonalny)
      └── Climb

Grade, StyleRule, EffortProfile i RelativeEffortBand
 └── wersjonowane dane konfiguracyjne używane do utworzenia snapshotu wpisu
```

Najważniejszym agregatem transakcyjnym jest `TrainingSession`. Katalog (`Area`, `Sector`, `Climb`) oraz słowniki mają własny cykl życia. Snapshot w sesji odcina historyczny wynik od ich późniejszych zmian.

## 4. Encje i obiekty modelu

### 4.1. `User`

Minimalna reprezentacja właściciela danych. Pełny profil i logowanie mogą zostać dodane później.

| Pole | Znaczenie | Status |
| --- | --- | --- |
| `id` | stabilny UUID użytkownika ClimbBetter | ustalone |
| `displayName` | nazwa wyświetlana | ustalone |
| `email` | opcjonalny adres do przyszłego auth | propozycja v1 |
| `externalSubject` | identyfikator z przyszłego dostawcy OIDC | propozycja v1 |
| `createdAt` | czas utworzenia | ustalone |

W środowisku lokalnym istnieje jeden użytkownik developerski o deterministycznym UUID. `userId` nie jest przyjmowany z body zwykłych requestów.

### 4.2. `UserGradeReference`

Określa poziom odniesienia wspinacza obowiązujący w danym okresie. Nie jest życiowym maksimum zapisanym na zawsze.

| Pole | Znaczenie |
| --- | --- |
| `id`, `userId` | tożsamość rekordu i właściciela |
| `discipline` | `BOULDER` albo `ROUTE`; `CIRCUIT` korzysta z `ROUTE` |
| `gradeId` | wycena będąca punktem odniesienia |
| `gradeIndex` | utrwalony indeks poziomu |
| `source` | np. `ONBOARDING`, `SESSION_INFERENCE`, `HISTORY_MODEL`, `MANUAL_OVERRIDE`, `IMPORT` |
| `validFrom`, `validTo` | okres obowiązywania; `validTo` może być puste |
| `algorithmVersion` | wersja algorytmu estymacji; puste dla ręcznego poziomu |
| `sampleSize` | liczba różnych ukończonych wspinaczek bez rozgrzewek w dwuletnim oknie |
| `confidence` | `UNASSESSED`, `LOW`, `MEDIUM` albo `HIGH`; siła dowodów, a nie poziom sportowy |
| `estimationMethod`, `supportScore` | `LOW_SAMPLE_MAX`, `PYRAMID_SUPPORT`, `ATTEMPT_FALLBACK` albo źródło deklarowane oraz wynik wsparcia |
| `supportAtOrAboveCount`, `supportMinusOneCount`, `supportMinusTwoCount` | surowe liczności warstw użytych do wyjaśnienia wyniku |
| `windowStart`, `windowEnd` | dokładne okno historii użyte przez estimator |

W onboardingu użytkownik może podać najwyższy poziom pokonany w ostatnich dwóch latach, osobno dla balda i drogi. Może też pozostawić wartość pustą. Poziom jest wtedy wyznaczany po sesji: najpierw z ukończonych przejść, a przy ich braku z konserwatywnego fallbacku jeden stopień poniżej najłatwiejszej próbowanej wyceny.

Przy 1–9 różnych ukończonych wspinaczkach poziomem jest maksimum z dwóch lat, `estimationMethod = LOW_SAMPLE_MAX` i `confidence = LOW`. Od 10 wspinaczek obowiązuje wersjonowany Pyramid Support. Dla każdego kandydata `G`:

```text
support(G) = min(uniqueClimbsAtOrAbove(G), 2) × 1.00
           + min(uniqueClimbsAt(G - 1), 4)   × 0.50
           + min(uniqueClimbsAt(G - 2), 8)   × 0.25
```

Poziomem jest najwyższy stopień `G`, nie wyższy od faktycznego maksimum, dla którego `support(G) >= 3.00`. `G - 1` i `G - 2` oznaczają kolejne indeksy właściwej skali. Próby nie podnoszą dojrzałej estymacji; są wyłącznie fallbackiem przy braku przejścia. Estymacja wyznaczona po pierwszej sesji może posłużyć do policzenia `adjustedLoad` tej samej sesji.

Confidence v1 jest wyprowadzane z zachowanych dowodów:

- `UNASSESSED` — poziom deklarowany/importowany bez oceny automatycznej;
- `LOW` — fallback z prób albo mniej niż 10 różnych przejść;
- `MEDIUM` — Pyramid Support z wynikiem od `3.00` do poniżej `4.50` albo z pustą jedną z trzech warstw;
- `HIGH` — co najmniej 10 przejść, `supportScore >= 4.50` i niepuste wszystkie trzy warstwy.

Confidence nie zmienia poziomu ani loadu. Informuje, jak mocno historia wspiera wynik. Progi należą do `algorithmVersion`, dlatego mogą być rozwijane bez utraty interpretacji starych estymacji.

Ogólny poziom dyscypliny uwzględnia wszystkie ukończone wspinaczki poza rozgrzewkami, niezależnie od `ascentMode`: OS, Flash, Fast RP i pozostałe RP dostarczają takiego samego dowodu wyceny. Styl nie jest dodatkową wagą Pyramid Support. Dla piramidy dana katalogowa wspinaczka jest dowodem tylko raz; jej kolejne przejścia nadal pozostają faktami potrzebnymi do loadu, historii i profilu stylów.

Każdy wpis zapisuje indeks, źródło i wersję poziomu użytego podczas obliczenia. Późniejsza zmiana poziomu nie zmienia automatycznie historycznego loadu.

### 4.3. `Area`

Miejsce treningu lub wspinaczki: ściana, rejon skalny, prywatny panel albo inne miejsce. Model obsługuje dwa równorzędne przepływy:

1. **Area uporządkowane** — użytkownik albo oficjalny pakiet opisuje kraj, region, rejon i opcjonalny sektor/skałę, a następnie konkretne wspinaczki.
2. **Szybka lokalizacja** — podczas sesji wystarcza nazwa, np. `Zimny Dół`. Backend zapisuje ją jako prywatny draft Area bez wymuszania kraju, regionu i sektora. Draft można później uzupełnić albo połączyć z Area użytkownika/oficjalnym.

Struktura katalogowa v1 pozostaje prosta:

```text
Area → opcjonalny Sector → Climb
```

| Pole | Znaczenie |
| --- | --- |
| `id` | UUID miejsca |
| `clientAreaId` | opcjonalny UUID klienta dla offline i idempotencji draftu |
| `name` | nazwa, np. `Bronx`, `Garaż`, `Zimny Dół` |
| `areaType` | np. `ARTIFICIAL`, `ROCK`, `OTHER` |
| `facilityType` | opcjonalnie `BOULDER_GYM`, `ROPE_GYM`, `MIXED_GYM`, `HOME_WALL`, `BOARD` |
| `accessType` | opcjonalny sposób dostępu, np. prywatny/publiczny |
| `country`, `region`, `city` | np. Polska, Jura Południowa; opcjonalne dla draftu |
| `displayLocation` | gotowy, stabilny opis lokalizacji do prostego MVP |
| `ownerId` | właściciel prywatnego miejsca; puste dla danych wspólnych |
| `resolutionStatus` | `DRAFT`, `RESOLVED` albo `MERGED` |
| `sourceType` | `USER`, a w przyszłości również `OFFICIAL_PACKAGE` |
| `sourceExternalId`, `sourceVersion` | opcjonalne dane przyszłego pobranego pakietu Area |
| `isPublic`, `isOfficial`, `isArchived` | widoczność i cykl życia |
| `createdAt`, `updatedAt`, `version` | audyt i optimistic locking |

Przykład uporządkowany: `Polska → Jura Południowa → Dolinka Bolechowicka → Abazy → Ryski nad tablicą`, gdzie Dolinka jest `Area`, Abazy opcjonalnym `Sector`, a Ryski `Climb`. Nie budujemy generycznego drzewa parent–child w v1.

Draft `Zimny Dół` może istnieć bez wiedzy, gdzie leży, i domyślnie nie pojawia się w ręcznie uporządkowanym „My Areas”. Późniejsze scalenie zachowuje sesje i client ID. W przyszłości oficjalne pakiety rejonów mogą dostarczać gotowe Area, Sector i Climb bez zmiany tego modelu.

### 4.4. `Sector`

Opcjonalny poziom grupujący wspinaczki w większym `Area`.

| Pole | Znaczenie |
| --- | --- |
| `id`, `areaId` | tożsamość i rodzic |
| `name` | nazwa sektora |
| `description` | opcjonalny opis |
| `isArchived` | ukrycie bez niszczenia historii |

Małe `Area` nie musi mieć żadnego sektora. `Climb.sectorId` jest nullable.

### 4.5. `Climb`

Stabilna tożsamość balda, drogi z liną albo obwodu.

| Pole | Znaczenie |
| --- | --- |
| `id` | UUID serwera |
| `clientClimbId` | opcjonalny UUID utworzony przez klienta offline |
| `areaId`, `sectorId` | miejsce i opcjonalny sektor |
| `name` | bieżąca nazwa katalogowa |
| `climbType` | `BOULDER`, `ROUTE` albo `CIRCUIT` |
| `gradeId` | bieżąca wycena katalogowa |
| `defaultEffortProfileId` | opcjonalna sugestia profilu, nigdy bezwarunkowe źródło wpisu |
| `suggestedMoveCount` | opcjonalna sugestia pełnej długości |
| `ownerId` | właściciel prywatnego rekordu |
| `isPublic`, `isVerified`, `isArchived` | status katalogowy |
| `createdAt`, `updatedAt`, `version` | audyt i kontrola konfliktów |

`TrainingEntry` może wskazywać `Climb`, ale zawsze zachowuje własną nazwę, wycenę i lokalizację w snapshocie.

### 4.6. `Grade`

Pozycja rozszerzalnego słownika wycen.

| Pole | Znaczenie |
| --- | --- |
| `id` | stabilny UUID |
| `label` | etykieta prezentacyjna, np. `6B+` |
| `scaleType` | `BOULDER_FONT` albo `ROUTE_FRENCH` |
| `points` | punkty obiektywne modelu |
| `gradeIndex` | porządkowy indeks do porównania poziomów |
| `sortOrder` | kolejność wyświetlania |
| `isActive` | możliwość wyboru w nowych wpisach |

Skale mają oddzielne rekordy, zakres `1–9c/9C` oraz wspólne punkty i indeksy dla odpowiadających sobie stopni. Droga `6a` i bald `6A` mają po 60 punktów, ale pozostają rozróżnialne przez skalę. Obwód używa `ROUTE_FRENCH`.

Historyczna sesja nie pobiera ponownie `points` ani `gradeIndex` z aktualnego `Grade`.

### 4.7. `EffortProfile`

Profil EDL opisujący charakter i spodziewaną długość trudnej części wspinaczki.

| Pole | Znaczenie |
| --- | --- |
| `id`, `code`, `name` | stabilna tożsamość techniczna i nazwa |
| `baseEdl` | wartość startowa używana w obliczeniu |
| `description` | podpowiedź dla użytkownika |
| `isWarmup` | czy profil oznacza rozgrzewkę |
| `isActive` | dostępność w nowych wpisach |

Profil jest wybierany dla konkretnego wpisu. Nie jest tym samym co `climbType` ani `totalMoves`.

### 4.8. Rezultat, tryb i reguła stylu

Wcześniejszy, łączony `EntryStyle` został rozdzielony na trzy niezależne informacje zapisane przy wpisie:

| Pole | Znaczenie |
| --- | --- |
| `resultType` | `ASCENT`, `ATTEMPT` albo `WARMUP` — fakt ukończenia |
| `ascentMode` | deklarowane `OS`, `FLASH` albo `RP` |
| `familiarityBand` | `FIRST_CONTACT`, `LOW`, `NORMAL` albo `ESTABLISHED`, wyprowadzane z historii |

Mnożnik pochodzi z konfiguracyjnej `StyleRule`:

| Pole `StyleRule` | Znaczenie |
| --- | --- |
| `id`, `code`, `name` | stabilna tożsamość i etykieta |
| `resultType`, `ascentMode`, `familiarityBand` | kombinacja wybierająca regułę |
| `multiplier` | wynikowy mnożnik stylu |
| `description`, `isActive` | opis i dostępność |

Taki model uniemożliwia kombinację nieukończonej próby ze stylem ukończonego RP i pozwala automatyzować znajomość bez odbierania użytkownikowi deklaracji OS/Flash/RP. Nie istnieje osobne `isCompleted`; źródłem prawdy jest `resultType`.

`familiarityBand` jest wyznaczany z liczby wszystkich wcześniejszych prób i przejść tej samej wspinaczki z ostatnich dwóch lat, bez rozgrzewek. Wcześniejszy wpis tej samej sesji już zwiększa licznik:

- `0` → `FIRST_CONTACT`;
- `1–10` → `LOW`;
- `11–20` → `NORMAL`;
- `>20` → `ESTABLISHED`.

Ukończone RP w drugiej albo trzeciej próbie w całej znanej historii tej wspinaczki tworzy pochodną informację `fastRp = true`. Ten licznik nie używa przesuwnego okna dwóch lat: dawna próba nadal wyklucza fałszywe osiągnięcie Fast RP. Jest to osiągnięcie i element profilu wspinacza, ale nie zmienia mnożnika loadu. `FLASH` pozostaje osobnym trybem/osiągnięciem, mimo że punktowo ma tę samą wagę co OS.

Deklaracja OS/Flash sprzeczna z istniejącą historią wymaga jawnego potwierdzenia. Bez potwierdzenia backend zwraca konflikt domenowy; po potwierdzeniu zapisuje deklarację wraz z liczbą wcześniejszych kontaktów i audytem override. Nie stosujemy twardej blokady, ponieważ historia może być niepełna albo wspinaczki mogły zostać błędnie połączone.

### 4.9. `RelativeEffortBand`

Konfiguracja mnożnika trudności względnej.

| Pole | Znaczenie |
| --- | --- |
| `id` | UUID |
| `minGradeDifference`, `maxGradeDifference` | zakres różnicy indeksów; dla pojedynczej wartości są równe |
| `multiplier` | mnożnik względnego wysiłku |
| `policy` | opcjonalne oznaczenie zachowania na granicy modelu |
| `isActive` | czy rekord uczestniczy w nowych obliczeniach |

Aktywne bandy nie mogą się nakładać. Dla różnicy `<= -6` obowiązuje floor `0.20`, a dla `>= 5` cap `5.00`. Rzeczywista różnica pozostaje w snapshocie; wpis poza zakresem ma `outsideCalibratedRange = true`.

### 4.10. `TrainingSession`

Aggregate root jednego treningu lub dnia/wyjazdu zapisanego jako jedna sesja.

| Grupa | Pola |
| --- | --- |
| Tożsamość | `id`, `userId`, `clientSessionId` |
| Kalendarz | `sessionDate`, `timeZoneId` |
| Czas | `durationMinutes`, opcjonalne `startedAt`, `endedAt` |
| Kontekst | `areaId`, `locationNameSnapshot`, opcjonalne `sessionName`, `sessionNameSource`, `notes` |
| Historia techniczna | `createdAt`, `updatedAt`, `version` |
| Podsumowanie | `totalMoves`, `classicLoad`, `adjustedLoad`, `averageMoveIntensity`, `entryCount`, `completedClimbsCount`, opcjonalnie `maxCompletedGrade` |

`clientSessionId` jest kluczem idempotencji i przyszłej synchronizacji offline. `areaId` wskazuje pełne Area albo automatycznie utworzony prywatny draft. Snapshot nazwy miejsca chroni historię przed późniejszym uzupełnieniem lub scaleniem katalogu.

`sessionName` jest opcjonalne. Gdy użytkownik go nie poda, powstaje stabilna nazwa w stylu `Wieczorna sesja boulderowa na Bronx`, a `sessionNameSource = GENERATED`. Nazwę można później edytować; wtedy źródło zmienia się na `USER`.

Podsumowanie jest zapisanym cache’em odtwarzalnym z wpisów, a nie niezależnym źródłem prawdy. Model periodyzacji nie dodaje kolumn do sesji v1; powstanie później jako rozszerzenie modułu planowania.

### 4.11. `TrainingEntry`

Jedna próba, jedno przejście albo rozgrzewka. Kolejność wpisów w sesji jest częścią danych.

| Grupa | Pola |
| --- | --- |
| Tożsamość | `id`, `sessionId`, opcjonalny `clientEntryId` |
| Kolejność | `entryOrder`, opcjonalne `attemptBlockId`, `attemptNumber` |
| Identyfikacja wspinaczki | opcjonalne `climbId`, `clientClimbId`, `climbName`, `climbType` |
| Klasyfikacja | `gradeId`, `effortProfileId`, `resultType`, `ascentMode`, wyprowadzone `contactCountBeforeTwoYears`, `attemptOrdinalAllTime`, `familiarityBand`, `styleRuleId` i `fastRp` |
| Fakty z próby | `totalMoves`, `executedMoves`, jednoznaczny rezultat, opcjonalne `notes` |
| Historia techniczna | `createdAt`, `updatedAt`, `version` |
| Wynik | osadzony `CalculationSnapshot` |

Wpis musi obsługiwać trzy przypadki identyfikacji wspinaczki:

1. istniejące `climbId`;
2. znane klientowi `clientClimbId`, które zostanie zmapowane na serwerze;
3. nazwa robocza i dane wystarczające do zapisania wpisu bez wcześniejszego katalogu — backend tworzy prywatną, niezweryfikowaną wspinaczkę pod pełnym albo draftowym Area i zwraca stabilne `climbId`.

`clientClimbId` zapobiega duplikatom podczas synchronizacji. Późniejsze połączenie prywatnej wspinaczki z wpisem katalogowym zachowuje historię i pozostawia audyt mapowania.

### 4.12. `CalculationSnapshot`

Niezmienny obiekt wartości osadzony w rekordzie `TrainingEntry`. Zawiera wszystko, co pozwala wyświetlić i wyjaśnić historyczne obliczenie bez odpytywania bieżących słowników.

Minimalna zawartość:

- `climbName`, `areaName` lub czytelny breadcrumb;
- `climbType`;
- `gradeLabel`, `gradePoints`, `gradeIndex`;
- `currentLevelGradeLabel` i `currentLevelIndex`;
- `effortProfileName`, `baseEdl`, `edlCount`;
- `styleRuleName`, `entryResultType`, `ascentMode`, `contactCountBeforeTwoYears`, `attemptOrdinalAllTime`, `familiarityBand`, `fastRp`, `styleMultiplier`;
- `relativeGradeDiff`, `relativeEffortMultiplier`, `outsideCalibratedRange`;
- `moveIntensity`, `classicLoad`, `adjustedLoad`;
- `calculationModelVersion`;
- opcjonalnie `calculatedAt`.

Snapshot nie jest osobnym agregatem. W PostgreSQL jest zestawem jawnych kolumn tabeli `training_entries`; ułatwia to filtrowanie, agregowanie i audyt.

### 4.13. `SessionSummary`

Obiekt wartości wyliczany wyłącznie z aktywnych wpisów sesji:

- `totalMoves` — suma wykonanych ruchów, także rozgrzewkowych;
- `classicLoad` — suma klasycznego obciążenia wpisów;
- `adjustedLoad` — suma skorygowanego obciążenia wpisów;
- `averageMoveIntensity` — średnia `moveIntensity` ważona wykonanymi ruchami ocenianych wpisów;
- `entryCount`;
- `completedClimbsCount`;
- opcjonalnie najwyższa ukończona wycena.

Pola te są zapisane w `training_sessions` jako read cache. Po zmianie wpisu cały summary jest odtwarzany w tej samej transakcji.

### 4.14. Modele późniejszych etapów

Poniższe encje są potrzebne produktowo, ale nie powinny blokować pierwszego pionowego przepływu:

| Model | Cel |
| --- | --- |
| `UserClimbStatus` | ulubiona wspinaczka, projekt, ostatnie użycie |
| `UserAreaStatus` | ulubione i ostatnio używane miejsca |
| `TrainingCycle` | zakres dat i planowanie cyklu treningowego |
| `CalculationRevision` | audyt każdej zmiany wpływającej na historyczne obliczenie |
| `UserGoal` | cele tygodniowe/miesięczne po ustaleniu reguł produktu |

## 5. Proponowana reprezentacja w PostgreSQL

Schemat jest propozycją v1. Flyway ma być jedynym źródłem DDL, a Hibernate ma wyłącznie walidować zgodność mapowania.

| Schemat i tabela | Model | Kluczowe relacje |
| --- | --- | --- |
| `identity.users` | `User` | — |
| `catalog.areas` | `Area` | opcjonalnie `owner_id → users` |
| `catalog.sectors` | `Sector` | `area_id → areas` |
| `catalog.grades` | `Grade` | — |
| `catalog.climbs` | `Climb` | `area_id`, opcjonalny `sector_id`, `grade_id`, `owner_id` |
| `catalog.user_area_statuses` | `UserAreaStatus` | złożony PK `(user_id, area_id)` |
| `catalog.user_climb_statuses` | `UserClimbStatus` | złożony PK `(user_id, climb_id)` |
| `training.style_rules` | `StyleRule` | wynikowa reguła dla rezultatu, trybu i znajomości |
| `training.effort_profiles` | `EffortProfile` | — |
| `training.relative_effort_bands` | `RelativeEffortBand` | — |
| `training.user_grade_references` | `UserGradeReference` | `user_id`, `grade_id` |
| `training.training_sessions` | `TrainingSession` + `SessionSummary` | `user_id`, `area_id` |
| `training.training_entries` | `TrainingEntry` + `CalculationSnapshot` | `session_id`, opcjonalny `climb_id`, słowniki |
| `training.calculation_revisions` | `CalculationRevision` | `entry_id`, `created_by_user_id` |
| `training.training_cycles` | `TrainingCycle` | poza schematem v1; przyszły moduł planowania |

### 5.1. Typy danych

- identyfikatory: `uuid`;
- lokalna data treningu i zakresy poziomu: `date`;
- chwile absolutne: `timestamptz`, mapowane na `Instant`;
- strefa sesji: tekstowy identyfikator IANA, mapowany na `ZoneId`;
- punkty, EDL, mnożniki i load: `numeric`, mapowane na `BigDecimal`;
- liczby ruchów i kolejność: `integer`;
- wersja do optimistic locking: `bigint`;
- typy zamknięte w kodzie: tekst z check constraintem albo stabilnym kodem, nie ordinal enuma.

### 5.2. Najważniejsze ograniczenia bazy

- unique `(user_id, client_session_id)`;
- unique `(owner_id, client_area_id)` dla niepustego client ID;
- unique `(session_id, entry_order)`;
- unique `(session_id, client_entry_id)` dla niepustego client ID;
- unique mapowanie prywatnego `(owner_id, client_climb_id)` dla niepustego client ID;
- `duration_minutes > 0`;
- `total_moves > 0` dla wpisu ocenianego;
- `executed_moves >= 0` i `executed_moves <= total_moves`;
- nieujemne cache’e sesji;
- brak fizycznego cascade delete z katalogu do historii treningowej;
- indeks `(user_id, session_date desc)`;
- indeksy FK oraz indeksy pod zapytania po `climb_id`, `area_id` i zakresie dat.

Walidacja relacji styl–wynik, aktywności słowników i nienakładających się okresów poziomu pozostaje również w domenie, ponieważ nie wszystkie te reguły są wygodne do wyrażenia prostym constraintem.

## 6. Reprezentacja w warstwach aplikacji

Ta sama informacja nie powinna być jednym współdzielonym obiektem w całej aplikacji.

| Warstwa | Reprezentacja | Co zawiera |
| --- | --- | --- |
| HTTP input | rekord request DTO | identyfikatory oraz fakty wprowadzone przez klienta; bez wyników obliczeń |
| Application input | command/query criteria | dane potrzebne konkretnemu przypadkowi użycia, już niezależne od HTTP |
| Domain | encje, value objects, typowane identyfikatory | reguły spójności i pełna semantyka modelu |
| Persistence | `*JpaEntity` i repozytoria-adaptery | mapowanie kolumn, relacji i optimistic locking |
| Query/read model | dedykowane projekcje DTO | dokładny kształt potrzebny ekranowi, bez odtwarzania całego agregatu |
| HTTP output | response DTO | surowe wartości domenowe i snapshoty, bez encji JPA |

### 6.1. Dane przyjmowane dla sesji

Ustalenie web z 2026-09-22: rejestrowana sesja pozostaje lokalnym szkicem frontendu do zakończenia; `Start` nie tworzy sesji w bazie backendu. Zapis przyjmuje całą ukończoną sesję. Przy obliczaniu każdego wpisu historia obejmuje wcześniejsze rekordy w bazie oraz wcześniejsze wpisy tej samej przesłanej sesji. Ewentualny podgląd obliczeń nie zapisuje szkicu do bazy. Szczegóły i propozycje niezawodności: [pierwszy etap web](CB_web_stage_1.md).

Klient przekazuje m.in.:

- `clientSessionId`;
- `sessionDate`, `timeZoneId`;
- `areaId` albo szybki draft miejsca (`clientAreaId`, `locationName`), który backend rozwiązuje do `areaId`;
- nazwę/cel sesji, czas i notatkę;
- uporządkowaną listę wpisów.

Nie przekazuje `userId`, cache’u summary ani dyscypliny głównej.

### 6.2. Dane przyjmowane dla wpisu

Klient przekazuje m.in.:

- client ID i kolejność;
- identyfikator katalogowy albo dane robocze wspinaczki;
- `climbType`, wycenę, profil, rezultat i tryb OS/Flash/RP;
- pełną oraz wykonaną liczbę ruchów;
- jednoznaczną informację o wyniku;
- opcjonalną notatkę.

Klient nie przekazuje jako źródła prawdy punktów, indeksów, EDL base, mnożników, `MoveIntensity`, `ClassicLoad` ani `AdjustedLoad`.

### 6.3. Model domenowy a model JPA

Rekomendacja dla ClimbBetter:

- domenowe `TrainingSession` i `TrainingEntry` pozostają czystymi klasami bez adnotacji HTTP;
- persystencja ma osobne `TrainingSessionJpaEntity` i `TrainingEntryJpaEntity` albo bardzo wyraźnie izolowane mapowanie;
- adapter repozytorium mapuje agregat do encji JPA i z powrotem;
- query side może mapować SQL/JPA bezpośrednio do read DTO, gdy nie zmienia domeny;
- obiekty wartości, np. `SessionDate`, `MoveCount`, `CalculationSnapshot`, nie przeciekają do JSON w przypadkowym kształcie.

To rozdzielenie kosztuje kilka jawnych mapperów, ale chroni model obliczeniowy przed wymaganiami Hibernate i kształtem ekranów.

## 7. Modele odczytowe nie są encjami domenowymi

Dashboard, Calendar, Areas i Session Details potrzebują innych przekrojów tych samych danych. Nie tworzymy dla nich nowych encji tylko dlatego, że ekran ma osobny DTO.

Przykładowe read modele:

- `TrainingSessionDetailsView` — sesja, summary i wpisy w kolejności;
- `TrainingSessionListItemView` — skrót sesji dla Board/feedu;
- `CalendarDayView` — agregaty sesji według `sessionDate`;
- `DashboardSummaryView` — przekrój użytkownika i zakresu dat;
- `ClimbingStyleProfileView` — ogólny poziom i peak grade oraz najwyższe OS, Flash, Fast RP i RP Max z sample size, confidence i różnicami indeksów;
- `AreaSummaryView` — miejsce z licznikami i statusem użytkownika.

Read modele mogą zawierać pola wyprowadzone, np. charakter sesji `MIXED`, ale nie zapisujemy ich automatycznie jako nowe źródło prawdy.

`ClimbingStyleProfileView` nie tworzy czterech konkurencyjnych poziomów używanych do wyceny loadu. `UserGradeReference` pozostaje jednym ogólnym poziomem na dyscyplinę. Profil zachowuje osobno `peakOsGrade`, `peakFlashGrade`, `peakFastRpGrade`, `peakRpGrade`, numer prób przy najlepszym RP oraz różnice `fastRpVsOsGap`, `maxRpVsOsGap` i `maxRpVsFastRpGap`. Dzięki temu ten sam sezon może pokazać dobry ogólny poziom, ale mały zysk z projektowania albo słabsze OS.

## 8. Historia, edycja i archiwizacja

- `Area`, `Sector`, `Climb` i pozycje słowników są dezaktywowane lub archiwizowane zamiast fizycznie usuwane.
- `TrainingEntry` pozostaje czytelny dzięki snapshotowi nawet po archiwizacji powiązanego `Climb`.
- Każda edycja starego wpisu wpływająca na obliczenie zapisuje pełny poprzedni i nowy snapshot, użytkownika, czas i typ/powód operacji.
- Masowa reewaluacja po korekcie wyceny jest osobną, jawną operacją i również pozostawia `CalculationRevision`.
- Zmiana słownika nigdy automatycznie nie uruchamia reewaluacji.

## 9. Elementy świadomie poza modelem v1

- globalny społecznościowy katalog wszystkich dróg;
- komentarze, obserwowanie, feed znajomych i partnerzy;
- pełny model celów, wyzwań i achievementów;
- fizjologia, regeneracja, sen i samopoczucie jako czynniki automatycznie zmieniające load;
- osobna baza odczytowa i event sourcing;
- pełna ontologia geograficzna świata;
- periodyzacja, makro-/mezo-/mikrocykle i planer; architektura pozostawia na nie osobny przyszły moduł bez pól v1 w sesji.

## 10. Kryteria gotowości modelu po czterech iteracjach

Model można uznać za gotowy do implementacji, gdy:

- decyzje `DEC-006` i `DEC-008` są przeniesione do modelu;
- każda wartość wejściowa, słownikowa, wyliczona i historyczna ma jednoznacznego właściciela;
- schemat nie ma dwóch pól opisujących to samo pod różnymi nazwami;
- dane potrzebne przez React i Flutter można zbudować bez łamania historii;
- golden cases modelu wyceny są policzone ręcznie i zatwierdzone;
- pełne i draftowe Area, `Area → Sector? → Climb` oraz późniejsze merge są jednoznaczne;
- jest jasne, co jest zapisywane, co wyliczane przy odczycie, a co jest tylko DTO ekranu.
