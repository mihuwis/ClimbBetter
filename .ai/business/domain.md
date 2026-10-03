# Pojęcia i reguły domenowe

Status: konsolidacja zatwierdzonego modelu z 2026-08-15 i ustaleń web z 2026-09-22; uzupełniona decyzją użytkownika DEC-018 z 2026-10-03.

## Podstawowe pojęcia

| Pojęcie               | Znaczenie                                                              |
| --------------------- | ---------------------------------------------------------------------- |
| `User`                | właściciel prywatnych danych treningowych ---------------------------- |
| `TrainingSession`     | jeden trening z lokalną datą, strefą, czasem i uporządkowanymi wpisami |
| `TrainingEntry`       | jedna próba, przejście albo rozgrzewka, nigdy sam licznik wielu prób   |
| `Climb`               | stabilna tożsamość balda, drogi lub obwodu, niezależna od nazwy ------ |
| `Area` / `Sector` --- | miejsce i opcjonalny podział na sektory ------------------------------ |
| `Grade`               | wycena w konkretnej skali, wraz z indeksem i punktami ---------------- |
| `EffortProfile`       | charakter trudności i bazowe EDL, niezależne od typu wspinania ------- |
| `StyleRule`           | reguła wyznaczająca mnożnik dla rezultatu, trybu i znajomości -------- |
| `UserGradeReference`  | poziom odniesienia ważny w określonym czasie, osobno dla balda i drogi |
| `CalculationSnapshot` | zachowane wejścia, wartości słowników i wyniki konkretnego obliczenia  |
| `SessionSummary`      | odtwarzalne podsumowanie wpisów, nie niezależne źródło danych -------- |
| `CalculationRevision` | audyt jawnej korekty historycznych obliczeń -------------------------- |

## Sesja i próby

**Jedna sesja obejmuje jedno Area — rejon wspinaczkowy albo ściankę wspinaczkową.** To zasada biznesowa określająca sposób korzystania z aplikacji (DEC-018), a nie wyłącznie uproszczenie pierwszej implementacji.

W tej samej sesji można wspinać się na wielu drogach i baldach oraz w różnych sektorach należących do tego Area. Zmiana sektora w obrębie rejonu nie wymaga nowej sesji. Trening w innym Area zapisujemy jako osobną sesję, również jeśli odbywa się tego samego dnia. Jedno Area może mieć wiele sesji; nie utożsamiamy sesji z całym dniem ani wyjazdem obejmującym różne rejony.

Formularz wybiera miejsce dla całej sesji, a katalog i walidacja wpisów zachowują zgodność z tym miejscem. Techniczną konsekwencją jest jedno `TrainingSession.areaId`, wskazujące istniejące Area albo rozwiązany prywatny draft.

`TrainingSession` stanowi granicę atomowego zapisu. Kolejność `entryOrder` jest częścią danych: wcześniejsza próba tej samej sesji wpływa na znajomość przy następnej. Backend bierze pod uwagę właściwą wcześniejszą historię w bazie i wcześniejsze wpisy przesłanej sesji; późniejszy wpis nie staje się wcześniejszą próbą.

Sesja nie zapisuje `primaryDiscipline`; charakter jest wyprowadzany z typów wpisów. Nazwa sesji jest opcjonalna: brak nazwy powoduje utworzenie stabilnej nazwy z `sessionNameSource = GENERATED`, a ręczna zmiana daje `USER`. Notatka sesji i notatka próby są osobne.

`sessionDate` jest lokalną datą treningową. `timeZoneId` przechowuje strefę IANA, `startedAt` i `endedAt` opisują chwile absolutne. Podróż do innej strefy nie przesuwa historycznego dnia. Dokładne reguły północy, precyzji czasu i kolejności sesji wymagają rozstrzygnięć `OPEN-08` i `OPEN-12`.

## Wynik, tryb i znajomość

`resultType` ma wartości `ASCENT`, `ATTEMPT`, `WARMUP`; nie wprowadzamy równoległego `isCompleted`. Wykonanie wszystkich ruchów samo nie dowodzi czystego przejścia. OS i Flash są deklaracjami użytkownika, nie wnioskiem z braku wpisów w aplikacji.

`ascentMode` i wyprowadzony `familiarityBand` są odrębne. Znajomość opiera się na wcześniejszych kontaktach z ostatnich dwóch lat bez rozgrzewek. Fast RP używa numeru próby w całej znanej historii i nie zmienia mnożnika loadu. Dokładne progi, tabele i wzory: [grading-and-scoring](grading-and-scoring.md).

Konflikt OS/Flash z historią wymaga jawnego potwierdzenia i audytu. Zatwierdzona możliwość override nie rozstrzyga wszystkich szczegółów doboru mnożnika; luka jest zapisana jako `OPEN-10`.

## Katalog i szybkie logowanie

Struktura: `Area → opcjonalny Sector → Climb`. W v1 nie budujemy uniwersalnego drzewa geograficznego. Małe miejsce może nie mieć sektorów.

Brak katalogowej pozycji nie blokuje treningu. Sama nazwa miejsca wystarcza do prywatnego Area `DRAFT`, domyślnie ukrytego w uporządkowanym My Areas. Nowa wspinaczka otrzymuje `clientClimbId`; przy końcowym zapisie backend tworzy prywatny rekord i zwraca stabilne ID. Nazwa nie jest kluczem tożsamości. Nowe obiekty szkicu web pozostają lokalne do końcowego zapisu.

Uzupełnienie i scalanie draftów mają zachować historię oraz audyt mapowania. Przyszłe oficjalne pakiety rejonów są rozszerzeniem. Ulubione i projekty należą do relacji użytkownika z miejscem/drogą, nie do globalnego statusu wspinaczki.

Decyzja DEC-018 potwierdza jedno `TrainingSession.areaId`. Pytanie `OPEN-03` zostało zamknięte 2026-10-03; sesje obejmujące wiele Area nie należą do przyjętego modelu.

## Obliczenia i historia

Backend wylicza wyniki z faktów użytkownika, słowników, historii i poziomu odniesienia. Frontend nie dostarcza wiążących wyników. Typ wspinania wyznacza skalę i poziom: `BOULDER` korzysta z balda, `ROUTE` i `CIRCUIT` z drogi.

Poziom wspierany piramidą, najwyższa ukończona wycena, profil OS/Flash/Fast RP/RP i confidence są różnymi informacjami. Confidence objaśnia siłę dowodów; nie jest dodatkową wagą loadu.

Snapshot chroni historię przed późniejszą zmianą nazwy, wyceny, poziomu lub konfiguracji. Katalog archiwizujemy, nie usuwamy kaskadowo z treningami. Edycja dawnych sesji jest dozwolona produktowo; zmiana wpływająca na wynik i masowa reewaluacja muszą zachować poprzedni i nowy snapshot, autora, czas i powód. Nie uruchamiamy reewaluacji automatycznie przy zmianie słownika.

Schemat i pełniejsze pola: [database](../documentation/database.md). Rejestr decyzji `DEC-001`–`DEC-018`: [decisions](../documentation/decisions.md). Źródło zachowane w całości: [CB_model](../archive/source-snapshot/docs/CB_model.md); zasadę jednej sesji w jednym Area potwierdził użytkownik 2026-10-03.
