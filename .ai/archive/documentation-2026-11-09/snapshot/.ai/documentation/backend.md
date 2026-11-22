# Backend Java

Aktualizacja auth 2026-11-08: Google/Firebase Admin, Spring Security, mapowanie issuer/UID, bootstrap i profil są wdrożone i odebrane na prawdziwym koncie Mihu. Pełny zestaw220/220 przeszedł. V24 działa na właściwej bazie, z zachowaną historią i UUID002; profil ukończony, użytkownik potwierdził Dashboard. Osobne modele działają przez Auth Emulator. [Uruchomienie i dowody](firebase-authentication-local.md). Poniższe statusy mają własne daty.

Status: dodany raport Training summary miesiąc/rok; aktualizacja 2026-11-07. Pełny zestaw backendu 210/210 przeszedł, bez nowej migracji. Aktualny zakres raportu i stan UI: [Training summary](period-training-summary.md). Poniżej zachowano opis v3 oraz wcześniejszych przyrostów.

## Training summary — 2026-11-07

W module reporting dodano publiczne `PeriodSummaryRequest`, `PeriodSummaryQuery` i `PeriodTrainingSummary`, a wewnętrznie `PeriodSummaryRepository`, `PeriodSummaryQueryService` i `PeriodSummaryController`. GET `/api/v1/statistics/summary` obsługuje MONTH/YEAR, rok 1–9998 oraz ALL/BOULDER/ROUTE_AND_CIRCUIT. MONTH wymaga month 1–12, YEAR go odrzuca. Osobny handler zwraca 400 `INVALID_PERIOD_SUMMARY_REQUEST`, bez zmiany kontraktu błędów piramidy.

Serwis działa w odczytowej transakcji REPEATABLE_READ. Pierwsze zapytanie agreguje pasujące wpisy do jednego rekordu sesji, zachowując jej pełny czas; drugie czyta kwalifikujące osiągnięcia z istniejącego widoku. Serwis tworzy komplet dni/miesięcy, bez limitu 20 sesji Dashboardu. Suma i liczniki pustego koszyka wynoszą 0, średnie/maksima są NULL. Loady i czas średni liczone są z sum sesyjnych, BigDecimal/HALF_UP do 4 miejsc. Wyróżnienia mają stabilne chronologiczne rozstrzygnięcie remisów i linki do źródeł.

DEC-048: filtr dyscypliny wybiera pełny czas sesji i częściowe ruchy/load. Najpierw okres/dyscyplina i kwalifikacja osiągnięć, potem jeden climbId według najlepszego udanego OS > FLASH > RP; roczna unikalność jest liczona ponownie dla całego roku. Próby i TOP_ROPE pozostają w pracy treningowej, lecz nie w osiągnięciach. Raport odczytuje historyczne snapshoty, nie uruchamia kalkulatora i nie zmienia piramidy. [Kontrakt API](api.md#training-summary--2026-11-07), [zakres i testy](period-training-summary.md).

## Poziom odniesienia i zapis v3 — 2026-11-04

`TrainingSessionService` używa `calculateAssessedV3` przy nowym POST i jawnym PUT. `TrainingMetricsFacade` zachowuje obliczenia intensywności, podziału cruxa i Classic Load z v2; zmiana dotyczy poziomu odniesienia używanego przez mnożnik względnego wysiłku i Adjusted Load. Wycena i punkty dawnej wspinaczki pozostają niezmienione. Kalkulatory v2 pozostają dostępne i testowane.

`ClimberLevelResolver.resolveWeighted` pobiera najnowsze kwalifikujące przejście każdej różnej wspinaczki, nie późniejsze niż data obliczanej sesji, a w tej samej dacie — niż jej początek. Rozdziela baldy i drogi/obwody oraz wyklucza usunięte sesje i TOP_ROPE. `RecencyWeight` i `RecencyWeightedLevelEstimator` obliczają referencję z kwadratową wagą wieku i horyzontem 730 dni. Przy mniej niż 10 kwalifikujących wspinaczkach pozostaje maksimum bez ważenia; brak historii uruchamia fallback bieżącej sesji. `RecencyLoadSettings` przechowuje ustawienia, które wraz ze źródłem i wersją estymatora trafiają do snapshotu. Raportowy `PyramidLevelEstimator` pozostaje odrębnym algorytmem bez tej zmiany wag. [Pełne reguły](recency-load.md).

`RelativeEffortInterpolator` interpoluje liniowo między sąsiednimi aktywnymi regułami trudności względnej. Zachowuje rzeczywistą różnicę indeksów i informację o przekroczeniu kalibracji; do wyszukania mnożnika ogranicza różnicę do zakresu −6/+5. Efektywny poziom ma 10 miejsc po przecinku; mnożnik jest zaokrąglany HALF_UP do 4 miejsc przed obliczeniem loadu i zapisem. Używamy `BigDecimal`.

Repozytorium zapisuje siedem nowych metadanych poziomu wymaganych przez V23, a Details zwraca je jako techniczne pola `CalculationDetails`; dla v1/v2 i WARMUP mają `null`. `currentLevelIndex` pozostaje całkowitą podłogą poziomu. Nowe zapisy i edycje mają wersję v3; GET i identyczne retry POST zachowują stare wyniki bez przeliczenia. Audyt edycji zachowuje snapshot przed/po, a pozostałe sesje pozostają nietknięte. Poniższe sekcje dokumentują wcześniejsze przyrosty.

## Piramida osiągnięć — 2026-11-03

Nowe GET `/api/v1/statistics/pyramid` i `/api/v1/statistics/pyramid/climbs` obsługują wszystkie 12 kombinacji dyscypliny/okresu/stylu, pierwszy kwalifikujący snapshot per climbId, dokładne punkty i paginację. Publiczny PyramidLevelEstimator implementuje uzgodniony ważony algorytm jako informację raportową. Wspólna kwalifikacja wyklucza wędkę z osiągnięć, personal bests, maksimów sesji i Fast RP, również przy odczycie dawnej historii. V21/V22 tworzą i optymalizują projekcje odczytu; surowe dane i load nie zostały zmienione. ClimberLevelResolver pozostaje dotychczasowy, jego integracja jest osobnym etapem OPEN-20. [Kontrakt, pliki i weryfikacja](achievement-pyramid-backend.md).

## Implementacja v2 przy wdrożeniu — 2026-11-01

`TrainingSessionService` używa v2 przy utworzeniu i edycji sesji. `TrainingMetricsFacade`, `AssessedEntryCalculator`, `MoveIntensityCalculator` i `IntensityProfileResolver` wyliczają wyniki z `profileIntensityIndex`; stare EDL nie uczestniczy już w obliczeniach. Profile cruxowe zapisują podział wykonanych ruchów, intensywności obu części oraz etykiety bazowej, trudnej i łatwiejszej wyceny. Wzory i zatwierdzona kalibracja Font pozostają w [modelu wyceny](../business/grading-and-scoring.md).

Repozytorium zapisuje wersję obliczeń i indeks profilu oraz opcjonalne dane cruxa jako snapshot wpisu. Nowy DTO `CruxDetails` jest częścią `TrainingSessionDetails.CalculationDetails`; odczyt obejmuje również punkty i wersję kalibracji wyceny. Historyczne EDL pozostają wyłącznie do odczytu snapshotów v1. Details, Dashboard i Calendar oznaczają wynik `calculationModelVersion`, w tym `mixed` dla różnych wersji wpisów, bez przeliczania zapisanych loadów.

Jawna edycja v1 tworzy wyniki v2 i audyt przed/po; pozostałe historyczne sesje pozostają nietknięte. Identyczne retry istniejącego POST nadal zwraca jego `sessionId` przed ponownym rozwiązywaniem profili i liczeniem. `SHORT_CONTINUOUS_ROUTE` został wycofany z aktywnego słownika; jego historyczne referencje pozostają w bazie.

Poprawiono przypadek pierwszej sesji w `ClimberLevelResolver`: PostgreSQL wyliczał indeks 0 z `GREATEST(NULL, 0)` przy pustej historii. Fallback prób wymaga teraz `EXISTS (SELECT 1 FROM history)`. Gdy historii nie ma, poziom jest wyprowadzany z bieżącej sesji, osobno dla balda i dróg/obwodów.

Trwała baza local ma schemat V20. Ostatnia migracja odświeża wyłącznie opisy profili; nie zmienia ich indeksów ani loadów. Pełny zestaw 74 testów po ostatnim uproszczeniu kodu przeszedł na schemacie do V19; wynik osobnej kontroli świeżej bazy V20 jest oddzielnym etapem weryfikacji. Przed sprawdzeniem UI użytkownik musi uruchomić lub zrestartować backend z nowym kodem. Frontend nie był zmieniany. Poniższe wyniki z października i opisy wcześniejszych przyrostów są historyczne.

## Stan potwierdzony w repozytorium

Weryfikacja 2026-10-17: pełne Maven `test` zakończyło się wynikiem 51/51, bez failures/errors/skipped. Potwierdzono podstawowy CRUD sesji, katalog, rollback, dostęp, scenariusze A/B i migracje do V15. [Raport](backend-verification.md). Ten wynik zastępuje wcześniejsze adnotacje o nieuruchomionych testach; kolejnym zakresem jest React.

| Element             | Dowód                                                                                                                                      |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| Maven i Spring Boot | [pom.xml](../../backend/pom.xml): Java 21, Spring Boot 4.1.0, BOM Modulith 2.1.0 --------------------------------------------------------- |
| Maven Wrapper ----- | `mvnw`, `mvnw.cmd`, konfiguracja dystrybucji Maven 3.9.16 -------------------------------------------------------------------------------- |
| Start aplikacji --- | [ClimbbetterBackendApplication](../../backend/src/main/java/pl/climbbetter/ClimbbetterBackendApplication.java) --------------------------- |
| Moduły ------------ | `package-info.java` w `identity`, `catalog`, `training`, `reporting`, `shared` ----------------------------------------------------------- |
| Profile i JPA ----- | `application.yml`, `application-local.yml`, testowy `application-test.yml`; `open-in-view: false`, `ddl-auto: validate` ------------------ |
| Czas -------------- | [TimeConfiguration](../../backend/src/main/java/pl/climbbetter/shared/internal/time/TimeConfiguration.java) udostępnia `Clock.systemUTC()` |
| Testy ------------- | context load, strefa UTC, `ApplicationModules.verify()` i konfiguracja Testcontainers PostgreSQL ----------------------------------------- |
| Zależności -------- | MVC, validation, JPA, Flyway/PostgreSQL, Actuator, Modulith oraz testowe startery -------------------------------------------------------- |

Wersje powyżej opisują pliki repozytorium, nie ocenę aktualności bibliotek. Istnienie zależności Actuator nie oznacza sprawdzenia odpowiedzi health. Local i Testcontainers używają przypiętego obrazu `postgres:17.6-alpine` zgodnie z DEC-023.

PostgreSQL testowy tworzy programowo `TestcontainersConfiguration`, a `@ServiceConnection` podłącza go do Spring Boot. Wymaga to uruchomionego Docker Desktop i pozostaje bazą tymczasową. Osobne środowisko local działa z [Compose](../../infra/compose.yml), portem hosta `5433` oraz nazwanym wolumenem zachowującym dane między restartami.

Moduł `training` zawiera klasyfikację wpisu, rozdzielone tryby przejścia i asekuracji, `MoveCount`, osobny `WarmUpEntry`, kalkulatory intensywności v2 i obu loadów oraz agregację `EntryContribution` do `SessionMetrics`. `TrainingMetricsFacade` stanowi jedno wejście do tych obliczeń i nie wystawia serwisowi zapisu wszystkich pomocniczych typów modelu. Reguły obejmują `executedMoves > totalMoves`, rozgrzewkę wnoszącą ruchy i zerowy load oraz sumowanie wpisów ocenianych z rozgrzewką.

Wcześniejszy pełny `./mvnw test` z 2026-10-10 wykonał 13 testów dla pierwszego pionu Dashboardu. Po dodaniu kolejnych testów domenowych użytkownik ponownie potwierdził sukces pełnego zestawu, ale bez przekazania końcowej liczby. Po migracji `V5` celowany `DashboardQueryIntegrationTest` uruchomił PostgreSQL 17.6 przez Testcontainers i przeszedł po dostosowaniu oczekiwanej skali loadów do czterech miejsc. Dnia 2026-10-11 testy integracyjne zastosowały migracje do `V12`. `TrainingSessionFlowIntegrationTest` zakończył się sukcesem po rzeczywistym żądaniu `POST`, zapisie sesji i trzech wpisów w PostgreSQL oraz odczycie wyniku przez endpointy Dashboardu i Details. Ten sam test potwierdził później rollback całej transakcji, identyczne ponowienie bez duplikatu, konflikt `409` przy ponownym użyciu `clientSessionId` dla zmienionej treści, personal bests oraz `404` dla nieznanej sesji.

## Potwierdzony pion Dashboardu

`GET /api/v1/dashboard/sessions` przechodzi przez `DashboardController`, `DashboardQueryService`, `CurrentUserProvider` i `DashboardSessionRepository`. Repozytorium używa `JdbcClient`, filtruje po bieżącym użytkowniku, sortuje sesje malejąco i zwraca maksymalnie 20 pozycji jako `DashboardSessionSummary`, wraz z zapisaną `averageMoveIntensity`.

`GET /api/v1/dashboard/personal-bests` zwraca wyniki życiowe `overall`, `os`, `flash` i `rp` oraz `rpVsOsGap` osobno dla balda i drogi. Każdy dostępny wynik wskazuje etykietę wyceny, datę pierwszego osiągnięcia i źródłową sesję. Pionowy test Bronx potwierdza `6A` overall/RP, `5` OS i różnicę dwóch indeksów.

Migracje `V1`–`V12` tworzą schematy modułów oraz tabele `identity.users`, `catalog.areas`, `catalog.grades`, `catalog.climbs`, `training.effort_profiles`, `training.style_rules`, `training.relative_effort_rules`, `training.training_sessions` i `training.training_entries`. `V5` rozdziela wariant oceniany od rozgrzewki, zachowuje kolejność wpisów i snapshot podstawowych wyników oraz ustawia cztery miejsca dziesiętne dla loadów. `V6` zawiera osobne skale `ROUTE_FRENCH` i `BOULDER_FONT`, lokalny `grade_index`, kalibrowane `grade_points` i wersję kalibracji. `V7` zasila osiem dodatnich profili EDL ocenianych wpisów; rozgrzewka pozostaje osobnym wariantem bez profilu. `V8` dodaje stabilną tożsamość wspinaczki, mapowanie `client_climb_id` oraz wymagane `climb_id` ocenianego wpisu. `V9` uzupełnia snapshot wpisu o identyfikację wyceny i profilu, indeks poziomu, historię kontaktów, familiarity oraz dane względnego wysiłku. `V10` zasila reguły stylu i trudności względnej oraz dodaje źródło znajomości i audyt konfliktu historii. `V11` dodaje początek, koniec i średnią intensywność sesji oraz pozwala zapisać poprawną sesję bez ukończonego przejścia. `V12` dodaje nullable `request_fingerprint` dla danych historycznych i ograniczenie poprawnego formatu SHA-256. Testowa migracja powtarzalna dodaje deterministycznego użytkownika wyłącznie w profilu testowym.

Uruchomienie z profilem `local` połączyło aplikację z trwałym PostgreSQL, zastosowało migracje do `V6` i uruchomiło Tomcat na porcie 8080. Profil ma domyślne połączenie zgodne z Compose, a zmienne `CB_DB_*` pozostają opcjonalnym nadpisaniem. Ręczne `GET /api/v1/dashboard/sessions` zwróciło wcześniej `[]` z pustej lokalnej tabeli. DBeaver potwierdził schematy `identity`, `catalog`, `training`, `reporting` oraz dane w `catalog.grades`.

## Zapis sesji

`TrainingSessionController`, transakcyjny `TrainingSessionService` i `TrainingSessionRepository` obsługują `POST /api/v1/training/sessions`. Serwis waliduje komendę i wylicza SHA-256 ze znormalizowanej treści: opcjonalne nazwy są przycięte, brak flagi konfliktu odpowiada `false`, a wpisy są porządkowane po `entryOrder`. Identyczny `clientSessionId` i fingerprint zwracają istniejące ID; zmieniona treść daje `409 CLIENT_SESSION_ID_CONFLICT`. Następnie serwis rozwiązuje Area, wspinaczki, wyceny, profile, historię i poziom, liczy wpisy przez `TrainingMetricsFacade`, zapisuje je w kolejności oraz aktualizuje summary sesji. Pierwsza sesja może wyznaczyć poziom z własnych przejść albo prób. Wcześniejsze wpisy tej samej transakcji są widoczne dla historii następnego wpisu.

Na granicy JDBC `Instant` jest konwertowany do `OffsetDateTime` w UTC, ponieważ sterownik PostgreSQL nie wyprowadza sam typu SQL dla `Instant`. Test wykrył ten problem przy tworzeniu prywatnego Area; poprawka obejmuje Area, Climb, sesję i wpisy.

## Szczegóły sesji

`GET /api/v1/training/sessions/{sessionId}` przechodzi przez `TrainingSessionDetailsService` i istniejące repozytorium JDBC. Odpowiedź zawiera podsumowanie oraz płaskie wpisy posortowane po `entryOrder`. `TrainingSessionDetails` grupuje w jednym pliku dane wpisu, obliczenia i kontekst historii jako rekordy zagnieżdżone. Zapytania zawsze filtrują po `sessionId` i bieżącym `userId`; brak i cudzy rekord są nierozróżnialnym `404`.

## Czego jeszcze nie ma w Javie

Aktualizacja 2026-10-17: kod zawiera edycję i usuwanie sesji, audyt, katalog Area/Climb, słowniki wycen/profili oraz lokalny CORS dla Vite. Aplikacja i testy kompilują się; testy nowego przyrostu pozostają do uruchomienia. Szczegóły, granice kontraktu i polecenie weryfikacji: [backend-crud-handoff](backend-crud-handoff.md).

Swagger UI i OpenAPI dodano 2026-10-17 przez springdoc `3.1.1`; kompilacja przeszła, sprawdzenie HTTP pozostaje do wykonania. [Adresy i uruchomienie](development.md#swagger-ui-i-openapi).

Nadal nie ma recent personal bests ani integracji React z API. Okno recent bests ustalono na 12 miesięcy (DEC-036). Brakuje potwierdzenia scenariusza wyłącznie z próbami i ochrony równoczesnych pierwszych retry. Te rozszerzenia nie blokują przejścia do React po potwierdzeniu podstawowego CRUD.

Stary `CB_Backend.md` jednocześnie nazywał katalog pustym i odhaczał bootstrap. Bieżący opis opiera się na kodzie; dawne checklisty pozostają w archiwum.

## Bieżący zakres backendu

Odtwarzamy Javę dla webowego przepływu z [priorytetów](../project-management/priorities.md): katalog i słowniki formularza, atomowy zapis ukończonej sesji, odczyt listy oraz szczegółów. Nie zaczynamy od endpointu `Start session`. Dashboard wymaga Adjusted Load i maksymalnych ukończonych wycen per skala; Details wymagają stabilnych ID, kolejności i snapshotów prób.

Zapis ma uwzględnić powtórzony request, rollback całego treningu przy błędzie, rozwiązywanie ID klienta, prywatne drafty katalogowe, historię poprzedzającą każdy wpis i konflikt OS/Flash. Mechanizm współbieżności i dokładny kontrakt są jeszcze do ustalenia.

## Granice implementacji

- Domena i kalkulator powinny być testowalne bez Springa i bazy.
- Command handler wyznacza granicę transakcji; query zwraca projekcję dla odbiorcy.
- Nie serializujemy encji JPA jako odpowiedzi HTTP.
- Flyway ma tworzyć schemat; Hibernate wyłącznie go waliduje.
- `CurrentUserProvider` jest wdrożonym portem; stała implementacja działa tylko w local/test, a `userId` nie pochodzi ze zwykłego body requestu.
- Reguły dziesiętne, wynik i snapshot odpowiadają [specyfikacji wyceny](../business/grading-and-scoring.md).

## Dowód ukończenia pierwszego przyrostu

Migracje budują pusty PostgreSQL; kalkulator zgadza się z zatwierdzonymi przykładami. Test integracyjny zapisuje próbę i przejście tej samej drogi, sprawdza wyniki i idempotentne ponowienie, a błędny wpis powoduje rollback. GET zwraca listę i pełne dane historyczne; frontend prezentuje ten sam wynik po ponownym otwarciu.

To kryteria przyszłej implementacji, nie lista testów wykonanych teraz. Pełne wcześniejsze checklisty zachowano w [CB_Backend](../archive/source-snapshot/docs/CB_Backend.md); ich kolejność Flutter-first została zastąpiona przez web-first.
