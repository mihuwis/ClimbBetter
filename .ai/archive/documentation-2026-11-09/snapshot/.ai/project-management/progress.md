# Stan projektu i prognoza

Data aktualizacji: 2026-11-08. Metoda: odczyt kodu, potwierdzone polecenia użytkownika i raporty testów. Poniższe wyniki uruchomień mają własne daty; aktualizacja dokumentacji nie oznacza ponownego wykonania testów.

Firebase Auth local zakończone i odebrane 2026-11-08 (DEC-052): Google, Firebase Admin/Spring Security, mapowanie UID, bootstrap/profil, transport i chronione trasy. V24 działa również na właściwej bazie; użytkownik potwierdził prawdziwe logowanie, profil Mihu i widoczność historii. Końcowa kontrola wykazała zachowane UUID002,17 aktywnych sesji/137 wpisów, pełne52 sesje/415 wpisów/46 revisions i niezmienione dane innych kont/katalogu. Backend220/220, web build/lint/30 model/37 browser,4 testy auth i1 rzeczywisty UI/API przeszły. Osobna baza modeli ma dwa syntetyczne treningi/osiem wpisów; drugie konto ich nie widzi. [Raport i instrukcja](../documentation/firebase-authentication-local.md). Bez resetu/replay i bez produkcji.

Plan środowisk z 2026-11-07 skrócony 2026-11-08 zgodnie z nowym poleceniem: dev local + production dla 5–10 osób, bez QA/staging. Ponownie sprawdzono tanie opcje OVH/Hetzner/Neon; zaproponowano dwa małe VPS z synchroniczną replikacją i niezależnym backupem. Użytkownik akceptuje utrzymanie; dostawca, koszt i szczegóły ochrony pozostają do zatwierdzenia. [Plan](../documentation/Environments_plan.md), DEC-049, OPEN-21–25. Zmieniono tylko dokumentację, bez testów aplikacji, zmian danych i zakupu zasobów.

Statystyki miesiąc/rok wdrożone na polecenie użytkownika2026-11-07: osobny endpoint i raport Training summary, reguły DEC-048, kompletne dni/miesiące, kafle, wykres z maksymalnie trzema liniami, dwa pierścienie, wyróżnienia i drilldown. Recharts ładowany osobno; dotychczasowa piramida i jej URL zachowane. Backend 210/210, build/lint, 30 model/37 browser i realny odczyt UI/API sześciu widoków przeszły. Niezależne sprawdzenie 18 raportów na 17 sesjach/137 wpisach i porównanie hashów historii/katalogu/audytu potwierdziły poprawność bez zmian danych lub V23. Tymczasowy serwer zamknięty, użytkownik restartuje8080 do nowego kodu. Bez nowej migracji i replay. [Opis i dowody](../documentation/period-training-summary.md).

Adjusted Load v3 wdrożony w backendzie i V23 local: kwadratowy zanik aktualności przez 730 dni, dowody bez TOP_ROPE, zachowane maksimum dla 1–9 różnych wspinaczek. Wyceny/punkty/intensywności i historyczne wyniki bez zmian. Pełny backend 176/176, frontend build/lint/24 model oraz realny browser CRUD v3 przeszły. Po teście izolowanym i restarcie backendu użytkownika na 8080 ukończono właściwy chronologiczny replay: 16 aktywnych sesji/133 wpisy v3. Pełna kontrola potwierdziła zgodność wejść oraz sum kontrolnych oryginalnych 35 sesji/278 wpisów/46 revisions, katalogu i danych innych kont. Dashboard zwraca 16 sesji. Wszystkie 12 widoków piramidy sprawdzone: RP ma 23 baldy i 4 drogi/obwody (27 łącznie), OS ma 10 baldów i 0 dróg/obwodów. [Reguły i dowody](../documentation/recency-load.md).

Eksport i odtworzenie historii zakończone: SQL i dane wejściowe Mihu pozostają w ignorowanym `.local-backups/mihu-002/2026-11-04-before-recency-model/`. Zweryfikowano wierne odtworzenie kopii 35/278/30 rekordów oraz testowy API replay 16/133 na osobnej bazie. Następnie właściwe 16 sesji odwracalnie usunięto i odtworzono przez API v3 na 8080, bez importowania starych wyników obliczeń. Aktywna historia ponownie zawiera 16 sesji/133 wpisy, z nowymi metadanymi i loadami; oryginalne rekordy, katalog i dziennik replay pozostają zachowane. [Raport](../documentation/training-history-export.md).

Najnowsze usprawnienie sesji: Repeat climb tworzy niezależny wpis tej samej wspinaczki bez zmiany wartości, a użytkownik zatwierdził dokładną równość ruchów Completed (DEC-046). Walidacja w UI i API obejmuje nowe zapisy/edycje, zachowując odczyt historii oraz identyczny retry. Końcowe build/lint, 24 model/31 browser/136 backend przeszły. Samo usprawnienie nie wymagało nowych migracji ani zmian lokalnej historii; serwer użytkownika działa już z v3 po restarcie na 8080. [Opis](../documentation/frontend-web.md#repeat-climb-i-completed--2026-11-04).

## Statistics — 2026-11-04

Kolejne usprawnienie po ocenie użytkownika: ręczne Start → End +1 h oraz Repeat session z wyborem daty i edycją skopiowanych wpisów w tym samym miejscu. Nowe ID, odrębny odzyskiwalny szkic i dokładny retry; poprzednia sesja nie jest edytowana. Build/lint, 16 testów modelu i pełne 26 przeglądarkowych przeszły, bez zapisu do lokalnej bazy. [Opis](../documentation/frontend-web.md#ręczna-data-i-repeat-session--2026-11-04).

Frontend `/statistics` wdrożony z piramidą jako pierwszym osobnym raportem. Wybory i listy zachowują stan w URL, warstwy odzwierciedlają backend, estymacja jest informacyjna. Build/lint, 11 testów modelu i 22 przeglądarkowe przeszły. Osobny odczytowy test rzeczywistego API/UI: wszystkie 12 kombinacji, listy, powrót z sesji i telefon; rekordy 19 sesji/145 wpisów oraz audyt i loady nie zmieniły się. Obejrzano zrzuty desktop/mobile. Pozostają ręczna ocena użytkownika i rozszerzony odbiór P13 z operacjami zapisu. Kolejne zestawy mają obejmować okresy miesięczne i sezonowe; obecny kontrakt piramidy pozostaje 2 lata/all-time. [Raport](../documentation/statistics-frontend.md).

## Plan 2026-11-03 — wdrożenie piramidy

Następnie użytkownik zlecił backend i bazę: ten zakres jest już wykonany. API raportu i list, informacyjny estimator i poprawki kwalifikacji osiągnięć przeszły pełny zestaw 120 testów. Local zastosował V21/V22; 12 kombinacji rzeczywistego HTTP, listy i OpenAPI potwierdzone. Skróty pełnych rekordów 19 sesji, 145 wpisów i audytu oraz oba loady pozostały identyczne. Frontend i pełny odbiór UI oczekują, integracja z loadem nadal jest poza tym przyrostem. [Handoff](../documentation/achievement-pyramid-backend.md). Poniższy akapit opisuje poprzedzający wdrożenie etap planowania.

Użytkownik podtrzymał ważony Pyramid Support i poprosił o pełny plan backendu/frontendu. Zapisano P01–P13: kontrakt, kwalifikacja osiągnięć, spójny odczyt dawnej historii, raport/deduplikacja, baza/wydajność, estimator, punkty łączne, API, testy backendu, Statistics, wykres/lista i testy frontendu oraz odbiór przez rzeczywistą bazę. Proponowany pierwszy przyrost pokazuje estymację informacyjnie; integracja z poziomem/loadem i wspólny estimator punktowy pozostają osobnym etapem OPEN-20. Plan nie jest wykonanym wdrożeniem; kod/baza bez zmian, testów aplikacji w tym kroku nie uruchamiano. [Pełny plan](../documentation/achievement-pyramid-implementation-plan.md).

## Ustalenie 2026-11-01 — piramida osiągnięć

Zatwierdzono DEC-045: jedna wspinaczka raz po filtrze okresu i stylu; cała historia/ostatnie dwa lata; drogi razem z obwodami; RP obejmuje RP/OS/Flash, OS wyłącznie OS, również dla baldów i widoku zbiorczego. Wędka nigdy nie jest osiągnięciem, pozostaje treningiem. Snapshot pierwszego kwalifikującego przejścia w okresie określa wycenę. Raport i Statistics są do wdrożenia; przegląd wykazał brak wykluczenia wędki w obecnych rekordach, maksimach sesji i Fast RP. Pełny Pyramid Support pozostaje niewdrożony, jego rola i zakres integracji z poziomem/loadem wymagają wyjaśnienia (OPEN-20). W tym kroku zmieniono tylko dokumentację, bez testów aplikacji i zmian bazy. [Reguły](../business/achievement-pyramid.md).

## Ustalenie 2026-11-01 — model v2 z wagami profili

Backend modelu v2 jest zaimplementowany i sprawdzony. Nowe sesje i edycje korzystają z `profileIntensityIndex`; dla cruxów liczymy ceil(15% wykonanych ruchów) jako trudne, resztę jako łatwiejsze, z intensywnościami wyznaczonymi proporcjami punktów Font. Zapis/odczyt obejmuje indeks, pełny podział cruxa i wersję; audyt zachowuje parametry przed i po edycji. Kalkulator EDL usunięto, dawna historia pozostaje czytelna. Krótka ciągowa jest wycofana z nowych wyborów, obwód ma indeks 7. Pełny zestaw po ostatnim uproszczeniu: **74 testy, 0 failures/errors/skipped, BUILD SUCCESS**. Flyway local zastosował V16–V20; porównanie 19 sesji i 145 wpisów, obu sum loadów i odcisku wyników potwierdziło brak zmian historii. Dodatkowo 9 testów integracyjnych v2 przeszło na świeżym schemacie V20; po tej migracji ponownie potwierdzono identyczne historyczne sumy i hash. [Model](../business/grading-and-scoring.md), [raport](../documentation/backend-verification.md).

Frontend v2 wdrożony na dalsze polecenie użytkownika: usunięto EDL z typów i Details, dodano indeks profilu, średnią intensywność oraz szacowany podział cruxa z API. Formularze pokazują opisy profili, pilnują aktywnych wyborów i pozwalają naprawić stare szkice bez utraty danych. Klucze szkiców, schemaVersion 1 i dokładny pendingRequest pozostają zachowane. **Build/lint, 6/6 testów modelu i 13/13 przeglądarkowych przeszły**; Details sprawdzono przy 390 px bez poziomego przewijania. Dzisiaj przeglądarka korzystała z kontrolowanych odpowiedzi HTTP. Kolejny etap: uruchomienie/restart aplikacji i wspólne dodanie sesji przez rzeczywisty backend V20. [Raport frontendu](../documentation/frontend-web.md#weryfikacja-v2--2026-11-01).

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
