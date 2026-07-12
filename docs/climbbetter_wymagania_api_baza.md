# ClimbBetter – wymagania dla API i bazy danych treningowych

## 1. Cel dokumentu

Celem pierwszej wersji systemu jest zastąpienie arkusza Excel aplikacją webową, która umożliwia:

- tworzenie sesji treningowych,
- szybkie dodawanie kolejnych prób,
- rejestrowanie dróg, baldów i obwodów,
- automatyczne obliczanie punktów,
- obliczanie `Classic Load`,
- obliczanie `Adjusted Load`,
- obliczanie średniej intensywności ruchu,
- zachowanie pełnej historii treningowej.

Najważniejsza zasada:

> Użytkownik wprowadza dane treningowe, a wszystkie punkty, mnożniki i podsumowania oblicza API.

---

## 2. Sesja treningowa – `TrainingSession`

Jedna sesja odpowiada jednemu treningowi, np. treningowi z 5 lipca 2026 w Bronksie.

### Pola podstawowe

| Pole | Typ | Wprowadzane przez użytkownika | Znaczenie |
|---|---|---:|---|
| `Id` | UUID | nie | Identyfikator sesji |
| `UserId` | UUID | nie | Właściciel sesji |
| `SessionDate` | date | tak | Data treningowa bez godziny i strefy czasowej |
| `LocationId` | UUID | tak | Miejsce treningu, np. Bronx, Avatar, Garaż |
| `CycleId` | UUID, nullable | opcjonalnie | Mezocykl, np. „M1 Rozruch i redukcja” |
| `SessionName` | string | tak | Nazwa lub cel sesji |
| `Notes` | text | opcjonalnie | Warunki, samopoczucie i uwagi do treningu |
| `CreatedAtUtc` | datetime | nie | Data utworzenia rekordu |
| `UpdatedAtUtc` | datetime | nie | Data ostatniej zmiany |
| `ClientSessionId` | UUID | nie | Identyfikator potrzebny do synchronizacji offline |

### Podsumowanie sesji

Sesja powinna zwracać również pola wyliczane:

| Pole | Znaczenie |
|---|---|
| `TotalMoves` | Suma wszystkich wykonanych ruchów, również rozgrzewkowych |
| `ClassicLoad` | Suma klasycznych punktów wszystkich wpisów |
| `AdjustedLoad` | Suma skorygowanego obciążenia wszystkich wpisów |
| `AverageMoveIntensity` | Średnia wartość `MoveInt` wpisów poza rozgrzewką |
| `EntryCount` | Liczba prób lub wpisów |
| `CompletedClimbsCount` | Liczba zakończonych przejść |

Źródłem prawdy są wpisy w sesji. Podsumowania mogą być zapisane w tabeli sesji jako cache, ale po każdej zmianie wpisu API musi je ponownie przeliczyć.

---

## 3. Pojedynczy wpis treningowy – `TrainingEntry`

Jeden rekord odpowiada jednemu wierszowi z Excela: przejściu, próbie, fragmentowi obwodu albo rozgrzewce.

### Pola wprowadzane przez użytkownika

| Pole | Typ | Znaczenie |
|---|---|---|
| `Id` | UUID | Identyfikator wpisu |
| `SessionId` | UUID | Sesja, do której należy wpis |
| `ClimbId` | UUID, nullable | Istniejący bald, droga lub obwód |
| `EntryOrder` | int | Kolejność wpisu w sesji |
| `AttemptBlockId` | UUID lub int | Grupa kolejnych prób na tym samym problemie |
| `AttemptNumber` | int | Numer próby w danej grupie |
| `ClimbName` | string | Nazwa problemu lub drogi |
| `ClimbType` | enum | `Boulder`, `Route`, `Circuit` |
| `EffortProfileId` | UUID | Profil EDL użyty do obliczeń |
| `GradeId` | UUID | Wycena próby |
| `StyleId` | UUID | Styl przejścia lub próby |
| `TotalMoves` | int | Całkowita liczba ruchów |
| `ExecutedMoves` | int | Liczba rzeczywiście wykonanych ruchów |
| `IsCompleted` | bool | Czy problem został ukończony |
| `Notes` | text | Uwagi do konkretnej próby |

`ClimbId` powinno być opcjonalne. Użytkownik nie może być zmuszany do tworzenia drogi w katalogu przed rozpoczęciem treningu. Powinien móc szybko wpisać nazwę tymczasową, np. „Żółty”, „Niebieski prawy” albo „5 jakaś”.

### Pola pobierane ze słowników

| Pole | Źródło |
|---|---|
| `GradePoints` | Tabela wycen |
| `GradeIndex` | Tabela wycen |
| `CurrentLevelIndex` | Aktualny poziom użytkownika |
| `RelativeGradeDiff` | `GradeIndex - CurrentLevelIndex` |
| `RelativeEffortMultiplier` | Tabela Relative Effort |
| `StyleMultiplier` | Tabela stylów |
| `BaseEdl` | Profil wysiłku |

---

## 4. Obliczenia pojedynczego wpisu

### 4.1. Skorygowany dzielnik długości

```text
LengthDivisor = BaseEdl + 0.2 × TotalMoves
```

Przykład dla balda o ośmiu ruchach:

```text
LengthDivisor = 4 + 0.2 × 8 = 5.6
```

### 4.2. Intensywność pojedynczego ruchu

```text
MoveInt = GradePoints / LengthDivisor
```

Pełny wzór:

```text
MoveInt = GradePoints / (BaseEdl + 0.2 × TotalMoves)
```

Przykład dla balda `6B`:

```text
GradePoints = 50
BaseEdl = 4
TotalMoves = 8

MoveInt = 50 / 5.6 = 8.93
```

### 4.3. Klasyczne punkty wpisu

```text
ClassicPoints =
    GradePoints
    × StyleMultiplier
    × ExecutedMoves / TotalMoves
```

Dla rozgrzewki:

```text
ClassicPoints = 0
```

Ruchy rozgrzewkowe nadal wchodzą do `TotalMoves`.

### 4.4. Różnica względem aktualnego poziomu

```text
RelativeGradeDiff = GradeIndex - CurrentLevelIndex
```

Dla wspinacza na poziomie `6B+`, czyli z `CurrentLevelIndex = 6`:

| Wycena | GradeIndex | RelativeGradeDiff |
|---|---:|---:|
| 6A+ | 4 | -2 |
| 6B | 5 | -1 |
| 6B+ | 6 | 0 |
| 6C | 7 | 1 |
| 6C+ | 8 | 2 |

### 4.5. Skorygowane obciążenie wpisu

```text
AdjustedLoad =
    ExecutedMoves
    × MoveInt
    × StyleMultiplier
    × RelativeEffortMultiplier
```

---

## 5. Podsumowanie obciążeń sesji

### Classic Load

```text
ClassicLoad = suma ClassicPoints wszystkich wpisów
```

Jest to suma punktów uwzględniająca:

- wycenę,
- mnożnik stylu,
- udział wykonanych ruchów.

### Adjusted Load

```text
AdjustedLoad = suma AdjustedLoad wszystkich wpisów
```

Obciążenie skorygowane uwzględnia dodatkowo:

- aktualny poziom wspinacza,
- trudność próby względem jego poziomu,
- profil długości i charakter wysiłku,
- intensywność pojedynczego ruchu.

### Średnia intensywność ruchu

```text
AverageMoveIntensity = średnia MoveInt wpisów poza rozgrzewką
```

---

## 6. Snapshoty w `TrainingEntry`

Wpis treningowy nie może polegać wyłącznie na aktualnych danych słownikowych. Po zmianie wyceny, poziomu użytkownika albo mnożników stare sesje nie powinny automatycznie zmieniać wyniku.

Przy zapisie wpisu należy zachować:

| Pole snapshotu | Znaczenie |
|---|---|
| `ClimbNameSnapshot` | Nazwa użyta podczas sesji |
| `AreaNameSnapshot` | Nazwa miejsca |
| `GradeNameSnapshot` | Wycena, np. `6B+` |
| `GradePointsSnapshot` | Punkty za wycenę |
| `GradeIndexSnapshot` | Indeks wyceny |
| `CurrentLevelIndexSnapshot` | Poziom użytkownika w dniu sesji |
| `BaseEdlSnapshot` | EDL użyte w obliczeniu |
| `StyleNameSnapshot` | Nazwa stylu |
| `StyleMultiplierSnapshot` | Mnożnik stylu |
| `RelativeEffortSnapshot` | Mnożnik względnego wysiłku |
| `MoveIntSnapshot` | Obliczona intensywność ruchu |
| `ClassicPoints` | Wynik klasyczny |
| `AdjustedLoad` | Wynik skorygowany |
| `CalculationModelVersion` | Wersja modelu obliczeniowego |

`CalculationModelVersion` pozwala w przyszłości zmieniać wzory bez utraty informacji o tym, według jakich zasad policzono starsze sesje.

---

## 7. Tabela wycen – `Grade`

### Pola

| Pole | Przykład |
|---|---|
| `Id` | UUID |
| `Name` | `6B+` |
| `Points` | `55` |
| `GradeIndex` | `6` |
| `ScaleType` | `Font` lub `FrenchRoute` |
| `SortOrder` | `6` |
| `IsActive` | `true` |

### Dane początkowe

| Wycena | Punkty | GradeIndex |
|---|---:|---:|
| 4+ | 25 | 0 |
| 5 | 30 | 1 |
| 5+ | 35 | 2 |
| 6A | 40 | 3 |
| 6A+ | 45 | 4 |
| 6B | 50 | 5 |
| 6B+ | 55 | 6 |
| 6C | 60 | 7 |
| 6C+ | 65 | 8 |
| 7A | 70 | 9 |
| 7A+ | 75 | 10 |
| 7B | 80 | 11 |

Skala nie powinna kończyć się w kodzie na `7B`. Dane muszą być przechowywane w bazie, aby można było dodawać kolejne wyceny bez zmiany aplikacji.

---

## 8. Tabela trudności względnej – `RelativeEffort`

### Pola

| Pole | Znaczenie |
|---|---|
| `GradeDifference` | Różnica pomiędzy wyceną a poziomem użytkownika |
| `Multiplier` | Mnożnik obciążenia |
| `IsActive` | Czy wartość jest używana |

### Dane początkowe

| Diff | Effort |
|---:|---:|
| -6 | 0.20 |
| -5 | 0.25 |
| -4 | 0.35 |
| -3 | 0.45 |
| -2 | 0.55 |
| -1 | 0.80 |
| 0 | 1.00 |
| 1 | 1.30 |
| 2 | 1.80 |
| 3 | 2.60 |
| 4 | 3.50 |
| 5 | 5.00 |

Do ustalenia pozostaje zachowanie dla wartości mniejszych niż `-6` i większych niż `5`.

Możliwe rozwiązania:

- ograniczenie do najbliższej wartości skrajnej,
- odrzucenie wpisu,
- osobna reguła „poza skalą”.

---

## 9. Profile wysiłku – `EffortProfile`

| Profil | Base EDL |
|---|---:|
| Bald | 4 |
| Krótka droga / baldowa | 10 |
| Ciągowa | 25 |
| Wytrzymałościowa | 30 |
| Obwód | 20 |
| Rozgrzewka | 0 |

### Pola tabeli

```text
Id
Name
BaseEdl
Description
IsWarmup
IsActive
```

Profil powinien być zapisany na wpisie. Nie powinien wynikać wyłącznie z typu drogi, ponieważ dwie drogi o tej samej wycenie mogą mieć inny charakter wysiłku.

---

## 10. Style przejść i prób – `EntryStyle`

| Styl | Mnożnik | Znaczenie |
|---|---:|---|
| OS/Flash | 1.5 | Przejście od razu albo bardzo szybkie |
| RP słaba znajomość | 1.3 | Pełny RP po słabym rozeznaniu |
| RP normalny | 1.0 | RP po około 5–12 próbach w sezonie |
| RP stały | 0.75 | Próby powtarzane, znana droga lub bald |
| Rozgrzewka | 0 | Nie liczy punktów, ale liczy ruchy |
| Attempt OS/FL | 1.5 | Próba w warunkach OS lub Flash |
| Attempt słaba znajomość | 1.2 | Próba po słabym rozeznaniu |
| Attempt normalny | 0.9 | Standardowa próba RP |
| Attempt stały | 0.5 | Regularne próbowanie tego samego problemu |

### Pola tabeli

```text
Id
Name
Multiplier
Description
EntryResultType
IsWarmup
IsActive
```

`EntryResultType` powinno przyjmować wartości:

```text
Ascent
Attempt
Warmup
```

---

## 11. Poziom użytkownika – `UserDisciplineLevel`

Poziom użytkownika nie powinien być jedną wartością dla wszystkich rodzajów wspinania. Wspinacz może mieć inny poziom na baldach i inny na drogach.

| Pole | Znaczenie |
|---|---|
| `UserId` | Użytkownik |
| `Discipline` | `Boulder`, `Route`, `Circuit` |
| `GradeId` | Aktualny poziom |
| `GradeIndex` | Indeks poziomu |
| `ValidFrom` | Data rozpoczęcia obowiązywania |
| `ValidTo` | Data zakończenia obowiązywania |

Podczas tworzenia wpisu API pobiera poziom właściwy dla daty sesji i zapisuje go jako snapshot.

---

## 12. Lokacje i drogi

### `Area`

```text
Id
Name
LocationType
Country
Region
City
OwnerId
IsPublic
IsArchived
```

### `Sector`

```text
Id
AreaId
Name
IsArchived
```

Sektor jest opcjonalny.

### `Climb`

```text
Id
AreaId
SectorId nullable
Name
ClimbType
GradeId
DefaultEffortProfileId
SuggestedMoveCount
OwnerId
IsPublic
IsArchived
```

### `UserClimbStatus`

```text
UserId
ClimbId
IsFavorite
IsProject
LastUsedAt
```

Dzięki temu publiczna droga może być projektem jednego użytkownika i zwykłą drogą dla innego.

---

## 13. Reguły historii danych

Historia treningowa nie może zostać utracona.

Dlatego:

- użytkownik może edytować stare sesje,
- usunięcie drogi nie może usuwać wpisów treningowych,
- drogi i baldy powinny być archiwizowane,
- stare wpisy przechowują snapshot danych,
- zmiana nazwy lub wyceny drogi nie zmienia automatycznie historii,
- zmiana mnożników nie przelicza automatycznie starych sesji.

---

## 14. Walidacja danych

API powinno pilnować co najmniej następujących reguł:

```text
ExecutedMoves >= 0
TotalMoves > 0
ExecutedMoves <= TotalMoves
EntryOrder > 0
Grade jest wymagane poza specjalną rozgrzewką
EffortProfile jest wymagany
Style jest wymagany
SessionDate jest wymagana
LocationId jest wymagane
```

Dodatkowo:

- `ClassicPoints` nie są przyjmowane z frontendu,
- `AdjustedLoad` nie jest przyjmowany z frontendu,
- frontend nie wysyła własnego `MoveInt`,
- wszystkie wartości oblicza backend,
- każda zmiana wpisu powoduje ponowne przeliczenie sesji,
- zmiana słownika nie przelicza automatycznie historii,
- usunięcie drogi nie usuwa wcześniejszych wpisów.

---

## 15. Minimalny kontrakt API

### Przykładowe dane wejściowe

```json
{
  "climbId": null,
  "climbName": "Żółte gliszy techn",
  "climbType": "Boulder",
  "effortProfileId": "bald-profile-id",
  "gradeId": "6a-plus-id",
  "styleId": "attempt-weak-knowledge-id",
  "totalMoves": 8,
  "executedMoves": 5,
  "isCompleted": false,
  "entryOrder": 6,
  "notes": ""
}
```

### Przykładowa odpowiedź API

```json
{
  "gradePoints": 45,
  "gradeIndex": 4,
  "currentLevelIndex": 6,
  "relativeGradeDiff": -2,
  "baseEdl": 4,
  "lengthDivisor": 5.6,
  "moveIntensity": 8.0357,
  "styleMultiplier": 1.2,
  "relativeEffortMultiplier": 0.55,
  "classicPoints": 33.75,
  "adjustedLoad": 26.5179
}
```

---

## 16. Minimalny zakres MVP

Pierwsza działająca wersja powinna umożliwiać:

1. utworzenie sesji treningowej,
2. wskazanie daty i lokacji,
3. dodawanie prób w kolejności,
4. wybór wyceny,
5. wybór stylu,
6. wybór profilu EDL,
7. podanie liczby ruchów całkowitych i wykonanych,
8. automatyczne obliczenie `MoveInt`,
9. automatyczne obliczenie `ClassicPoints`,
10. automatyczne obliczenie `AdjustedLoad`,
11. pokazanie podsumowania sesji:
   - `TotalMoves`,
   - `ClassicLoad`,
   - `AdjustedLoad`,
   - `AverageMoveIntensity`,
12. edycję wcześniejszych wpisów,
13. zachowanie historii mimo zmian katalogu dróg i słowników.

To jest wystarczający zakres bazy danych i API, aby zastąpić obecny arkusz Excel i rozpocząć zbieranie rzeczywistych danych treningowych w ClimbBetter.
