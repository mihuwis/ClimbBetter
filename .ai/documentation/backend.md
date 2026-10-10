# Backend Java

Status: odczyt Dashboardu i fundament obliczeń działają z PostgreSQL; zapis sesji jest rozpoczęty, ale jeszcze nie ma endpointu; aktualizacja 2026-10-10.

## Stan potwierdzony w repozytorium

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

Moduł `training` zawiera obecnie klasyfikację wpisu, rozdzielone tryby przejścia i asekuracji, `MoveCount`, osobny `WarmUpEntry`, kalkulatory EDL, intensywności i obu loadów oraz agregację `EntryContribution` do `SessionMetrics`. Potwierdzone reguły obejmują `executedMoves > totalMoves`, rozgrzewkę wnoszącą ruchy i zerowy load oraz sumowanie wpisów ocenianych z rozgrzewką.

Wcześniejszy pełny `./mvnw test` z 2026-10-10 wykonał 13 testów dla pierwszego pionu Dashboardu. Po dodaniu kolejnych testów domenowych użytkownik ponownie potwierdził sukces pełnego zestawu, ale bez przekazania końcowej liczby. Po migracji `V5` celowany `DashboardQueryIntegrationTest` uruchomił PostgreSQL 17.6 przez Testcontainers i przeszedł po dostosowaniu oczekiwanej skali loadów do czterech miejsc.

## Potwierdzony pion Dashboardu

`GET /api/v1/dashboard/sessions` przechodzi przez `DashboardController`, `DashboardQueryService`, `CurrentUserProvider` i `DashboardSessionRepository`. Repozytorium używa `JdbcClient`, filtruje po bieżącym użytkowniku, sortuje sesje malejąco i zwraca maksymalnie 20 rekordów jako `DashboardSessionSummary`.

Migracje `V1`–`V5` tworzą schematy modułów oraz tabele `identity.users`, `catalog.areas`, `training.training_sessions` i `training.training_entries`. `V5` rozdziela wariant oceniany od rozgrzewki, zachowuje kolejność wpisów i snapshot podstawowych wyników oraz ustawia cztery miejsca dziesiętne dla loadów. Testowa migracja powtarzalna dodaje deterministycznego użytkownika wyłącznie w profilu testowym. Test integracyjny wstawia Area i sesję do prawdziwego PostgreSQL, a następnie odczytuje read model przez `DashboardQuery`.

Uruchomienie z profilem `local` połączyło aplikację z trwałym PostgreSQL, zastosowało cztery migracje i uruchomiło Tomcat na porcie 8080. Ręczne `GET /api/v1/dashboard/sessions` zwróciło `[]` z pustej lokalnej tabeli. DBeaver potwierdził obecność schematów `identity`, `catalog`, `training` i `reporting`.

## Czego jeszcze nie ma w Javie

Nadal nie ma kontrolera, serwisu ani repozytorium zapisującego sesję, resolverów wyceny/profilu/stylu/poziomu, Details ani OpenAPI/springdoc. Powstał zewnętrzny `CreateTrainingSessionCommand`, ale w źródłach brakuje jeszcze wskazanego przez niego `TrainingEntryCommand`; jest to pierwszy konkretny krok następnej sesji. Dashboard ma działający odczyt, lecz lokalna baza nie zawiera jeszcze sesji utworzonej przez API, a frontend nadal korzysta z mocków.

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
