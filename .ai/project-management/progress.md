# Stan projektu i prognoza

Data aktualizacji: 2026-10-10. Metoda: odczyt kodu, potwierdzone polecenia użytkownika i raporty testów. Pełny zestaw testów przeszedł po dodaniu modelu obliczeń i agregacji. Następnie celowany test integracyjny Dashboardu potwierdził migrację `V5`; początkowy błąd dotyczył wyłącznie oczekiwanej skali `BigDecimal` po przejściu z dwóch do czterech miejsc.

## Stan obszarów

| Obszar                      | Co jest                                                                                                                         | Czego brakuje / co nie jest potwierdzone                                                           | Ocena                                         |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- | --------------------------------------------- |
| Produkt i wycena ---------- | tabele, wzory, DEC-001–018, wymagania web z września; zatwierdzone jedno Area na sesję ---------------------------------------- | luki stylu/czasu/poziomu i 5–10 realnych zatwierdzonych golden cases ----------------------------- | specyfikacja zaawansowana, nadal pytania ---- |
| Java ---------------------- | bootstrap, GET Dashboardu, kalkulatory EDL/intensywności/loadu, model rozgrzewki, agregacja metryk, szkic komendy sesji i testy domenowe/integracyjne | brakujący `TrainingEntryCommand`, zapis sesji, resolvery słowników/historii, Details i OpenAPI | odczyt i fundament obliczeń działają -------- |
| PostgreSQL i infrastruktura | Flyway `V1`–`V5`, tabele użytkowników, Area, sesji i wpisów, testowy/local seed, PostgreSQL 17.6 w Testcontainers i Compose local | słowniki wycen/profili, katalog wspinaczek, repozytorium zapisu i dane utworzone przez API ---------- | fundament zapisu istnieje, brak `POST` ------ |
| Web ----------------------- | Dashboard, Calendar, Areas, Session Details i routing ------------------------------------------------------------------------- | Record session, lokalny trwały szkic, integracja API, nowe metryki i grupowanie, testy komponentów | prototyp na mockach ------------------------- |
| Mobile -------------------- | shell, Board, profil, formularz i widget test --------------------------------------------------------------------------------- | HTTP, trwałe dane, outbox, sync, rozdzielenie EDL/ruchów ----------------------------------------- | prototyp w pamięci -------------------------- |
| Historia i audyt ---------- | opis snapshotów, client ID, archiwizacji i revisions -------------------------------------------------------------------------- | implementacja i testy scenariuszy ---------------------------------------------------------------- | zaplanowane --------------------------------- |
| Auth / wydanie ------------ | opis kierunku OIDC i izolacji użytkownika ------------------------------------------------------------------------------------- | implementacja i zweryfikowane środowisko wdrożenia ----------------------------------------------- | późniejszy zakres --------------------------- |
| Planowanie treningów ------ | kierunek osobnego modułu ------------------------------------------------------------------------------------------------------ | model, algorytm propozycji i implementacja ------------------------------------------------------- | odłożone ------------------------------------ |
| Dokumentacja `.ai` -------- | iteracja 1 zweryfikowana; w iteracji 2 zapisano DEC-019–022: Git dla `.ai`, zasady współpracy, trwały szkic i Dashboard bez wykresu | pozostałe decyzje kontraktu APP-01/02 i kontrola spójności w iteracji 3 -------------------------- | iteracja 2 w toku --------------------------- |

Dowody: [backend](../documentation/backend.md), [web](../documentation/frontend-web.md), [mobile](../documentation/frontend-mobile.md), [manifest źródeł](../archive/source-manifest.json). Istniejąca zmiana `.vscode/settings.json` nie należy do tej pracy.

Aktualizacja 2026-10-03: powstał pierwszy mały element domeny Java, `FamiliarityBand`, wraz z testami granic i odrzucenia wartości ujemnej. Polecenie `./mvnw -Dtest=FamiliarityBandTest test` zakończyło się wynikiem 7/7. Nie oznacza to jeszcze implementacji kalkulatora ani agregatu sesji.

Weryfikacja 2026-10-04: po uruchomieniu Docker Desktop pełne `./mvnw test` zakończyło się sukcesem. Potwierdzono 7 testów `FamiliarityBand`, 2 testy kontekstu Spring i zegara UTC oraz 1 test granic Modulith. PostgreSQL został uruchomiony tymczasowo przez Testcontainers.

Weryfikacja 2026-10-10: powstał `GET /api/v1/dashboard/sessions` z read modelem i filtrowaniem przez `CurrentUserProvider`. Local zastosował `V1`–`V4`, a ręczny GET zwrócił `[]`. Później dodano model klasyfikacji, ruchów, rozgrzewki, kalkulatory EDL/intensywności/loadów i agregację sesji; użytkownik potwierdził pełny zielony zestaw testów. `V5` utworzyła `training_entries` i zwiększyła skalę loadów do czterech miejsc; celowany test integracyjny przeszedł po aktualizacji oczekiwanych wartości. `CreateTrainingSessionCommand` istnieje, lecz wskazany `TrainingEntryCommand` nie jest obecny w aktualnych źródłach, więc komenda zapisu nie jest jeszcze kompletna.

## Weryfikacja iteracji dokumentacji 1

Utworzono wszystkie 21 wymaganych plików podstawowej struktury (wliczając główne AGENTS i README); razem z indeksem archiwum i czterema diagramami jest 26 aktywnych plików Markdown. Wszystkie 21 kopii źródłowych zgadza się z manifestem SHA-256. Pokryto każdy plik dawnego `docs`; pozostałe oryginały są niezmienione, a pierwotny główny README zachowano przed aktualizacją.

Sprawdzono lokalne odsyłacze aktywnych dokumentów, kodowanie UTF-8 i domknięcie bloków kodu. `git diff --check` nie zgłosił błędów. Diagramy zapisano w Mermaid i sprawdzono na poziomie źródła; nie renderowano ich w przeglądarce. Historyczne odsyłacze wewnątrz wiernych kopii nie były przepisywane.

## Stan iteracji dokumentacji 2

Odpowiedzi użytkownika zamknęły OPEN-01, OPEN-02 i OPEN-05 oraz część OPEN-04 dotyczącą odzyskiwalnego szkicu. `.ai` wraz z archiwum ma wejść do Git, a oryginalne `docs` pozostają ignorowane. Asystent może czytać pliki i wykonywać uzgodnione testy, ale zmiany kodu nadal wymagają zatwierdzenia. Lokalny szkic musi przetrwać odświeżenie i niepotwierdzony zapis; liczba szkiców, konflikty wielu kart i backendowy preview pozostają otwarte. Dashboard pierwszego przyrostu pokazuje tekstowe metryki z prawdziwych danych bez wykresu.

## Postęp wobec przyrostów

| Przyrost                    | Stan                                      | Co rozstrzyga ukończenie                                      |
| --------------------------- | ----------------------------------------- | ------------------------------------------------------------- |
| APP-01 — zapis/odczyt Java  | nieukończony; działa pierwszy odczyt Dashboardu | test rzeczywistego zapisu, obliczeń, rollbacku i idempotencji |
| APP-02 — pełny przepływ web | nieukończony; jest prototyp ekranów ----- | powtarzalny zapis i ponowny odczyt treningu przez UI -------- |
| APP-03–06 ----------------- | brak potwierdzonego ukończonego przyrostu | kryteria w roadmapie, do doprecyzowania przed pracą --------- |

Nie podajemy procentu gotowości całej aplikacji: nie ma zamkniętego, oszacowanego zakresu, a makiety, dokumentacja i działające funkcje nie są równoważnymi jednostkami postępu.

## Ile już trwało

Użytkownik podał 4 godziny pracy 2026-10-03 oraz około 5 godzin łącznie dla sesji 2026-10-10. Drugi pomiar zastępuje wcześniejszą cząstkową informację o około 3 godzinach tego dnia. Wcześniejszy historyczny nakład pozostaje nieznany; dat commitów nie traktujemy jako czasu pracy. Szczegóły zapisuje [work-log](work-log.md).

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
