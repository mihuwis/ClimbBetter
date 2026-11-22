# Konwencje projektu

Aktualizacja: 2026-11-09. Źródła: [decyzje](../documentation/decisions.md), aktualny kod i model v3.

## Nazwy i odpowiedzialności

Dokumentacja po polsku; kod i nowe nazwy plików po angielsku (`kebab-case`). UI po angielsku. Java: `pl.climbbetter`, moduły `identity`, `catalog`, `training`, `reporting`, `shared`; implementacje w `internal`.

| Nazwy                                                                   | Znaczenie                                                    |
| ----------------------------------------------------------------------- | ------------------------------------------------------------ |
| `TrainingSession`, `TrainingEntry`                                      | sesja i pojedyncza próba/przejście/rozgrzewka                |
| `Area`, `Sector`, `Climb`                                               | miejsce, opcjonalny sektor, stabilna tożsamość wspinaczki    |
| `climbType`, `effortProfileCode`                                        | dyscyplina i profil wysiłku                                  |
| `totalMoves`, `executedMoves`                                           | długość wspinaczki i faktyczna praca                         |
| `resultType`, `ascentMode`, `familiarityBand`, `protectionMode`         | rezultat, deklarowany styl, znajomość z historii, asekuracja |
| `gradeIndex`, `gradePoints`, `gradeCalibrationVersion`                  | porządek w skali, kalibracja loadu i jej wersja              |
| `profileIntensityIndex`, `moveIntensity`, `classicLoad`, `adjustedLoad` | waga profilu i jawnie nazwane metryki                        |
| `sessionDate`, `timeZoneId`, `entryOrder`                               | lokalny dzień, strefa IANA, kolejność domenowa               |
| `clientSessionId`, `clientEntryId`, `clientClimbId`, `clientAreaId`     | stabilne UUID klienta do ponawiania operacji                 |
| `calculationModelVersion`, `version`                                    | wersja obliczeń i wersja rekordu do ochrony edycji           |

Backend wylicza i zapisuje wyniki; klient utrzymuje szkic i formatuje odpowiedzi. Komendy, modele domenowe, zapis SQL i modele widoku mają osobne role. Nazwa obiektu nie zastępuje UUID.

## Historia i precyzja

Obowiązuje [model obciążenia](../business/grading-and-scoring.md): dziesiętne obliczenia, wersje i snapshoty. EDL dotyczy wyłącznie historii v1; nowe zapisy i edycje używają v3. Poziom odniesienia nie zmienia rzeczywistej wyceny. GET i identyczny retry nie przeliczają historii; edycja nie uruchamia kaskady późniejszych sesji. Lokalny dzień nie przesuwa się po zmianie strefy urządzenia.

## Dokumentacja

- Jeden opis reguły; pozostałe dokumenty linkują do niego. Datę i status podajemy krótko, wyniki testów wraz z datą wykonania.
- Zachowujemy ID decyzji i historię zastąpionych ustaleń. Pytania trafiają do `decisions.md`, bieżące zadania do `priorities.md`.
- Tabele Markdown mają pionowo wyrównane `|` i szerokości dopasowane do pełnej treści komórek, także linków. Nie zmieniamy historycznych kopii dla formatowania.
- Sekrety, tokeny i prywatne kopie danych pozostają poza Git i dokumentacją.
