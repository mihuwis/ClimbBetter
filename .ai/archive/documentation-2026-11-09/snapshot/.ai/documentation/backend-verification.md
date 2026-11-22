# Weryfikacja backendu

Data aktualizacji: 2026-11-07. Status: model v3 i raport okresowy wdrożone, pełny zestaw **210 testów**, bez failures/errors/skipped, BUILD SUCCESS. Local pozostaje na **V23**, zapisane wyniki i katalog zachowane. Raport miesiąca/roku wymaga restartu backendu8080 do nowego kodu. Poniższe przebiegi mają własne daty i opisują także wcześniejsze wersje.

## Training summary — 2026-11-07

Pełny offline Maven przez backend/target/v2-verification/pom.xml: **210/210**, 35,181 s, exit0. Log: period-summary-full-test.log. Nowe 17 przypadków kontrolera sprawdzają kontrakt i błędy parametrów; 17 integracyjnych — agregację, pełny czas sesji mieszanych, częściowy load, średnią/max, wszystkie reguły stylów, deduplikację roku, kalendarz, ponad20sesji, snapshoty, usunięcia i izolację użytkowników.

Weryfikacja lokalnego API odbyła się na tymczasowym18081 z połączeniami Hikari tylko do odczytu i Flyway wyłączonym (bez nowej migracji). Niezależne obliczenia na 17 aktywnych sesjach/137 wpisach potwierdziły **18 raportów**, w tym puste okresy. Osobny Edge z realnym API: **1/1**, sześć widoków miesiąc/rok/dyscyplina, kafle, źródłowa sesja/powrót i telefon. Pełne hashe historii, revisions, katalogu, użytkowników i schemaVersion identyczne przed/po. Tymczasowy serwer zamknięty. [Kontrakt i dowody](period-training-summary.md).

## Właściwy replay po restarcie — 2026-11-04

Przed importem potwierdzono użytkownika002, kontrakt v3 w OpenAPI8080, V23 i active002=0. Po chronologicznym API replay wszystkie 16 sesji/133 wpisy mają v3, oryginalne wejścia, wyceny, punkty i intensywności. Loady są odtwarzalne ze snapshotów. Hashe zachowanych 35 sesji/278 wpisów/46 revisions, katalogu, wycen i innych kont nie zmieniły się. Dashboard16 i 12 wariantów piramid poprawne: RP 23 baldy + 4 drogi/obwody = 27, OS 10 + 0 = 10, identycznie dla obu okresów. Dowody: live-v3-import-verification.json i główny dziennik replay w ignorowanym folderze kopii. [Szczegóły](training-history-export.md).

## Aktualność poziomu odniesienia v3 — 2026-11-04

Wdrożono DEC-047: zanik kwadratowy 730 dni, najnowszy dowód na każde climbId, bez TOP_ROPE, oddzielne skale oraz zachowane maksimum dla próbki poniżej 10. Przy większej próbce estymator wyznacza ułamkowy poziom i interpolowany mnożnik wysiłku. Nowy load używa zaokrąglonego do 4 miejsc snapshotu mnożnika, więc można odtworzyć wynik ze zgromadzonych wartości. [Reguły i granice](recency-load.md).

Pełny Maven offline przez izolowany POM backend/target/v2-verification/pom.xml: **176/176**, exit 0, 38,815 s; log recency-full-test.log. W tym 12 przypadków wag, 13 estymatora, 7 interpolacji i 7 nowych integracyjnych. Potwierdzono wygaśnięcie na granicy 730 dni, małą próbkę, deduplikację/odświeżanie, oddzielne skale, wędkę z własnym loadem bez dowodu poziomu, wykluczenie przyszłej/późniejszej/usuniętej/cudzej historii, PUT bez własnych starych wpisów oraz zamrożone retry v1/v2/v3. Istniejące testy ograniczeń v2 nadal używają rzeczywistych legacy snapshotów; edycja v1/v2 przechodzi na v3 z audytem.

Flyway zastosował wyłącznie V23 na local. Sumy kontrolne sesji, starych pól wszystkich 278 wpisów, revisions, wspinaczek i wycen są identyczne przed/po. Nowe nullable kolumny mają NULL dla historii. Aktywnych sesji użytkownika002 nadal zero. Raport: backend/target/v2-verification/recency-local-migration-verification.json oraz kopia w prywatnym folderze backupu.

Na osobnej bazie odtworzono wyłącznie zabezpieczoną historię użytkownika002 i jej zależności, zastosowano V23, uruchomiono nowy backend na 18081 i wykonano chronologiczny API replay: **16 sesji/133 wpisy**. Wszystkie pola wejściowe, wyceny, gradePoints i intensywności zachowane; metadane v3 i obliczenia snapshotów poprawne. Źródła poziomu ocenianych wpisów: 17 CURRENT_SESSION_FALLBACK, 47 LOW_SAMPLE_MAX, 53 RECENCY_WEIGHTED_PEAK. Raport v3-replay-verification.json w ignorowanym folderze kopii. Właściwego importu na 8080 nie wykonano.

Frontend: build, lint i **24/24** testy modelu przeszły. Dodatkowo **1/1** test Edge z realnym API v3 i PostgreSQL na bazie testowej: utworzenie, odczyt, edycja, kalendarz i usunięcie sesji. Oddzielny Vite 5174 wymaga w tym teście proxy żądań z lokalnym Origin 5173; odpowiedzi pochodzą z rzeczywistego backendu, bez mocków. Pierwszy przebieg ujawnił ograniczenie CORS testowego portu, nie błąd kalkulatora. Konfiguracji CORS aplikacji nie rozszerzano. Pierwszy nowy test PUT wymagał uzupełnienia entryOrder fixture; po poprawce pełny zestaw jest zielony. Pomocniczy weryfikator Windows poprawiono na jawne UTF-8; dane API nie były uszkodzone.

Po zakończeniu weryfikacji v3 zatrzymano wyłącznie tymczasowy proces 18081 i usunięto jego bazę cb_recency_verify_…. Ponowna kontrola local potwierdziła identyczne sumy kontrolne i active002=0. Proces użytkownika8080 pozostał uruchomiony.

## Completed — ścisła liczba ruchów 2026-11-04

Po odpowiedzi użytkownika wdrożono DEC-046: ASCENT wymaga executedMoves == totalMoves, ATTEMPT zachowuje ruchy częściowe i nadmiarowe. TrainingMetricsFacade odrzuca niespójne ukończenie przed obliczeniami; dotychczasowy handler zwraca 400 INVALID_TRAINING_SESSION. Warunku nie dodano do walidacji DTO poprzedzającej obsługę retry.

Pełny offline Maven z izolowanym POM i tymi samymi źródłami: **136 testów, 0 failures/errors/skipped, BUILD SUCCESS, exit 0**, 38,338 s. Log: `backend/target/v2-verification/completed-moves-full-test.log`. Nowe 16 przypadków: 11 parametryzowanych jednostkowych (bald, droga lead/top rope, obwód; za mało/za dużo, równe ruchy, próby) i 5 integracyjnych wykonań (POST rollback dla 2/8 i 10/8, poprawne 8/8 i próby, PUT z zachowaniem pełnych rekordów/wersji/audytu, odczyt dawnego niepełnego przejścia i identyczny retry). Dwa dotychczasowe scenariusze edycji zmieniają teraz długość razem z liczbą ruchów; oczekiwania loadu przeszły.

Bez migracji i bez operacji na lokalnej historii użytkownika; integracja działa na PostgreSQL Testcontainers. Działającego backendu użytkownika nie restartowano. Po restarcie API stosuje nową regułę do nowych zapisów i edycji; istniejąca historia pozostaje czytelna.

## Piramida osiągnięć — 2026-11-03

Celowane 50 testów przeszło, a końcowy pełny przebieg z V22 zakończył się wynikiem **120 testów, 0 failures/errors/skipped, BUILD SUCCESS**, czas 33,196 s. Obejmuje 46 nowych przypadków: kwalifikację, estymator, raport i kontroler, oraz dotychczasowe 74 testy z granicami modułów. Log: `backend/target/v2-verification/pyramid-full-test.log`; standardowe źródła i zależności, izolowany katalog kompilacji jak we wcześniejszej weryfikacji v2.

Local zastosował V21 i V22. Tymczasowy serwer na 127.0.0.1:18080 potwierdził HTTP wszystkich 12 kombinacji, listy niepustych warstw i dwie ścieżki OpenAPI; po weryfikacji został zatrzymany. Nie utworzono lokalnych sesji. Pełne skróty rekordów sesji, wpisów i audytu przed/po są identyczne: 19 sesji, 145 wpisów, Classic=12835.4207, Adjusted=24460.7012. EXPLAIN potwierdził zastąpienie globalnej agregacji maksimów skorelowanym odczytem dla wybranych sesji; nie jest to benchmark dużej historii. [Szczegóły kontraktu, scenariuszy i plików dowodowych](achievement-pyramid-backend.md).

## Model v2 — zakres i dowody 2026-11-01

Implementacja obejmuje zapis POST i edycję PUT według v2, odczyt historycznych snapshotów v1, identyczny retry bez przeliczania, indeksy profili, crux w DTO i audycie oraz `calculationModelVersion` w odczytach sesji. Stary kalkulator EDL został usunięty z bieżących obliczeń; historia nie została usunięta. Testy integracyjne v2 obejmują osobne poziomy balda i liny, rozgrzewkę, podział cruxa dla przejścia i próby, odczyt v1, konwersję przy edycji, audyt oraz odrzucenie niepoprawnych snapshotów przez bazę. Weryfikują też przypadek pierwszej sesji po poprawce pustej historii w `ClimberLevelResolver`. Wzory i kalibracja: [model wyceny](../business/grading-and-scoring.md).

Pełny Maven `test` po usunięciu starych obliczeń EDL zakończył się wynikiem **74 testy, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**, w czasie 27,393 s. Użyto odizolowanego katalogu `backend/target/v2-verification`: POM zachowuje te same zależności i wskazuje rzeczywiste źródła oraz zasoby repozytorium, a zmienia katalog wyjściowy. Zapobiega to nadpisywaniu plików `.class` przez działające IDE. Log pełnego przebiegu: `backend/target/v2-verification/test.log`; raporty Surefire: `backend/target/v2-verification/target/surefire-reports`. Ten pełny przebieg dotyczy migracji do V19.

Flyway zastosował do local migracje V16–V20; zwalidowano 21 migracji, a końcowa wersja schematu to 20. V20 zmienia wyłącznie opisy profili. Dodatkowe uruchomienie `TrainingModelV2IntegrationTest` na świeżym PostgreSQL z V20 również przeszło: **9 testów, 0 failures/errors/skipped, BUILD SUCCESS**, czas 12,938 s. Log: `backend/target/v2-verification/v20-test.log`. Jest to osobny przebieg podzbioru testów, nie dodatkowe 9 różnych testów do pełnego zestawu 74. W słowniku pozostaje siedem aktywnych profili z indeksami 10/7/5, w tym `CIRCUIT = 7`; krótka ciągowa jest nieaktywna.

Porównanie danych przed V16 oraz po V19 i po V20 wykazało niezmienione dane treningowe:

- 19 sesji i 145 wpisów;
- suma Classic Load: `12835.4207`;
- suma Adjusted Load: `24460.7012`;
- hash kontrolny: `935e257a2a9d1e51da80f3b500b83a74`.

Usunięto też nieistniejącą dodatkową lokalizację `classpath:db/local-migration` z konfiguracji local; standardowy katalog `db/migration` obejmuje istniejącą lokalną migrację powtarzalną. Końcowa walidacja Flyway local potwierdziła 21 poprawnych migracji i schemat V20 bez kolejnych zmian. Migracje wykonano bez uruchamiania serwera HTTP. Przed sprawdzeniem UI użytkownik musi uruchomić lub zrestartować backend z nowym kodem. Frontend nie otrzymał zmian; nie przeprowadzono nowej weryfikacji przeglądarkowej modelu v2.

## Wcześniejsza weryfikacja — 2026-10-17

Źródła tego wcześniejszego wyniku: Maven, raporty Surefire w `backend/target/surefire-reports` i log `backend/target/backend-test-run.log`. Poniższe liczby dokumentują bramkę przed React oraz późniejsze dodanie kalendarza, a nie końcowy model v2.

## Wynik

Aktualizacja po integracji React: pełny Maven `test` ponowiono z sukcesem — **52 testy, 0 failures/errors/skipped**, 31,023 s. Log: `backend/target/frontend-integration-verification.log`. Dodatkowy test sprawdza kalendarz z 22 widocznymi sesjami, wyklucza cudzą/usuniętą, sprawdza zakres dat i bieżącą tożsamość. React potwierdzono również pełnym przebiegiem przeglądarkowym na trwałej bazie Mihu; [raport](frontend-web.md). Poniższe 51 testów dokumentuje wcześniejszą bramkę przed React.

Na polecenie użytkownika asystent sprawdził Docker (`docker version` zawierał Client i Server), a następnie wykonał pełne Maven `test`. Wynik: **51 testów, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Czas raportowany przez Maven: 24,362 s. Nie były potrzebne poprawki kodu ani testów.

Testcontainers uruchomił tymczasowy PostgreSQL `17.6-alpine`. Flyway zbudował bazy testowe do `V15`. Testy nie korzystały z trwałej bazy Compose z danymi Mihu.

## Potwierdzony zakres

- `TrainingSessionCrudIntegrationTest` (2): create → read → update → delete, przeliczenie loadu, wersja i audyt, konflikt starej wersji, rollback błędnej edycji, wykluczenie usuniętej sesji z odczytów, kontrola właściciela i prywatność katalogu.
- `TrainingSessionFlowIntegrationTest` (6): zapis sesji, Dashboard, personal bests i Details, rollback całego zapisu, identyczne retry, konflikt zmienionej treści, OS i Flash z jawnym potwierdzeniem oraz zachowaniem mnożnika 1.50 i flag audytu.
- `CatalogClimbScenariosIntegrationTest` (4): nowe i wcześniej znane wspinaczki w treningu, zapamiętywanie Climb przed treningiem bez tworzenia kontaktów, retry katalogu i konflikt klientowego ID, dostęp, niezależność identycznych nazw, sektory, metry, ręczna liczba ruchów, mapowanie Kurtyki i projekt bez wyceny.
- `ClimbMoveSuggestionTest` (2): 2 ruchy/m, pierwszeństwo ręcznej wartości, domyślne 8 ruchów balda, zaokrąglenie i odrzucenie niepoprawnej długości.
- Pozostałe testy: kalkulatory i reguły domenowe, kontekst Spring i zegar UTC, odczyt Dashboardu, kontroler Dashboardu oraz granice modułów Modulith.

Są to wyniki istniejących scenariuszy, nie deklaracja kompletności wszystkich możliwych zachowań API. Rozszerzenia wcześniej odłożone, takie jak równoległy pierwszy POST sesji, recent personal bests i produkcyjne logowanie, zachowują swój zakres.

## Następny krok

Przechodzimy do React: najpierw Dashboard na prawdziwych danych Mihu zamiast mocków, następnie Details, formularz sesji z odzyskiwalnym szkicem oraz edycja/usuwanie. Dalsze uzupełnienia API wynikają z potrzeb konkretnego ekranu.

Polecenie do ponowienia pełnego zestawu w Bash z katalogu `backend`:

```bash
./mvnw test
```

Odczyt danych lokalnych, Swaggera i 14 sesji DEMO jest opisany osobno w [uruchomieniu](development.md) i [historii Mihu](mihu-demo-history.md).
