# Achievement pyramid — backend handoff

Data: 2026-11-03. Status: backend i baza wdrożone, pełny zestaw 120 testów przeszedł; local ma Flyway V22. Aktualizacja 2026-11-04: frontend Statistics również wdrożony; jego osobne wyniki testów są w [raporcie frontendu](statistics-frontend.md). Źródła reguł: [achievement-pyramid](../business/achievement-pyramid.md), DEC-045 i OPEN-20 w [rejestrze](decisions.md).

## Zakres

Raport obsługuje trzy dyscypliny, dwa okresy i dwa style. Wędka nigdy nie jest osiągnięciem; zachowuje ruchy, intensywność, load i historię kontaktów. Filtry są stosowane przed wyborem jednego pierwszego kwalifikującego przejścia per climbId. Wybrana wycena pochodzi ze snapshotu, nie aktualnego katalogu. Katalogowa archiwizacja nie usuwa dawnego osiągnięcia.

Ważony Pyramid Support działa informacyjnie w raporcie dla pojedynczych skal. Nie zmieniono ClimberLevelResolver ani poziomu używanego do loadu. Pozostała część OPEN-20 dotyczy jego późniejszej integracji, historii wyłącznie treningowej i ewentualnego wspólnego poziomu punktowego.

## Odczyt piramidy

```http
GET /api/v1/statistics/pyramid?view=ROUTE_AND_CIRCUIT&period=LAST_TWO_YEARS&style=RP&asOf=2026-11-03
```

Parametry:

- `view`: `BOULDER` (domyślnie), `ROUTE_AND_CIRCUIT`, `COMBINED`;
- `period`: `LAST_TWO_YEARS` (domyślnie), `ALL_TIME`;
- `style`: `RP` (domyślnie, obejmuje RP/OS/Flash), `OS` (wyłącznie OS);
- `asOf`: data ISO; opcjonalnie domyślnie dzień według wstrzykniętego Clock UTC. Frontend powinien podać własną bieżącą lokalną datę, aby uniknąć różnicy dnia przy północy. Zakresy są liczone po sessionDate, włącznie z obiema granicami; ostatnie dwa lata zaczynają się od asOf.minusYears(2). All-time również kończy się na asOf. Obsługiwane lata daty odniesienia to 1–9999.

Odpowiedź `AchievementPyramid` zawiera:

- `filters`, `windowStart` (null dla all-time), `windowEnd`, `uniqueClimbCount`;
- `layers`: uporządkowane warstwy z `bucketKey`, `gradeLabel`, `gradeIndex`, `gradePoints`, `gradeCalibrationVersion`, `climbCount`;
- `peak`: najwyższa wycena/punkty w wybranej reprezentatywnej próbce piramidy; null przy pustej próbce;
- `levelAssessment`: wynik estymatora dla baldów albo dróg/obwodów;
- `gradeCalibrationVersions` oraz nullable `assessmentUnavailableReason` i `peakUnavailableReason`.

Pojedyncza skala używa etykiety i indeksu, a punkty/wersja warstwy i peak są null. Klucz ma postać `grade:BOULDER_FONT:9` albo `grade:ROUTE_FRENCH:15`. Raport zachowuje puste stopnie słownika pomiędzy najłatwiejszą i najtrudniejszą osiągniętą wyceną; warstwa z climbCount=0 jest poprawna.

COMBINED używa punktów i kalibracji, a gradeLabel/gradeIndex warstwy i peak są null. Przykładowe klucze: `points:1:90`, `points:1:92.5`. Punkty są porównywane dokładnie, bez zaokrąglania: 7a i 6A+ trafiają do 90, 6B do 92,5. Różne wersje kalibracji mają oddzielne warstwy, uporządkowane najpierw po wersji, potem punktach malejąco. Przy kilku kalibracjach nie wybieramy wspólnego maksimum: peak=null i peakUnavailableReason=`MIXED_POINT_CALIBRATIONS`. W całym widoku COMBINED levelAssessment=null z assessmentUnavailableReason=`COMBINED_SCALE`.

## Estymacja

`PyramidLevelEstimator` ma publiczne, czyste API w module training; reporting wykorzystuje je bez zależności od pakietów internal. Wagi wynoszą 1/0,5/0,25, capy 2/4/8, próg 3,00. Wybierany jest najwyższy kandydat spełniający próg, nie kandydat z największą sumą support.

`levelAssessment` zawiera gradeLabel, gradeIndex, method, confidence, sampleSize, supportScore, supportAtOrAboveCount, supportMinusOneCount, supportMinusTwoCount i algorithmVersion=`pyramid-support-v1`.

- `NO_ASCENTS`: brak estymacji, confidence=`UNASSESSED`, sampleSize=0, wynik i dowody null;
- `LOW_SAMPLE_MAX`: 1–9 różnych wspinaczek, maksimum i confidence=`LOW`, support/dowody null;
- `PYRAMID_SUPPORT`: co najmniej 10 różnych wspinaczek i najwyższy kandydat ze wsparciem ≥3; confidence=`HIGH` dopiero przy support≥4,50 i dowodach we wszystkich trzech warstwach, inaczej `MEDIUM`;
- `PEAK_FALLBACK`: próbka ≥10 bez kwalifikującego kandydata, maksimum z confidence=`LOW`, support/dowody null.

Estimator używa okresu i stylu wybranego raportu. Poziom all-time ani OS nie staje się przez ten odczyt bieżącym poziomem do obliczania loadu. Brak przejść nie jest uzupełniany osiągnięciami z prób lub wędki.

Potwierdzone przykłady: 1×7a + 3×6c + 2×6b+ + 5×6b daje 6c+ i support=3,00; 5×6c + 3×7a + 1×7a+ + 2×7b + 1×7b+ daje 7b i support=3,25.

## Lista wspinaczek warstwy

```http
GET /api/v1/statistics/pyramid/climbs?view=ROUTE_AND_CIRCUIT&period=LAST_TWO_YEARS&style=RP&asOf=2026-11-03&bucketKey=grade:ROUTE_FRENCH:15&offset=0&limit=50
```

Należy przekazać te same filtry i bucketKey z raportu. `offset` domyślnie 0, `limit` domyślnie 50 i maksymalnie 100. Klucz musi pasować do rodzaju piramidy. Poprawny, pusty bucket zwraca pustą stronę z totalCount=0.

`PyramidClimbsPage`: filters, bucketKey, totalCount, offset, limit, nextOffset (null na końcu) i items. Pozycja listy zawiera climbId, climbName, areaId, areaName, climbType, gradeScaleCode, gradeLabel, gradePoints, gradeCalibrationVersion, achievedOn, ascentMode, sessionId i entryId. Daty, styl i snapshot odnoszą się do pierwszego kwalifikującego przejścia w danym widoku. Dalsze strony zachowują deterministyczną kolejność.

Użytkownik pochodzi z CurrentUserProvider. Żądania nie przyjmują dowolnego userId. Błędne filtry, daty, klucze i granice paginacji zwracają 400 ProblemDetail z code=`INVALID_PYRAMID_REQUEST`. Odczyt podsumowania oraz odczyt liczności i strony pracują w transakcji read-only REPEATABLE_READ; zmiana danych pomiędzy osobnymi żądaniami HTTP może wymagać odświeżenia raportu.

## Baza i dotychczasowe osiągnięcia

- V21 tworzy reporting.achievement_ascents oraz reporting.session_achievement_maxima. Pierwszy widok centralizuje SQL kwalifikacji i nieusuniętych sesji.
- V22 optymalizuje odczyt maksimów do skorelowanych wyszukań dla wybranych sesji zamiast globalnego GROUP BY. Wykorzystuje istniejące indeksy z session_id na początku. V21, zastosowana wcześniej, pozostała niezmieniona.
- Nowe zapisy/edycje korzystają z AchievementEligibility przy maksimum sesji i Fast RP. Personal bests odczytują kwalifikujące wpisy.
- Dashboard, Details i Calendar odczytują maksimum z projekcji; historyczne TOP_ROPE nie wygrywa maksimum. Historyczny ineligible fastRp jest prezentowany jako false. Surowe zapisy i audyt pozostają bez modyfikacji.
- Nie zmieniono znaczenia dotychczasowego personal best `rp`: to nadal dokładnie zadeklarowane RP. Szersza kategoria RP obejmująca OS/Flash należy do piramidy.

## Weryfikacja 2026-11-03

- Celowane testy: 50 bez failures/errors/skipped. Końcowy pełny zestaw z V22: **120 testów, 0 failures/errors/skipped, BUILD SUCCESS**, 33,196 s. Obejmuje granice modułów Modulith i dotychczasowe CRUD/retry/v2.
- Nowe testy: 11 estymatora, 11 kwalifikacji, 3 integracyjne kwalifikacji/historii, 13 integracyjnych przypadków piramidy (w tym wszystkie 12 kombinacji) oraz 8 przypadków kontrolera. Łącznie 46 nowych przypadków względem poprzednich 74.
- Testy obejmują snapshoty, okresy i rok przestępny, style, wędkę, obwody, powtórzenia, identyczne nazwy/różne ID, archiwalny katalog, izolację, usuwanie/edycję, dokładne punkty, mieszane kalibracje, paginację, ponad 20 sesji i oba przykłady poziomu.
- Testy uruchomiono z PostgreSQL Testcontainers w odizolowanym katalogu kompilacji o tych samych źródłach i zależnościach, ze względu na wcześniejsze nadpisywanie target/classes przez IDE. Log pełnego przebiegu: backend/target/v2-verification/pyramid-full-test.log; raporty Surefire: backend/target/v2-verification/target/surefire-reports.
- Lokalny PostgreSQL zastosował V21 i V22. Tymczasowy backend 127.0.0.1:18080 potwierdził przez HTTP wszystkie 12 kombinacji, odczyty list niepustych warstw oraz obie ścieżki w /v3/api-docs. Nie tworzono ani nie edytowano lokalnych sesji. Raport: backend/target/v2-verification/pyramid-local-http-checks.json.
- Przed/po: identyczne 19 sesji i 145 wpisów, Classic Load=12835.4207 i Adjusted Load=24460.7012. Skróty pełnych rekordów sesji, wpisów i revisions są identyczne, co obejmuje także fingerprinty i dawne snapshoty. Pliki kontrolne: pyramid-local-before.json i pyramid-local-after.json w tym samym katalogu target.
- EXPLAIN ANALYZE potwierdził zmianę globalnej agregacji na skorelowane odczyty. Pomiar dotyczy małej lokalnej historii, nie benchmarku dużej bazy; zapisano pliki pyramid-maxima-explain-before.log i pyramid-maxima-explain-after.log.

Tymczasowy serwer służył wyłącznie weryfikacji. Przed pracą nad UI należy uruchomić/zrestartować normalny backend z nowym kodem. Frontend Statistics nie został w tym przyroście zmieniony.
