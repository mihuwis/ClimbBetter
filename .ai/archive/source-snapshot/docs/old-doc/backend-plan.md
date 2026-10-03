# ClimbBetter — plan backendu Java

Status: plan wykonawczy do wspólnego dopracowania przed rozpoczęciem implementacji.  
Data przeglądu: 2026-08-13.

## 1. Cel i najważniejsza decyzja

Budujemy nowy backend na podstawie rzeczywistych potrzeb produktu, Reacta i Fluttera. `backend-net` pozostaje działającą referencją techniczną, ale nie jest wzorem architektury, encji ani kontraktów HTTP dla Javy.

Pierwszy pionowy przepływ ma wyglądać tak:

```text
Flutter: „Zapisz ukończoną sesję”
        ↓
Java REST API: walidacja i obliczenia
        ↓
PostgreSQL: sesja, wpisy, snapshoty i podsumowanie
        ↓
React: lista sesji i Session Details
        ↓
Flutter Board: ta sama zapisana sesja
```

Ten przepływ jest ważniejszy niż zbudowanie od razu wszystkich ekranów i tabel. Udowodni, że trzy projekty współpracują, a obliczenia mają jedno źródło prawdy. Następnie będziemy usuwać mocki ekran po ekranie.

### Zakres pierwszego działającego wydania

- lokalny PostgreSQL uruchamiany jedną komendą,
- migracje i deterministyczne dane startowe,
- jeden użytkownik developerski o stałym UUID,
- miejsca, drogi i słowniki potrzebne formularzowi,
- zapis całej ukończonej sesji razem z wpisami w jednej transakcji,
- serwerowe obliczenie punktów i obciążenia,
- zapis snapshotów historycznych,
- lista sesji i szczegóły sesji,
- pierwszy pełny test integracyjny,
- zapis sesji z Fluttera i odczyt tej samej sesji w React oraz Flutter Board.

### Świadomie poza pierwszym przepływem

- pełne logowanie OAuth/JWT,
- kompletna synchronizacja offline,
- cele, challenge, achievements i social feed,
- cykle treningowe i zaawansowana analityka,
- mikroserwisy, broker wiadomości, Redis i osobna baza odczytowa,
- automatyczne przepisanie lub import schematu EF Core.

## 2. Co wynika z istniejących frontendów

### React

Aktualne ekrany pracują wyłącznie na mockach:

| Ekran | Dane potrzebne z API |
|---|---|
| Dashboard | liczba sesji, streak, sesje tygodnia, ostatnia aktywność, feed, load/ruchy według dyscypliny |
| Calendar | dni miesiąca z agregatami, lista sesji dnia, suma tygodnia |
| Session Details | metadane sesji, serwerowe podsumowanie, wpisy w kolejności, snapshoty i wyniki obliczeń |
| Areas | typ i lokalizacja miejsca, liczba dróg/sektorów, official/favorite/recently used, liczba projektów |

Istotne obserwacje:

- React nie ma jeszcze klienta HTTP ani warstwy server state.
- Calendar grupuje mocki po dacie i sam sumuje `score`.
- Session Details sam sumuje punkty i ruchy.
- Po integracji agregaty mają przychodzić z API; frontend może je formatować, ale nie może być ich źródłem prawdy.
- Obecne pole `score` jest niejednoznaczne i nie może wejść bez zmian do kontraktu API.

### Flutter

Flutter ma już formularz „Log done session”, który zbiera:

- miejsce,
- dzień i godzinę rozpoczęcia,
- czas trwania,
- wiele wpisów w jednej sesji,
- nazwę lub wybór drogi,
- profil wysiłku,
- wycenę,
- styl,
- informację o ukończeniu,
- całkowitą oraz wykonaną liczbę ruchów.

Obecnie miejsca, drogi, wyceny, style i profile są zapisane na stałe, wpisy istnieją tylko w pamięci, a zapis kończy się komunikatem `Snackbar`. Dlatego naturalnym pierwszym commandem nie jest pusty `POST session` i kilkanaście ręcznych requestów, tylko atomowy zapis sesji wraz z początkową listą wpisów.

Model prezentacyjny Fluttera zawiera sformatowaną datę, tekst czasu i kolor. Nie są to pola API. Backend zwraca surowe dane (`LocalDate`, minuty, enum, wartości dziesiętne), a klient odpowiada za prezentację.

### Najważniejsze rozbieżności do usunięcia

1. `score`/`points` rozdzielamy na:
   - `classicPoints` i `adjustedLoad` dla wpisu,
   - `classicLoad` i `adjustedLoad` dla sesji.
2. Flutter obecnie miesza EDL profilu z liczbą ruchów. Docelowo:
   - `EffortProfile.baseEdl` pochodzi ze słownika API,
   - `TrainingEntry.totalMoves` wprowadza użytkownik,
   - klient wysyła `effortProfileId` i `totalMoves`, nigdy własne `baseEdl`.
3. Style, profile i wyceny różnią się pomiędzy mockami. PostgreSQL staje się jedynym źródłem słowników.
4. `sessionDate` jest lokalną datą treningu i występuje razem z IANA `timeZoneId`; nie wyliczamy dnia kalendarza z aktualnej strefy urządzenia.
5. Nowsza decyzja produktowa mówi, że sesja nie przechowuje `primaryDiscipline`. API wylicza charakter sesji z wpisów: `BOULDER`, `ROUTE`, `CIRCUIT` albo `MIXED`.
6. `goal`, `method`, achievements i cele dashboardowe nie mają jeszcze stabilnych reguł. Nie mogą blokować pierwszego przepływu.

## 3. Rekomendowany stos technologiczny

Wybieramy technologie aktualne, ale standardowe dla komercyjnych aplikacji Spring. Nie wprowadzamy narzędzia tylko dlatego, że jest modne.

| Obszar | Rekomendacja | Powód |
|---|---|---|
| Język | Java 25 LTS | aktualne LTS dla nowego projektu; rekordy, nowoczesny język i długi horyzont utrzymania |
| Framework | Spring Boot 4.1.x | aktualna stabilna linia, zarządzanie kompatybilnymi wersjami zależności |
| Build | Maven 3.9.x + Maven Wrapper | powszechny w Spring, prosty do nauki, taki sam build lokalnie i w CI |
| HTTP | Spring MVC | klasyczne blokujące REST dobrze pasuje do JPA i PostgreSQL |
| Walidacja | Jakarta Bean Validation | standardowa walidacja kształtu requestów |
| ORM | Spring Data JPA + Hibernate | powszechny stos dla zapisu agregatów i zwykłych odczytów |
| Baza | PostgreSQL 18 | jedna prawdziwa baza w local, testach i później produkcji |
| Migracje | Flyway | jawne, wersjonowane migracje SQL jako źródło schematu |
| Dokumentacja API | springdoc-openapi 3.x | OpenAPI i interaktywne Swagger UI zgodne ze Spring Boot 4 |
| Moduły | Spring Modulith 2.1.x | kontrola granic modularnego monolitu bez mikroserwisów |
| Monitoring | Spring Boot Actuator | health check od pierwszego uruchomienia |
| Testy | JUnit Jupiter, AssertJ, Mockito, Spring Test, Testcontainers | standardowy stos testowy Springa |

Na dzień przygotowania planu aktualne stabilne wersje to Spring Boot 4.1.0, Java 25 LTS i Maven 3.9.16. Konkretne wersje patch przypinamy podczas generowania projektu i aktualizujemy kontrolowanie. Zależności zarządzane przez Spring Boot nie dostają ręcznych wersji w `pom.xml`.

Jeśli środowisko uruchomieniowe nie obsłuży Java 25, bezpiecznym wariantem jest Java 21 LTS. Nie obniżamy jednak wersji „na zapas”; sprawdzamy JDK, IDE, Docker i hosting przed utworzeniem projektu.

### Zależności początkowe

- `spring-boot-starter-web`,
- `spring-boot-starter-validation`,
- `spring-boot-starter-data-jpa`,
- `spring-boot-starter-flyway`,
- `flyway-database-postgresql`,
- sterownik PostgreSQL jako zależność runtime,
- `spring-boot-starter-actuator`,
- kompatybilny `springdoc-openapi-starter-webmvc-ui` 3.x,
- `spring-boot-starter-test`,
- `spring-boot-testcontainers`,
- Testcontainers PostgreSQL,
- testowy starter Spring Modulith.

### Czego nie dodajemy na starcie

- WebFlux i R2DBC — aplikacja nie ma reaktywnego problemu do rozwiązania;
- Axon, event sourcing i generyczny command bus — nie są potrzebne do lekkiego CQRS;
- Kafka/RabbitMQ — moduły działają w jednym procesie i jednej transakcji;
- Redis — najpierw mierzymy realne zapytania;
- H2 — testy bazy mają wykrywać zachowanie PostgreSQL;
- Lombok — rekordy i jawny kod są lepsze edukacyjnie, a `@Data` jest ryzykowne na encjach JPA;
- MapStruct — pierwsze mapowania wykonujemy jawnie; dodajemy go dopiero, gdy powtarzalność będzie realnym problemem;
- QueryDSL/jOOQ — najpierw projekcje JPA, JPQL, native SQL lub `JdbcClient`; jOOQ rozważymy przy rozbudowanej analityce;
- Spring Security — dodamy po pierwszym lokalnym przepływie, ale własność danych uwzględniamy od pierwszej migracji.

## 4. Architektura: modularny monolit

Jeden backend, jeden proces, jeden plik JAR i jedna baza PostgreSQL. Granice funkcjonalne utrzymujemy przez pakiety oraz test Spring Modulith, nie przez wiele mikroserwisów ani osobne moduły Maven.

Proponowany układ:

```text
backend/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/
└── src/
    ├── main/
    │   ├── java/pl/climbbetter/
    │   │   ├── ClimbBetterApplication.java
    │   │   ├── identity/
    │   │   ├── catalog/
    │   │   ├── training/
    │   │   ├── reporting/
    │   │   └── shared/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       └── db/migration/
    └── test/
        ├── java/pl/climbbetter/
        └── resources/
```

### Moduły aplikacji

| Moduł | Odpowiedzialność |
|---|---|
| `identity` | `UserId`, użytkownik developerski, później integracja ze Spring Security/OAuth |
| `catalog` | `Area`, opcjonalny `Sector`, `Climb`, `Grade` oraz statusy ulubionych/projektów |
| `training` | sesje, wpisy, style, profile wysiłku, relative effort, poziom użytkownika, obliczenia, snapshoty i reewaluacja |
| `reporting` | read modele Dashboard, Calendar i późniejszej analityki |
| `shared` | wyłącznie małe elementy przekrojowe: zegar, wspólny identyfikator użytkownika, obsługa błędów; bez katalogu `utils` |

Zależności kodu mają być jednokierunkowe (strzałka wskazuje moduł używany):

```text
reporting ──uses──> training (public query API)
reporting ──uses──> catalog  (public query API)
training  ──uses──> catalog  (public application API)
training  ──uses──> identity (current user)
catalog   ──uses──> identity (ownership)
```

`training` może korzystać z publicznego API `identity` i `catalog`. `catalog` korzysta z tożsamości przy własności prywatnych miejsc i dróg. `reporting` może korzystać z publicznych projekcji danych treningowych i katalogowych. Moduły nie importują klas z pakietów `internal` innych modułów.

Każdy moduł ma układ package-by-feature z ukrytym wnętrzem, przykładowo:

```text
training/
├── TrainingOperations.java
├── TrainingQueries.java
└── internal/
    ├── web/
    ├── application/
    │   ├── command/
    │   └── query/
    ├── domain/
    │   └── calculation/
    └── persistence/
```

Test architektury powstaje w pierwszej iteracji i uruchamia `ApplicationModules.of(ClimbBetterApplication.class).verify()`. Ma wykrywać cykle oraz dostęp do wnętrza obcych modułów.

### Warstwy wewnątrz modułu

```text
REST controller
    ↓
application use case / command or query handler
    ↓
domain model and domain services
    ↓
repository port / persistence adapter
```

- Kontroler mapuje HTTP, nie zawiera logiki domenowej.
- Application handler ustala transakcję i orkiestruje przypadek użycia.
- Domena pilnuje niezmienników i wykonuje obliczenia.
- JPA jest szczegółem persystencji; encje nie są zwracane bezpośrednio jako JSON.
- DTO API są niemutowalnymi rekordami Javy.

## 5. CQRS w Javie — odpowiednik podejścia z .NET

Nie kopiujemy MediatR 1:1. W typowej aplikacji Spring centralny mediator jest dodatkową abstrakcją, która ukrywa zależności i utrudnia naukę przepływu programu.

Stosujemy lekki CQRS na poziomie kodu:

```text
Command endpoint
  → CreateTrainingSessionHandler.handle(command)
  → agregat + repozytorium JPA
  → jedna transakcja zapisu

Query endpoint
  → GetTrainingSessionHandler.handle(query)
  → projekcja DTO / zapytanie read-only
  → brak modyfikacji domeny
```

Przykładowe nazwy:

- `CreateTrainingSessionCommand` — rekord zawierający dane przypadku użycia,
- `CreateTrainingSessionHandler` — konkretny bean z `@Transactional`,
- `UpdateTrainingEntryCommand` i `UpdateTrainingEntryHandler`,
- `GetTrainingSessionQuery` i `GetTrainingSessionHandler`,
- `GetCalendarMonthQuery` i `GetCalendarMonthHandler`.

Kontroler wstrzykuje konkretny handler albo małą fasadę publiczną modułu, a nie `IMediator`/`CommandBus`. Dzięki temu zależności są jawne, łatwo przejść debuggerem cały request, a kompilator pilnuje kontraktu.

### Granice lekkiego CQRS

- Komendy i zapytania mają osobne klasy, modele wejścia i odpowiedzi.
- Komendy pracują na agregatach i repozytoriach JPA.
- Zapytania zwracają dedykowane projekcje przygotowane pod ekran, nie encje JPA.
- Zapis i odczyt używają tej samej bazy PostgreSQL.
- Nie tworzymy osobnej bazy read model ani eventual consistency.
- Dla prostych odczytów używamy projekcji Spring Data JPA.
- Dla Dashboard/Calendar dopuszczamy JPQL, native SQL lub `JdbcClient`, bez wymuszania ładowania całego agregatu.
- Fizyczny read model, materialized view lub zdarzenia wdrażamy dopiero po pomiarze kosztu zapytań.

### Kiedy użyć zdarzeń

Zdarzenia domenowe mają sens, gdy zakończenie sesji uruchomi niezależne działania, np. osiągnięcia, cele lub aktualizację projekcji. Na początku podsumowanie sesji jest częścią tej samej transakcji i nie powinno być asynchroniczne.

Jeżeli później użyjemy zdarzeń między modułami, wybieramy Spring Modulith i jego trwały rejestr publikacji. Nie używamy zwykłego `@Async` do krytycznych aktualizacji, ponieważ awaria procesu mogłaby zgubić zdarzenie.

## 6. Model domenowy pierwszych iteracji

### Agregat `TrainingSession`

`TrainingSession` jest aggregate rootem. Odpowiada za kolejność wpisów i spójne podsumowanie.

Minimalne dane:

- `id` — UUID serwera,
- `userId` — właściciel,
- `clientSessionId` — UUID klienta dla retry i przyszłego offline,
- `sessionDate` — `LocalDate`,
- `timeZoneId` — poprawny identyfikator `ZoneId`, np. `Europe/Warsaw`,
- `areaId`,
- `sessionName`,
- `durationMinutes`, domyślnie 60,
- opcjonalne `startedAtUtc` i `endedAtUtc` jako `Instant`,
- `notes`,
- `createdAt`, `updatedAt`,
- `version` do optimistic locking,
- cache podsumowania:
  - `totalMoves`,
  - `classicLoad`,
  - `adjustedLoad`,
  - `averageMoveIntensity`,
  - `entryCount`,
  - `completedClimbsCount`,
  - `maxCompletedGradeName`.

Nie zapisujemy `primaryDiscipline`. Read model wylicza dyscyplinę z typów wpisów.

### `TrainingEntry`

Minimalne dane wejściowe:

- `id` i opcjonalny `clientEntryId`,
- `entryOrder`,
- opcjonalne `attemptBlockId` i `attemptNumber`,
- `climbId` albo dane wystarczające do rozwiązania/utworzenia drogi,
- opcjonalny `clientClimbId`,
- `climbName`, `climbType`,
- `gradeId` poza rozgrzewką,
- `entryStyleId`,
- `effortProfileId`,
- `totalMoves`,
- `executedMoves`,
- `isCompleted`,
- `notes`,
- `createdAt`, `updatedAt`, `version`.

Backend ma obsłużyć trzy scenariusze drogi:

1. znane `climbId` — wpis wiąże się z katalogiem;
2. znane `clientClimbId` — backend odnajduje wcześniejsze mapowanie;
3. tylko dane robocze — backend tworzy prywatną, niezweryfikowaną drogę i zwraca stabilne `climbId`.

### Katalog

Na MVP stosujemy model wynikający z produktu:

```text
Area
 └── opcjonalny Sector
      └── Climb
```

Nie budujemy od razu uniwersalnego drzewa `kontynent → kraj → region → dolina → skała`. `Area` przechowuje typ, kraj/region/miasto i czytelny tekst lokalizacji/breadcrumb. Gdy realne dane pokażą potrzebę pełnej hierarchii, dodamy `parentAreaId` w osobnej migracji. Unikamy równoczesnego modelowania sektora jako encji i węzła drzewa.

Encje katalogowe pierwszego zakresu:

- `Area`,
- `Sector`,
- `Climb`,
- `Grade`,
- później `UserAreaStatus` i `UserClimbStatus`.

Konfiguracja modelu należąca do modułu `training`:

- `EntryStyle`,
- `EffortProfile`,
- `RelativeEffortBand`,
- `UserGradeReference`.

Drogi i miejsca archiwizujemy. Usunięcie lub zmiana katalogu nie może usunąć ani zmienić starego wpisu treningowego.

### Tożsamość przed pełnym logowaniem

Od początku definiujemy port `CurrentUserProvider`. Implementacja `local` i `test` zwraca stały UUID seedowanego użytkownika, ale request nigdy nie zawiera dowolnego `userId`. Implementacja developerska nie może zostać aktywowana w profilu produkcyjnym. Po dodaniu Spring Security port zacznie czytać stabilny identyfikator z claims tokenu bez zmiany przypadków użycia.

## 7. Model obliczeniowy

Obliczenia są osobnym, czystym fragmentem Javy: bez Springa, JPA, repozytoriów i aktualnego czasu. Wejście oraz wynik są niemutowalnymi rekordami.

### Serwisy

| Serwis | Odpowiedzialność |
|---|---|
| `TrainingLoadCalculator` | obliczenia pojedynczego wpisu |
| `RelativeEffortResolver` | wybór dokładnie jednego aktywnego bandu dla różnicy wycen |
| `UserGradeReferenceResolver` | poziom użytkownika właściwy dla daty sesji i rodzaju aktywności |
| `EntrySnapshotFactory` | utworzenie pełnego, niezmiennego snapshotu wejść i wyniku |
| `SessionSummaryCalculator` | agregaty po wszystkich aktywnych wpisach sesji |
| `ClimbResolver` | katalog, client ID i prywatna droga użytkownika |
| `HistoryReevaluationService` | późniejsza jawna reewaluacja z audytem |

### Wzory wersji 1

```text
LengthDivisor = BaseEdl + 0.2 × TotalMoves
MoveIntensity = GradePoints / LengthDivisor
ClassicPoints = GradePoints × StyleMultiplier × ExecutedMoves / TotalMoves
RelativeGradeDiff = GradeIndex - CurrentLevelIndex
AdjustedLoad = ExecutedMoves × MoveIntensity × StyleMultiplier × RelativeEffortMultiplier
```

Dla rozgrzewki:

- ruchy zwiększają `totalMoves` sesji,
- `moveIntensity = 0`,
- `classicPoints = 0`,
- `adjustedLoad = 0`,
- wpis nie wchodzi do `averageMoveIntensity`.

### Precyzja

- W Javie używamy `BigDecimal`, nigdy `double`, dla punktów, mnożników i loadu.
- Definiujemy jedną politykę `CalculationPrecision`: skala pośrednia, skala zapisu i `RoundingMode`.
- Nie zaokrąglamy każdego kroku tylko dlatego, że UI pokazuje dwie cyfry.
- API może zwracać wartości zapisane z większą dokładnością, a frontend formatuje prezentację.
- `calculationModelVersion = 1` zapisujemy na każdym wpisie.

### Snapshot wpisu

Każdy wpis utrwala co najmniej:

- nazwę drogi i Area/breadcrumb,
- nazwę, punkty oraz indeks wyceny,
- poziom użytkownika z dnia sesji,
- nazwę profilu i `baseEdl`,
- nazwę stylu, typ wyniku i mnożnik,
- różnicę wycen oraz relative effort multiplier,
- length divisor,
- move intensity,
- classic points,
- adjusted load,
- wersję modelu obliczeń.

Frontend nie może wysłać tych wyników jako źródła prawdy. Może je otrzymać w odpowiedzi, aby wyjaśnić użytkownikowi obliczenie.

### Podsumowanie sesji

Po utworzeniu, zmianie lub usunięciu wpisu w tej samej transakcji przeliczamy:

- sumę `executedMoves`,
- sumę `classicPoints`,
- sumę `adjustedLoad`,
- średnią `moveIntensity` wpisów innych niż rozgrzewka,
- liczbę wpisów,
- liczbę ukończonych przejść,
- najwyższą ukończoną wycenę.

Snapshoty są źródłem historycznych agregatów. Zmiana aktualnego słownika nie przelicza historii automatycznie.

## 8. Niezmienniki i walidacja

Bean Validation sprawdza format requestu, ale reguły biznesowe pozostają w domenie/application service.

### Sesja

- użytkownik pochodzi z `CurrentUserProvider`, nie z body requestu;
- `sessionDate`, `timeZoneId`, `areaId` i `sessionName` są wymagane;
- `timeZoneId` musi być rozpoznawalny przez `ZoneId`;
- `durationMinutes > 0`;
- jeśli oba czasy istnieją, `startedAtUtc <= endedAtUtc`;
- `(user_id, client_session_id)` jest unikalne;
- ponowienie identycznej komendy zwraca istniejący zasób;
- ten sam client ID z innym payloadem zwraca `409 Conflict`;
- każda operacja sprawdza właściciela sesji.

### Wpis

- `entryOrder > 0` i jest unikalne w sesji;
- `totalMoves > 0`;
- `0 <= executedMoves <= totalMoves`;
- poza rozgrzewką grade, style i effort profile są wymagane i aktywne;
- wpis wskazuje istniejącą drogę albo zawiera dane potrzebne do utworzenia prywatnej drogi;
- zarchiwizowana lub niedostępna droga nie może być użyta w nowym wpisie;
- `(session_id, client_entry_id)` jest unikalne, jeśli client ID istnieje;
- request nie zawiera pól obliczeniowych i snapshotów;
- styl typu `ASCENT` musi być zgodny z `isCompleted=true`, a `ATTEMPT` z `false`;
- dla ukończonego przejścia `executedMoves = totalMoves`;
- źródłem rozgrzewki jest `EntryStyle.resultType = WARMUP`; profil musi być z nim zgodny.

### Słowniki i okresy

- aktywne `RelativeEffortBand` nie mogą się nakładać i muszą obsłużyć wartości poza zwykłym zakresem;
- `UserGradeReference` dla tego samego użytkownika i zakresu aktywności nie może mieć nakładających się okresów;
- brak poziomu użytkownika jest kontrolowanym błędem domenowym/onboardingowym, nie cichym mnożnikiem domyślnym;
- encje katalogowe są archiwizowane zamiast kasowania;
- FK z wpisów do katalogów używają `RESTRICT`/`NO ACTION`, nigdy kasowania kaskadowego historii.

## 9. PostgreSQL, Flyway i JPA

### Lokalna baza od pierwszego dnia

W `infra/compose.yaml` tworzymy usługę PostgreSQL 18:

- baza `climbbetter`,
- osobny użytkownik aplikacyjny,
- nazwany volume,
- healthcheck,
- port konfigurowany przez zmienne środowiskowe,
- `.env.example` bez sekretów,
- jedna jawna komenda `docker compose up -d db`.

Spring otrzymuje URL, login i hasło przez zmienne środowiskowe. Sekretów nie zapisujemy w `application.yml`.

### Schematy i tabele pierwszego slice'a

Rekomendowane schematy logiczne:

- `identity` — `users`,
- `catalog` — `areas`, `sectors`, `climbs`, `grades`,
- `training` — `entry_styles`, `effort_profiles`, `relative_effort_bands`, `user_grade_references`, `training_sessions`, `training_entries`.

Nie tworzymy jeszcze tabel dla goals, cycles, achievements, social ani pełnego sync.

### Kolejność migracji

```text
V001__create_schemas_and_users.sql
V002__create_catalog.sql
V003__create_training.sql
V004__seed_reference_data.sql
V005__add_required_indexes_and_constraints.sql
```

Dane niezbędne do poprawnego działania aplikacji — wyceny, style, profile i bandy — są wersjonowane razem ze schematem. Dane demonstracyjne Bronx/Garaż i przykładowe drogi trafiają do migracji aktywowanych tylko profilem `local`/`test`, nie do produkcyjnego seedu.

Zasady migracji:

- Flyway jest jedynym właścicielem DDL;
- `spring.jpa.hibernate.ddl-auto=validate`;
- `spring.jpa.open-in-view=false`;
- nie używamy `create`, `update` ani `create-drop` dla schematu aplikacji;
- istniejącej migracji po użyciu nie edytujemy — dodajemy następną;
- migracje uruchamiają się również w testach Testcontainers;
- stare migracje EF Core pozostają tylko w `backend-net` jako referencja i nie są tłumaczone automatycznie.

### Typy danych

- identyfikatory: PostgreSQL `uuid` / Java `UUID`,
- lokalna data sesji: `date` / `LocalDate`,
- czas absolutny: `timestamptz` / `Instant`,
- nazwa strefy IANA: `text` / walidowane `ZoneId`,
- wyniki i mnożniki: `numeric` / `BigDecimal`,
- wersja optimistic locking: `bigint`,
- timestampy audytowe generowane w aplikacji przy użyciu wstrzykniętego `Clock`.

### Constraints i indeksy od początku

- unique `(user_id, client_session_id)`,
- unique `(session_id, client_entry_id)` dla niepustego client ID,
- unique `(session_id, entry_order)`,
- indeks `(user_id, session_date desc)`,
- indeksy FK: `area_id`, `climb_id`, `grade_id`, `entry_style_id`, `effort_profile_id`,
- check `duration_minutes > 0`,
- check `entry_order > 0`,
- check `total_moves > 0`,
- check `executed_moves >= 0 AND executed_moves <= total_moves`,
- check poprawnej kolejności dat obowiązywania i czasu sesji tam, gdzie da się ją zagwarantować w DB.

Walidacja aplikacyjna daje czytelny błąd, a constraint bazy jest ostatnią linią ochrony przed race condition i błędem w kodzie.

### Zasady JPA

- encje JPA nie opuszczają persistence/application layer;
- jawne nazwy tabel, kolumn, schematów i relacji;
- ostrożne `equals/hashCode` oparte na stabilnej tożsamości;
- brak `@Data` i automatycznego `toString` po relacjach;
- kolekcje domyślnie lazy, ale zapytania jawnie pobierają potrzebny graf;
- kontrolujemy N+1 testami i logami SQL;
- komendy otwierają transakcję na application handlerze;
- query handlery używają `@Transactional(readOnly = true)` tam, gdzie korzystają z JPA;
- nie polegamy na lazy loadingu podczas serializacji HTTP.

## 10. Kontrakt REST API

### Konwencje

- prefiks `/api/v1`,
- JSON w `camelCase`,
- enumy w stabilnej formie `UPPER_SNAKE_CASE`,
- daty `YYYY-MM-DD`, czasy jako ISO-8601 UTC,
- UUID jako string,
- standardowe statusy HTTP,
- błędy jako RFC 9457 `ProblemDetail`,
- błędy walidacji zawierają mapę pól i stabilny kod problemu,
- brak encji JPA i stack trace w odpowiedzi,
- OpenAPI generowane i sprawdzane w repozytorium,
- CORS ograniczony do lokalnych adresów Reacta podczas developmentu.

### Endpointy potrzebne formularzowi

```http
GET /api/v1/areas?scope=my
GET /api/v1/areas/{areaId}/climbs?query=czerwone&limit=10
GET /api/v1/grades?scale=FONT_BOULDER
GET /api/v1/entry-styles
GET /api/v1/effort-profiles
```

Słowniki zwracają identyfikator, nazwę, opis, kolejność i dane informacyjne dla UI. Backend podczas zapisu zawsze ponownie pobiera aktualny rekord z bazy; nie ufa punktom ani mnożnikom odesłanym przez klienta.

### Atomowy zapis ukończonej sesji

```http
POST /api/v1/training-sessions
```

Przykładowa komenda:

```json
{
  "clientSessionId": "50cd74af-c3c0-4bab-91cf-58780849c2d4",
  "sessionDate": "2026-08-13",
  "timeZoneId": "Europe/Warsaw",
  "areaId": "11111111-1111-1111-1111-111111111111",
  "sessionName": "Bronx - boulder volume",
  "durationMinutes": 90,
  "startedAtUtc": "2026-08-13T16:00:00Z",
  "notes": "Mocny dzień na przewieszeniu.",
  "entries": [
    {
      "clientEntryId": "e20effeb-2a97-44c2-a024-64aac02bc45d",
      "entryOrder": 1,
      "attemptBlockId": "2a9d26b2-24fb-44e6-a553-498528401bbf",
      "attemptNumber": 1,
      "climbId": null,
      "clientClimbId": "e19d397e-e95e-48b5-960d-f3283266214b",
      "climbName": "Czerwone placki",
      "climbType": "BOULDER",
      "effortProfileId": "22222222-2222-2222-2222-222222222222",
      "gradeId": "33333333-3333-3333-3333-333333333333",
      "entryStyleId": "44444444-4444-4444-4444-444444444444",
      "totalMoves": 8,
      "executedMoves": 5,
      "isCompleted": false,
      "notes": null
    }
  ]
}
```

Komenda celowo nie zawiera `gradePoints`, `baseEdl`, mnożników ani obliczonych wyników.

Odpowiedź `201 Created` zwraca nagłówek `Location` oraz pełny read model zapisanej sesji:

```json
{
  "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "clientSessionId": "50cd74af-c3c0-4bab-91cf-58780849c2d4",
  "sessionDate": "2026-08-13",
  "timeZoneId": "Europe/Warsaw",
  "sessionName": "Bronx - boulder volume",
  "discipline": "BOULDER",
  "area": {
    "id": "11111111-1111-1111-1111-111111111111",
    "name": "Bronx"
  },
  "durationMinutes": 90,
  "notes": "Mocny dzień na przewieszeniu.",
  "summary": {
    "totalMoves": 5,
    "classicLoad": 28.125,
    "adjustedLoad": 22.0982,
    "averageMoveIntensity": 8.9286,
    "entryCount": 1,
    "completedClimbsCount": 0,
    "maxCompletedGradeName": null
  },
  "entries": [
    {
      "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
      "entryOrder": 1,
      "climbId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
      "climbResolutionStatus": "USER_PRIVATE",
      "climbName": "Czerwone placki",
      "climbType": "BOULDER",
      "gradeName": "6B",
      "entryStyleName": "Attempt normalny",
      "effortProfileName": "Bald",
      "totalMoves": 8,
      "executedMoves": 5,
      "isCompleted": false,
      "calculation": {
        "modelVersion": 1,
        "lengthDivisor": 5.6,
        "moveIntensity": 8.9286,
        "styleMultiplier": 0.9,
        "relativeEffortMultiplier": 0.55,
        "classicPoints": 28.125,
        "adjustedLoad": 22.0982
      }
    }
  ]
}
```

### Wspólny odczyt dla Reacta i Fluttera

```http
GET /api/v1/training-sessions?from=&to=&areaId=&limit=20&cursor=
GET /api/v1/training-sessions/{sessionId}
```

Lista zwraca lekkie elementy zawierające:

- ID i lokalną datę,
- nazwę sesji oraz Area,
- wyliczoną dyscyplinę,
- czas trwania i notatkę,
- oba rodzaje loadu,
- liczbę ruchów i wpisów,
- najwyższą ukończoną wycenę,
- maksymalnie kilka `gradeHighlights` dla karty mobile.

Kolor, `dateLabel`, tekst czasu i tłumaczenia pozostają po stronie klienta.

### Mutacje po pierwszym przepływie

```http
PATCH  /api/v1/training-sessions/{sessionId}
POST   /api/v1/training-sessions/{sessionId}/entries
PATCH  /api/v1/training-sessions/{sessionId}/entries/{entryId}
DELETE /api/v1/training-sessions/{sessionId}/entries/{entryId}
```

Każda zmiana wpisu ponownie tworzy właściwy snapshot i przelicza podsumowanie sesji w jednej transakcji.

### Read modele kolejnych ekranów

```http
GET /api/v1/calendar/month?year=2026&month=8
GET /api/v1/calendar/day?date=2026-08-13
GET /api/v1/dashboard/summary
GET /api/v1/analytics/load?from=2026-06-01&to=2026-08-31&bucket=WEEK
GET /api/v1/areas?scope=my
GET /api/v1/areas/{areaId}
GET /api/v1/areas/{areaId}/climbs
```

Nie zmuszamy frontendu do pobrania setek sesji i samodzielnego liczenia kalendarza. Query endpoint zwraca projekcję dokładnie odpowiadającą ekranowi.

### Mapa ekranów na endpointy

| Klient/ekran | Pierwszy endpoint | Kolejny docelowy endpoint |
|---|---|---|
| Flutter — formularz | słowniki, Areas, search climbs, `POST /training-sessions` | osobne mutacje i sync batch |
| Flutter — Board | `GET /training-sessions` | dedykowany home summary, jeśli będzie potrzebny |
| React — Session Details | `GET /training-sessions/{id}` | ten sam kontrakt po dodaniu edycji |
| React — Dashboard feed | `GET /training-sessions` | `GET /dashboard/summary` |
| React — Calendar | lista z zakresem dat jako krok przejściowy | `GET /calendar/month` i `/day` |
| React — Areas | `GET /areas?scope=my` | szczegóły Area i statusy użytkownika |

## 11. Strategia testów od pierwszego commita

Testy nie są etapem po implementacji. Każdy pionowy fragment zawiera test jednostkowy, integracyjny i kontrakt HTTP odpowiedni do ryzyka.

### Poziomy testów

| Poziom | Narzędzia | Co sprawdzamy |
|---|---|---|
| Czysta domena | JUnit Jupiter + AssertJ | kalkulator, resolvery, niezmienniki, podsumowania |
| Test parametryzowany/golden | JUnit `@ParameterizedTest` | przykłady uzgodnione z arkuszem i granice modelu |
| Persystencja | `@DataJpaTest` + PostgreSQL Testcontainers | mapowanie JPA, constraints, zapytania i migracje |
| Web slice | Spring MVC Test + MockMvc | statusy HTTP, JSON, Bean Validation i `ProblemDetail` |
| Moduł | Spring Modulith test | granice i współpraca przypadków użycia w module |
| Pełna aplikacja | `@SpringBootTest` + `@ServiceConnection` + PostgreSQL Testcontainers | REST → domena → Flyway/JPA → REST |
| Kontrakt klientów | OpenAPI + testy mapperów | zgodność TypeScript/Dart z payloadem API |
| Demo end-to-end | działające trzy aplikacje | zapis Flutter → odczyt React/Flutter |

### Testy kalkulatora wymagane przed endpointem zapisu

- pełne ukończone przejście,
- nieukończona próba z częścią ruchów,
- rozgrzewka,
- minimalna i duża liczba ruchów,
- każda granica `RelativeEffortBand`,
- wartości poniżej i powyżej skonfigurowanych zwykłych różnic,
- brak albo nakładanie się bandów jako błąd konfiguracji,
- brak referencji poziomu użytkownika,
- precyzja `BigDecimal` i uzgodnione zaokrąglenie,
- zgodność `isCompleted`, stylu oraz wykonanych ruchów,
- agregacja sesji z mieszanką przejść, prób i rozgrzewki,
- jawny test decyzji: zwykła czy ważona średnia `moveIntensity`.

Przykłady liczb z dokumentacji i obecnych mocków przenosimy do testów jako nazwane przypadki biznesowe. Oczekiwane wyniki nie mogą być w teście wyliczone tym samym algorytmem co kod produkcyjny.

### Testy integracyjne pierwszego slice'a

1. Pusta baza przyjmuje wszystkie migracje Flyway.
2. Seed zawiera dokładnie jednego dev-usera oraz wymagane, aktywne słowniki.
3. `POST /training-sessions` zapisuje sesję i wszystkie wpisy atomowo.
4. Błąd jednego wpisu wycofuje całą sesję.
5. Wynik, snapshot i podsumowanie w DB odpowiadają golden case.
6. `GET /training-sessions/{id}` nie zależy od późniejszej zmiany nazwy/wyceny w katalogu.
7. Identyczny retry z `clientSessionId` nie tworzy duplikatu.
8. Ten sam client ID z innym payloadem daje `409`.
9. Wpis bez katalogowego climb tworzy prywatny climb i stabilne mapowanie client ID.
10. Użytkownik nie odczytuje ani nie zmienia cudzej sesji.
11. `SessionDate` pozostaje tym samym dniem niezależnie od strefy klienta.
12. Późniejsze add/update/delete entry przelicza cache w tej samej transakcji.

Testy bazy nigdy nie używają H2. `@ServiceConnection` przekazuje Springowi dane połączenia do kontenera PostgreSQL, a Flyway buduje schemat tak samo jak lokalnie.

### Szybkość zestawu testowego

- testy czystej domeny pozostają bez Spring context i wykonują się w sekundach,
- kontener PostgreSQL współdzielimy w obrębie zestawu testowego zamiast uruchamiać dla każdej metody,
- `mvn test` uruchamia szybkie testy,
- `mvn verify` uruchamia również testy integracyjne i kontraktowe,
- CI wykonuje `./mvnw verify` na każdym pull requeście,
- żadnego testu integracyjnego nie oznaczamy jako opcjonalnego tylko dlatego, że używa Dockera.

## 12. Plan prac krok po kroku

Poniższa kolejność jest backlogiem wykonawczym. Nie przechodzimy do rozbudowanej analityki, zanim nie działa pierwszy pionowy przepływ.

### Etap 0 — domknięcie reguł produktu

- [ ] Uzgodnić, która metryka jest główna w UI: `classicLoad`, `adjustedLoad` czy obie.
- [ ] Zatwierdzić pełną tabelę wycen od `4a` do `9c`: indeksy i punkty.
- [ ] Zatwierdzić jedną listę `EntryStyle` i mnożników; usunąć rozbieżności mocków React/Flutter.
- [ ] Zatwierdzić jedną listę `EffortProfile` i wartości `baseEdl`.
- [ ] Zatwierdzić bandy `RelativeEffort` również dla skrajnych różnic.
- [ ] Potwierdzić, że `isCompleted=true` oznacza `executedMoves=totalMoves`.
- [ ] Potwierdzić, że `EntryStyle.resultType` jest źródłem prawdy dla warmup/ascent/attempt.
- [ ] Potwierdzić, że `totalMoves` sesji oznacza sumę ruchów wykonanych.
- [ ] Potwierdzić, że maksymalna wycena dotyczy ukończonych przejść.
- [ ] Wybrać zwykłą albo ważoną ruchami średnią `moveIntensity`; rekomendacja v1: zwykła średnia wpisów poza rozgrzewką zgodna z obecnym dokumentem.
- [ ] Potwierdzić uproszczony model `Area → optional Sector → Climb` dla MVP.
- [ ] Dodać do formularza Flutter `sessionName` i notatkę sesji albo jawnie zatwierdzić regułę ich tworzenia.
- [ ] Zapisać 5–10 ręcznie policzonych golden cases z realnych sesji.

Rezultat: model v1 jest jednoznaczny i testowalny.

### Etap 1 — bootstrap projektu Java

- [ ] Sprawdzić Java 25, Maven/Docker i wsparcie IDE na komputerze deweloperskim.
- [ ] Wygenerować pojedynczy projekt Spring Boot w `backend`.
- [ ] Dodać Maven Wrapper i ustawić toolchain/release Java 25.
- [ ] Dodać minimalne zależności z sekcji technologicznej.
- [ ] Utworzyć pakiet bazowy `pl.climbbetter` i moduły funkcjonalne.
- [ ] Dodać profile `local`, `test` i bazowy `application.yml`.
- [ ] Wyłączyć Open Session/EntityManager in View.
- [ ] Ustawić Hibernate na `ddl-auto=validate`.
- [ ] Dodać globalny `Clock` UTC.
- [ ] Dodać Actuator i sprawdzić `/actuator/health`.
- [ ] Dodać springdoc oraz sprawdzić `/v3/api-docs` i Swagger UI.
- [ ] Dodać endpoint lub test startowy bez logiki biznesowej.
- [ ] Dodać test granic Spring Modulith.
- [ ] Uruchomić `./mvnw verify`.

Rezultat: aplikacja startuje i ma zielony build, health check oraz OpenAPI.

### Etap 2 — PostgreSQL i migracje

- [ ] Dodać `infra/compose.yaml` z PostgreSQL 18 i healthcheckiem.
- [ ] Dodać `.env.example` i udokumentować komendy local development.
- [ ] Połączyć profil `local` z bazą przez zmienne środowiskowe.
- [ ] Dodać migracje schematów `identity`, `catalog`, `training`.
- [ ] Utworzyć minimalne tabele pierwszego slice'a.
- [ ] Dodać indeksy, unique i check constraints.
- [ ] Dodać stałego dev-usera.
- [ ] Dodać `CurrentUserProvider` z implementacją dostępną wyłącznie w profilach `local` i `test`.
- [ ] Dodać zatwierdzone słowniki i `UserGradeReference` dev-usera.
- [ ] Dodać local fixtures: Bronx, Garaż i kilka dróg.
- [ ] Dodać PostgreSQL Testcontainer z `@ServiceConnection`.
- [ ] Dodać test: Flyway buduje pustą bazę.
- [ ] Dodać testy repozytoriów i constraints.
- [ ] Sprawdzić start aplikacji na lokalnym compose.

Rezultat: realna, odtwarzalna baza działa lokalnie i w testach; nie istnieje alternatywny schemat tworzony przez Hibernate.

### Etap 3 — czysty model obliczeniowy

- [ ] Utworzyć rekordy wejścia i wyniku kalkulatora.
- [ ] Zdefiniować `CalculationPrecision` i wersję modelu v1.
- [ ] Zaimplementować `RelativeEffortResolver`.
- [ ] Zaimplementować `UserGradeReferenceResolver`.
- [ ] Zaimplementować `TrainingLoadCalculator`.
- [ ] Zaimplementować szczególne zasady rozgrzewki.
- [ ] Zaimplementować `SessionSummaryCalculator`.
- [ ] Zaimplementować `EntrySnapshotFactory`.
- [ ] Dodać wszystkie testy jednostkowe i golden cases.
- [ ] Porównać wyniki z rzeczywistym arkuszem/danymi użytkownika.

Rezultat: model obliczeń jest zweryfikowany bez uruchamiania Springa i bazy.

### Etap 4 — pierwszy vertical slice backendu

- [ ] Zaimplementować publiczne API modułu `catalog` dla Areas, Grades i wyszukiwarki climbs.
- [ ] Zaimplementować query API modułu `training` dla EntryStyles i EffortProfiles.
- [ ] Zaimplementować `ClimbResolver` dla znanego, klienckiego i prywatnego climb.
- [ ] Zaimplementować agregat sesji oraz persistence mapping.
- [ ] Zaimplementować `CreateTrainingSessionCommand` z listą wpisów.
- [ ] Zaimplementować transakcyjny `CreateTrainingSessionHandler`.
- [ ] Dodać idempotencję `clientSessionId` i fingerprint komendy.
- [ ] Zaimplementować snapshoty i cache summary.
- [ ] Zaimplementować `GET /training-sessions/{id}` jako projekcję DTO.
- [ ] Zaimplementować stronicowaną listę sesji.
- [ ] Dodać Bean Validation oraz walidację domenową.
- [ ] Dodać centralny `@RestControllerAdvice` i `ProblemDetail`.
- [ ] Dodać wszystkie testy integracyjne pierwszego slice'a.
- [ ] Uzupełnić przykłady OpenAPI i wyeksportować specyfikację do repozytorium.

Rezultat: curl/Swagger potrafi zapisać pełną sesję, zobaczyć obliczenia i odczytać ją z PostgreSQL.

### Etap 5 — podłączenie formularza Flutter

- [ ] Dodać konfigurowalny URL API (`--dart-define`); dla emulatora Android przewidzieć `10.0.2.2` zamiast hostowego `localhost`.
- [ ] Dodać klienta HTTP, serializację DTO i interfejs `TrainingRepository`.
- [ ] Ukryć HTTP za repository, żeby UI można było testować fake'iem i później dodać local-first.
- [ ] Pobrać Areas, wyceny, style i profile z API.
- [ ] Pobrać sugestie climbs dla wybranego Area.
- [ ] Rozdzielić `effortProfileId` od `totalMoves` w formularzu.
- [ ] Wysyłać surowe `sessionDate`, IANA `timeZoneId` i poprawnie wyliczone opcjonalne `startedAtUtc`.
- [ ] Generować `clientSessionId`, `clientEntryId` i `clientClimbId` po stronie klienta.
- [ ] Zamienić `Snackbar`-stub na prawdziwy `POST /training-sessions`.
- [ ] Obsłużyć loading, walidację, problem details, retry i brak sieci.
- [ ] Po udanym zapisie odświeżyć Board z `GET /training-sessions`.
- [ ] Dodać test repository z fake API oraz widget test formularza.

Rezultat: sesja wprowadzona w aplikacji mobilnej jest obliczona przez Javę i trwała w PostgreSQL.

### Etap 6 — pierwszy odczyt React i koniec głównych mocków

- [ ] Dodać `VITE_API_URL` i mały typowany wrapper nad `fetch`.
- [ ] Dodać TanStack Query dla cache, loading/error/retry i odświeżania.
- [ ] Wygenerować typy TypeScript z OpenAPI albo utrzymywać jawne DTO zgodne z testem kontraktu.
- [ ] Najpierw podłączyć Session Details do `GET /training-sessions/{id}`.
- [ ] Usunąć lokalne sumowanie punktów/ruchów ze strony szczegółów.
- [ ] Podłączyć feed Dashboard do listy sesji.
- [ ] Obsłużyć loading, empty, error i not found.
- [ ] Dodać Vitest + Testing Library + MSW dla stanów UI i mapowania kontraktu.
- [ ] Usunąć odpowiednie dane z `dashboard.mock.ts` dopiero po zielonym przepływie.
- [ ] Wykonać demo: zapis Flutter → odświeżenie React → otwarcie tej samej sesji.

Rezultat: najważniejszy mock treningowy znika z obu klientów.

### Etap 7 — Calendar, Dashboard summary i Areas

- [ ] Zaimplementować projekcję `calendar/month` agregującą po `sessionDate`.
- [ ] Zaimplementować `calendar/day` oraz później `week`.
- [ ] Podłączyć React Calendar i usunąć grupowanie/sumowanie mocków w UI.
- [ ] Ustalić regułę streak i dodać jej testy.
- [ ] Zaimplementować `dashboard/summary` oraz podsumowania dyscyplin.
- [ ] Podłączyć Dashboard i mobile Profile/summary.
- [ ] Zaimplementować `areas?scope=my` z licznikami i statusami użytkownika.
- [ ] Podłączyć React Areas i zastąpić `areas.mock.ts`.
- [ ] Dodać zakresy dat i indeksy na podstawie planów zapytań PostgreSQL.

Rezultat: Dashboard, Calendar, Areas i Session Details korzystają z prawdziwych danych.

### Etap 8 — edycja i bezpieczeństwo historii

- [ ] Dodać update sesji oraz add/update/delete wpisu.
- [ ] Po każdej mutacji przeliczać snapshot i summary atomowo.
- [ ] Dodać optimistic locking oraz `409` dla konfliktu wersji.
- [ ] Dodać archiwizowanie Area/Climb i test niezmienności historii.
- [ ] Dodać `EntryCalculationRevision` z pełnym poprzednim i nowym snapshotem.
- [ ] Dodać jawną reewaluację wybranego zakresu wraz z powodem i audytem.
- [ ] Dodać testy: zmiana słownika nie rusza historii, jawna reewaluacja ją zmienia i pozostawia audyt.

Rezultat: można bezpiecznie poprawiać dane bez cichego przepisywania przeszłości.

### Etap 9 — prawdziwe offline first w Flutter

- [ ] Wybrać lokalną relacyjną bazę Fluttera i zaprojektować lokalne encje niezależnie od modeli UI.
- [ ] Zapisywać sesję lokalnie przed próbą wysłania.
- [ ] Dodać local outbox i stany synchronizacji.
- [ ] Dodać batch/sync API albo wykorzystać idempotentne commandy pojedynczo.
- [ ] Utrzymywać mapowanie client UUID → server UUID.
- [ ] Obsłużyć kolejność zmian, tombstone i retry z backoff.
- [ ] Zdefiniować politykę konfliktów przy edycji z wielu urządzeń.
- [ ] Dodać testy utraty połączenia, restartu aplikacji i wielokrotnego retry.

Rezultat: logging w skałach działa bez sieci, a synchronizacja nie tworzy duplikatów.

### Etap 10 — auth i rozwój produktu

- [ ] Dodać Spring Security jako OAuth2 Resource Server.
- [ ] Wybrać zewnętrznego dostawcę OIDC; backend nie powinien sam przechowywać haseł bez potrzeby.
- [ ] Zamienić dev-usera na `CurrentUserProvider` oparty o claims.
- [ ] Dodać testy izolacji danych użytkowników.
- [ ] Dopiero potem wdrażać goals, cycles, achievements, planowanie sesji i social.
- [ ] Rozważyć eventy Spring Modulith dla niezależnych projekcji i osiągnięć.
- [ ] Rozważyć jOOQ/materialized views dopiero przy realnie trudnych zapytaniach analitycznych.

## 13. Critical path do pierwszego demo

Minimalna ścieżka, której nie rozszerzamy w trakcie:

```text
1. Zatwierdzone słowniki + golden cases
2. Spring Boot + Maven Wrapper + health/OpenAPI
3. PostgreSQL Compose + Flyway + Testcontainers
4. Czysty TrainingLoadCalculator
5. Seed dev-user + Bronx + słowniki + poziom użytkownika
6. GET formularzowych słowników
7. POST całej ukończonej sesji
8. GET listy i szczegółów sesji
9. Flutter zapisuje i odświeża Board
10. React pokazuje listę i szczegóły
11. Jedno ręczne demo + automatyczny test end-to-end API
```

Calendar, dashboard summary, auth i offline sync nie mogą opóźniać punktu 11.

## 14. Codzienny workflow developerski

Docelowe podstawowe komendy:

```bash
docker compose -f infra/compose.yaml up -d db
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
./mvnw test
./mvnw verify
```

Na Windows używamy odpowiednio `mvnw.cmd`. W `backend/README.md` po utworzeniu projektu zapisujemy:

- wymagane wersje JDK i Docker,
- zmienne środowiskowe,
- start i zatrzymanie bazy,
- start backendu,
- adres health check i Swagger UI,
- sposób wyczyszczenia wyłącznie lokalnej bazy developerskiej,
- uruchamianie testów szybkich oraz integracyjnych,
- przykładowe requesty pierwszego przepływu.

### CI od początku

Pierwszy workflow CI powinien:

1. skonfigurować Java 25,
2. uruchomić `./mvnw verify`,
3. uruchomić test granic modułów i Testcontainers,
4. wyeksportować/sprawdzić OpenAPI,
5. później dołączyć build/lint Reacta oraz `flutter analyze` i `flutter test`.

Maven Wrapper, a nie lokalnie zainstalowany Maven, jest źródłem wersji build toola.

## 15. Zasady usuwania mocków

Nie wykonujemy jednego dużego refaktoru frontendów. Każdy mock znika dopiero po spełnieniu czterech warunków:

1. endpoint ma stabilne DTO i przykład OpenAPI,
2. istnieje test integracyjny endpointu na PostgreSQL,
3. ekran obsługuje success, loading, empty i error,
4. test UI używa MSW/fake repository zamiast importu produkcyjnego mocka.

Kolejność:

1. słowniki formularza Flutter,
2. zapis sesji Flutter,
3. lista sesji Flutter Board,
4. React Session Details,
5. React Dashboard feed,
6. React Calendar,
7. React Areas,
8. mobile Profile/load.

Mocki mogą pozostać jako fixtures testowe, ale nie mogą być importowane przez kod produkcyjny zintegrowanego ekranu.

## 16. Definition of Done

### Pierwszy pionowy przepływ

- [ ] `docker compose up -d db` uruchamia zdrowy PostgreSQL.
- [ ] Nowa/pusta baza przechodzi wszystkie migracje Flyway.
- [ ] Backend startuje na profilu local i `/actuator/health` zwraca `UP`.
- [ ] Swagger/OpenAPI pokazuje rzeczywisty kontrakt pierwszego slice'a.
- [ ] Wszystkie golden testy kalkulatora przechodzą.
- [ ] Testcontainers sprawdza zapis, rollback, snapshot, summary i idempotencję.
- [ ] Flutter pobiera słowniki oraz Area z API.
- [ ] Flutter zapisuje zakończoną sesję do PostgreSQL.
- [ ] Flutter Board odczytuje zapisaną sesję z API.
- [ ] React wyświetla tę samą sesję na liście i stronie szczegółów.
- [ ] Punkty i load pokazane w klientach są wynikami backendu.
- [ ] Zintegrowane ekrany nie importują produkcyjnych mocków.
- [ ] `./mvnw verify`, testy Reacta oraz testy Fluttera są zielone.

### Backend MVP

- [ ] Sesję i wpisy można tworzyć oraz edytować.
- [ ] Każda mutacja atomowo aktualizuje podsumowanie.
- [ ] Historia jest odporna na zmianę/archiwizację katalogu.
- [ ] Jawna reewaluacja ma pełny audyt.
- [ ] Calendar, Dashboard i Areas korzystają z dedykowanych read modeli.
- [ ] Dane użytkowników są izolowane przez auth.
- [ ] Mobile zapisuje lokalnie i synchronizuje bez duplikatów.
- [ ] Plan wykonania zapytań i metryki potwierdzają, że nie potrzebujemy przedwczesnego cache lub osobnej bazy odczytowej.

## 17. Decyzje zapisane i pytania otwarte

### Decyzje architektoniczne

- modularny monolit zamiast mikroserwisów,
- jeden moduł Maven i jeden artefakt JAR,
- package-by-feature i kontrola granic Spring Modulith,
- lekki CQRS bez MediatR clone, Axona i event sourcingu,
- wspólny PostgreSQL dla command/query,
- JPA dla zapisu, projekcje/SQL dla read modeli,
- Flyway jako jedyne źródło DDL,
- PostgreSQL także w testach,
- atomowy zapis ukończonej sesji z wpisami,
- Java `BigDecimal` i wersjonowanie obliczeń,
- snapshoty historyczne od pierwszego wpisu,
- client UUID i idempotencja od pierwszej migracji,
- auth później, ale `userId` i własność danych od początku,
- uproszczony katalog `Area → optional Sector → Climb` jako rekomendacja MVP.

### Pytania wymagające decyzji przed Etapem 3

1. Pełna tabela punktów i indeksów wycen.
2. Finalne profile wysiłku i wartości `baseEdl`.
3. Finalne style oraz mnożniki.
4. Bandy relative effort poza zwykłym zakresem.
5. Główna metryka prezentowana użytkownikowi.
6. Polityka średniej intensywności ruchu.
7. Dokładna polityka skali i zaokrągleń.
8. Zachowanie produkcyjne bez aktywnego `UserGradeReference` — rekomendacja: onboarding blokujący obliczenie, a nie ukryty default.

### Pytania, które mogą poczekać

- provider OIDC,
- finalny algorytm streak,
- reguły achievements i goals,
- szczegóły Cycle View,
- strategia konfliktów pełnego offline sync,
- materialized views/jOOQ,
- hierarchiczny katalog światowych rejonów.

## 18. Źródła i pierwszeństwo dokumentów

Plan powstał po analizie:

- `frontend-web/src/pages` oraz typów i mocków Reacta,
- `frontend-mobile/lib/features` oraz modeli Fluttera,
- [wymagań API i bazy](climbbetter_wymagania_api_baza.md),
- [ustaleń produktowych](project_goals_02.md),
- [wcześniejszego planu API/DB](api_db_plan_iteracji.md),
- [checklisty API v2](Api_v2_kanban.md),
- historycznego kodu i planu w `backend-net` wyłącznie jako referencji.

W razie konfliktu:

1. zatwierdzona reguła produktu i model obliczeń,
2. ten plan wykonawczy Java,
3. bieżący kontrakt OpenAPI i testy akceptacyjne,
4. starsze plany oraz prototyp .NET.

Po każdej decyzji zmieniającej domenę aktualizujemy dokument wymagań, ten plan oraz golden tests w tym samym zadaniu. OpenAPI opisuje kontrakt transportowy, ale nie zastępuje opisu reguł biznesowych.

### Dokumentacja technologii

- [Spring Boot — project](https://spring.io/projects/spring-boot/)
- [Spring Boot — system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Oracle Java SE support roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [Apache Maven release history](https://maven.apache.org/docs/history.html)
- [Spring Boot — SQL/JPA](https://docs.spring.io/spring-boot/reference/data/sql.html)
- [Spring Boot — Flyway/database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html)
- [Spring Boot — Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Spring Framework — RFC 9457 ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Spring Modulith — fundamentals](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
- [Spring Modulith — module verification](https://docs.spring.io/spring-modulith/reference/verification.html)
- [Spring Modulith — events](https://docs.spring.io/spring-modulith/reference/events.html)
- [Spring Data JPA — projections](https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html)
- [PostgreSQL — current documentation](https://www.postgresql.org/docs/current/)
- [Flyway — PostgreSQL support](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database)
- [springdoc-openapi releases](https://github.com/springdoc/springdoc-openapi/releases)
