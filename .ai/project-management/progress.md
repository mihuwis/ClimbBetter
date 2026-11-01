# Stan projektu i prognoza

Data aktualizacji: 2026-10-11. Metoda: odczyt kodu, potwierdzone polecenia użytkownika i raporty testów. Pełny zestaw testów przeszedł po dodaniu modelu obliczeń i agregacji. Celowany test integracyjny Dashboardu potwierdził następnie migracje do `V6`, w tym utworzenie i zasilenie dwóch słowników wycen.

## Potwierdzenie 2026-10-17

**Najnowszy stan — React 1–5:** zaimplementowano Dashboard na API, grupowane Details i chronologię, edycję/usuwanie wpisów oraz sesji, Calendar z zakresem dat, ręczne dodanie i Start now. Szkic użytkownika, w tym niedokończony wpis, przetrwa odświeżenie; niepotwierdzone żądanie zachowuje dokładną treść. UI jest angielski zgodnie z odpowiedzią użytkownika. `build` i `lint` przeszły, testy modelu 4/4, przeglądarkowe 6/6 i realny CRUD React → API → PostgreSQL 1/1. Pełny backend po dodaniu odczytu kalendarza: 52/52. Oczekuje ocena użytkownika; media, preview, pełny offline i produkcyjne logowanie nie wchodzą do tego przyrostu. [Raport, uruchomienie i ograniczenia](../documentation/frontend-web.md). Poniższe wcześniejsze stany prototypu i oczekujących testów są historyczne.

Końcowa bramka przed React: na wyraźne polecenie użytkownika asystent uruchomił pełny zestaw Maven `test`. Wynik: **51 testów, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS**. Potwierdzono CRUD, rollback, wersję/audyt, izolację użytkowników, scenariusze A/B, OS/Flash oraz katalog skalny i migracje do V15 na PostgreSQL Testcontainers. Nie były potrzebne poprawki. Następny krok to Dashboard React na danych API. [Raport](../documentation/backend-verification.md). Poniższe zapisy o oczekujących testach opisują wcześniejszy stan tego dnia i są zastąpione tym wynikiem.

Lokalne dane DEMO: zapis przez serwisy aplikacji i GET Dashboardu potwierdziły 14 sesji Mihu, po dwie tygodniowo od 1 września do 15 października. Przy poprawieniu dat pięciu pierwszych rekordów wykorzystano edycję z wersją, przeliczeniem i audytem; zestawu testów automatycznych nadal nie uruchamiano. [Raport](../documentation/mihu-demo-history.md).

Katalog skalny: Flyway zastosował V15 do local. SQL potwierdził publiczną Dolinę Bolechowicką, Filar Abazego i 28 pozycji (27 wycenionych), oryginalne etykiety Kurtyki, francuskie mapowania v1, metry i sugestie ruchów. Oba konta widzą wszystkie 28 dróg. Kod i testy skompilowano; HTTP i testy nowego rozszerzenia pozostają do sprawdzenia. [Raport](../documentation/outdoor-catalog.md).

Rozszerzenie kodu katalogu: samodzielne tworzenie prywatnego Climb i odczyt po ID, z testami scenariuszy A/B. Kompilacja potwierdzona; uruchomienie testów i UI React pozostają do wykonania. [Szczegóły](../documentation/catalog-climb-scenarios.md).

Po sprawdzeniu Dockera użytkownik potwierdził sukces celowanego `TrainingSessionFlowIntegrationTest`. Konflikt OS z wcześniejszym wpisem tej samej sesji zwraca `409 ASCENT_MODE_HISTORY_CONFLICT`, wycofuje zapis i pozwala ponowić żądanie z jawnym potwierdzeniem; zapisane są obie flagi audytu. Do sprawdzenia pozostają mnożnik `1.50` po potwierdzeniu oraz wariant Flash. To aktualizacja względem poniższego stanu z 2026-10-11, bez potwierdzenia dzisiejszego pełnego zestawu testów.

## Stan obszarów

Aktualizacja kodu 2026-10-17: na polecenie użytkownika asystent dodał podstawowy CRUD sesji, audyt, katalog Area/Climb i słowniki formularza oraz lokalny CORS dla React/Vite. Nowy `TrainingSessionCrudIntegrationTest` obejmuje cały przepływ i kontrolę dostępu. Kompilacja aplikacji i testów przeszła; nowych testów integracyjnych jeszcze nie uruchomiono. Po ich potwierdzeniu najbliższym celem jest React. [Raport i kontrakt](../documentation/backend-crud-handoff.md). Tabela poniżej zachowuje potwierdzony stan z 2026-10-11; obecność nowego kodu nie oznacza jeszcze potwierdzenia działania.

| Obszar                      | Co jest                                                                                                                         | Czego brakuje / co nie jest potwierdzone                                                           | Ocena                                         |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| Produkt i wycena ---------- | tabele, wzory, DEC-001–018, wymagania web z września; zatwierdzone jedno Area na sesję ---------------------------------------- | luki stylu/czasu/poziomu i 5–10 realnych zatwierdzonych golden cases ----------------------------- | specyfikacja zaawansowana, nadal pytania ---- |
| Java ---------------------- | POST sesji, Dashboard z intensywnością i personal bests, Details z chronologicznymi wpisami, resolvery, kalkulatory, błędy i fingerprint retry potwierdzone integracyjnie | katalog formularza, edycja, recent bests, konflikt historii, ochrona równoległego retry i OpenAPI | pierwszy pion zapisu/odczytu działa end-to-end po backendzie |
| PostgreSQL i infrastruktura | Flyway `V1`–`V12`, tabele i snapshoty pierwszego pionu, słowniki/reguły, fingerprint żądania, Testcontainers i trwały Compose local | konflikt historii, zapis local przez API i scenariusz równoległego retry | baza obsługuje atomowy oraz idempotentny zapis |
| Web ----------------------- | Dashboard, Calendar, Areas, Session Details i routing ------------------------------------------------------------------------- | Record session, lokalny trwały szkic, integracja API, nowe metryki i grupowanie, testy komponentów | prototyp na mockach ------------------------- |
| Mobile -------------------- | shell, Board, profil, formularz i widget test --------------------------------------------------------------------------------- | HTTP, trwałe dane, outbox, sync, rozdzielenie EDL/ruchów ----------------------------------------- | prototyp w pamięci -------------------------- |
| Historia i audyt ---------- | opis snapshotów, client ID, archiwizacji i revisions -------------------------------------------------------------------------- | implementacja i testy scenariuszy ---------------------------------------------------------------- | zaplanowane --------------------------------- |
| Auth / wydanie ------------ | opis kierunku OIDC i izolacji użytkownika ------------------------------------------------------------------------------------- | implementacja i zweryfikowane środowisko wdrożenia ----------------------------------------------- | późniejszy zakres --------------------------- |
| Planowanie treningów ------ | kierunek osobnego modułu ------------------------------------------------------------------------------------------------------ | model, algorytm propozycji i implementacja ------------------------------------------------------- | odłożone ------------------------------------ |
| Dokumentacja `.ai` -------- | iteracja 1 zweryfikowana; w iteracji 2 zapisano DEC-019–022: Git dla `.ai`, zasady współpracy, trwały szkic i Dashboard bez wykresu | pozostałe decyzje kontraktu APP-01/02 i kontrola spójności w iteracji 3 -------------------------- | iteracja 2 w toku --------------------------- |

Dowody: [backend](../documentation/backend.md), [web](../documentation/frontend-web.md), [mobile](../documentation/frontend-mobile.md), [manifest źródeł](../archive/source-manifest.json). Istniejąca zmiana `.vscode/settings.json` nie należy do tej pracy.

Aktualizacja 2026-10-03: powstał pierwszy mały element domeny Java, `FamiliarityBand`, wraz z testami granic i odrzucenia wartości ujemnej. Polecenie `./mvnw -Dtest=FamiliarityBandTest test` zakończyło się wynikiem 7/7. Nie oznacza to jeszcze implementacji kalkulatora ani agregatu sesji.

Weryfikacja 2026-10-04: po uruchomieniu Docker Desktop pełne `./mvnw test` zakończyło się sukcesem. Potwierdzono 7 testów `FamiliarityBand`, 2 testy kontekstu Spring i zegara UTC oraz 1 test granic Modulith. PostgreSQL został uruchomiony tymczasowo przez Testcontainers.

Weryfikacja 2026-10-10: powstał `GET /api/v1/dashboard/sessions` z read modelem i filtrowaniem przez `CurrentUserProvider`. Local zastosował `V1`–`V4`, a ręczny GET zwrócił `[]`. Później dodano model klasyfikacji, ruchów, rozgrzewki, kalkulatory EDL/intensywności/loadów i agregację sesji; użytkownik potwierdził pełny zielony zestaw testów. `V5` utworzyła `training_entries` i zwiększyła skalę loadów do czterech miejsc; celowany test integracyjny przeszedł po aktualizacji oczekiwanych wartości. Tego dnia `CreateTrainingSessionCommand` nadal nie miała brakującego wówczas `TrainingEntryCommand`.

Weryfikacja 2026-10-11: dodano `TrainingEntryCommand` i potwierdzono czystą kompilację. Ustalono niezależne skale `ROUTE_FRENCH` i `BOULDER_FONT`, lokalne indeksy oraz wspólną kalibrację punktów v1. Migracje `V6`–`V10` dodały wyceny, profile, wspinaczki, pełniejszy snapshot i reguły obliczeń. `V11` uzupełniła summary sesji o czas i średnią intensywność oraz usunęła błędny wymóg ukończonej wyceny. `V12` dodała 64-znakowy SHA-256 fingerprint żądania. Powstały resolvery, `TrainingMetricsFacade`, transakcyjny `TrainingSessionService`, repozytorium oraz `POST /api/v1/training/sessions`. `TrainingSessionFlowIntegrationTest` uruchomił PostgreSQL 17.6 przez Testcontainers, wysłał sesję Bronx z rozgrzewką i dwoma przejściami, potwierdził trzy wpisy w bazie oraz odczytał 36 ruchów, średnią intensywność `14.0625` i maksymalną wycenę `6A` przez Dashboard. Kolejne potwierdzone scenariusze sprawdzają rollback całego zapisu, identyczne ponowienie bez duplikatu, odpowiedź `409 CLIENT_SESSION_ID_CONFLICT`, personal bests (`6A` overall/RP, `5` OS, `rpVsOsGap = 2`) oraz Details z trzema uporządkowanymi wpisami i `404`. Pierwszy błąd testu ujawnił opcjonalny boolean rozgrzewki, drugi brak mapowania `Instant` w JDBC; oba przypadki poprawiono.

## Weryfikacja iteracji dokumentacji 1

Utworzono wszystkie 21 wymaganych plików podstawowej struktury (wliczając główne AGENTS i README); razem z indeksem archiwum i czterema diagramami jest 26 aktywnych plików Markdown. Wszystkie 21 kopii źródłowych zgadza się z manifestem SHA-256. Pokryto każdy plik dawnego `docs`; pozostałe oryginały są niezmienione, a pierwotny główny README zachowano przed aktualizacją.

Sprawdzono lokalne odsyłacze aktywnych dokumentów, kodowanie UTF-8 i domknięcie bloków kodu. `git diff --check` nie zgłosił błędów. Diagramy zapisano w Mermaid i sprawdzono na poziomie źródła; nie renderowano ich w przeglądarce. Historyczne odsyłacze wewnątrz wiernych kopii nie były przepisywane.

## Stan iteracji dokumentacji 2

Odpowiedzi użytkownika zamknęły OPEN-01, OPEN-02 i OPEN-05 oraz część OPEN-04 dotyczącą odzyskiwalnego szkicu. `.ai` wraz z archiwum ma wejść do Git, a oryginalne `docs` pozostają ignorowane. Asystent może czytać pliki i wykonywać uzgodnione testy, ale zmiany kodu nadal wymagają zatwierdzenia. Lokalny szkic musi przetrwać odświeżenie i niepotwierdzony zapis; liczba szkiców, konflikty wielu kart i backendowy preview pozostają otwarte. Dashboard pierwszego przyrostu pokazuje tekstowe metryki z prawdziwych danych bez wykresu.

## Postęp wobec przyrostów

| Przyrost                    | Stan                                      | Co rozstrzyga ukończenie                                      |
| --------------------------- | ----------------------------------------- | ------------------------------------------------------------- |
| APP-01 — zapis/odczyt Java  | częściowo ukończony; działa `POST → PostgreSQL → Dashboard/Details`, metryki, personal bests, rollback i retry z fingerprintem | katalog formularza, konflikt historii, recent bests, edycja i ochrona współbieżnego retry |
| APP-02 — pełny przepływ web | nieukończony; jest prototyp ekranów ----- | powtarzalny zapis i ponowny odczyt treningu przez UI -------- |
| APP-03–06 ----------------- | brak potwierdzonego ukończonego przyrostu | kryteria w roadmapie, do doprecyzowania przed pracą --------- |

Nie podajemy procentu gotowości całej aplikacji: nie ma zamkniętego, oszacowanego zakresu, a makiety, dokumentacja i działające funkcje nie są równoważnymi jednostkami postępu.

## Ile już trwało

Użytkownik podał 4 godziny pracy 2026-10-03, około 5 godzin dla sesji 2026-10-10 oraz około 7 godzin 2026-10-11. Daje to około 16 godzin jawnie zaraportowanej pracy w tych trzech dniach. Wcześniejszy historyczny nakład pozostaje nieznany; dat commitów nie traktujemy jako czasu pracy. Szczegóły zapisuje [work-log](work-log.md).

## Prognoza pozostałego czasu

**Na dziś: niewyznaczona.** Brakuje uzgodnionego zakresu wydania, dostępności tygodniowej i danych o tempie realizacji. Nie ma podstaw do rzetelnej daty ukończenia ani liczbowego przedziału.

W iteracji 2 określimy zakres prognozy (najpierw APP-01/02), rozbijemy tylko najbliższy przyrost na małe zadania i uzgodnimy sposób estymowania. Po zapisaniu rzeczywistych wyników można wyznaczyć przedział, np. pozostały nakład / dostępne godziny tygodniowo albo liczbę porównywalnych przyrostów / obserwowane tempo. Nie mieszamy obu jednostek na jednym wykresie.

Prognoza będzie zawierała datę wyliczenia, przyjęty zakres, założenia dostępności, optymistyczny i ostrożny wariant oraz poziom niepewności. Zmiana zakresu wymaga aktualizacji prognozy. Czas oczekiwania na decyzje zapisujemy oddzielnie od pracy.

## Burndown i burnup

Burndown pokaże pozostałą oszacowaną pracę w ustalonej iteracji; burnup — ukończoną pracę na tle całego uzgodnionego zakresu. Dane będą pochodzić z [work-log](work-log.md), nie z liczby plików lub commitów.

| Data       | Zakres                            | Jednostka   | Zakres całkowity | Ukończone    | Pozostałe     |
| ---------- | --------------------------------- | ----------- | ---------------- | ------------ | ------------- |
| 2026-10-03 | APP-01/02 — przed doprecyzowaniem | nieustalona | nieoszacowane -- | niezmierzone | nieoszacowane |

Nie narysowano fikcyjnego trendu z jednego nieoszacowanego punktu. Po uzgodnieniu jednostki i zebraniu obserwacji dodamy wykres z tymi danymi. Nieznane wartości nie są zerem.
