# ClimbBetter — backend Java, plan i kanban

Status: `IN PROGRESS`, realizacja etapu 1  
Data przeglądu: 2026-08-15  
Katalog docelowy: `backend/`  
Referencja historyczna: `backend-net/`

Aktualizacja priorytetu 2026-09-22: pierwszy użyteczny przepływ produktu to rejestrowanie i analiza treningu w webie. Robocza sesja pozostaje lokalnie we frontendzie; `Start` nie zapisuje jej w bazie. Backend przyjmuje ukończoną sesję ze wszystkimi wpisami i uwzględnia ich kolejność przy wyznaczaniu historii prób. [Zakres etapu web](CB_web_stage_1.md) opisuje także propozycję podglądu obliczeń bez zapisu. Opisany niżej przepływ rozpoczynający się od Fluttera pozostaje celem integracji, ale nie wyznacza już kolejności pierwszego etapu.

## 1. Cel

Budujemy backend Java/Spring od początku na podstawie prawdziwego modelu ClimbBetter, a nie poprzez mechaniczne przepisanie prototypu .NET. Backend ma stać się jedynym źródłem prawdy dla danych treningowych, obliczeń, snapshotów i agregatów używanych przez React oraz Flutter.

Pierwszy dowód działania:

```text
Flutter zapisuje ukończoną sesję z wpisami
                    ↓
Java waliduje i oblicza model
                    ↓
PostgreSQL utrwala sesję, wpisy, snapshoty i summary
                    ↓
React oraz Flutter odczytują tę samą sesję
```

Nie rozpoczynamy od pełnego CRUD-u wszystkich tabel. Najpierw powstaje jeden kompletny pionowy przepływ.

## 2. Stan bieżący

- [x] Stary backend .NET został zachowany w `backend-net/` jako referencja.
- [x] `backend/` jest pustym miejscem na aplikację Java.
- [x] Istnieje propozycja modularnego monolitu i lekkiego CQRS.
- [x] Istnieje bieżący model wyceny oparty na realnym arkuszu.
- [x] Zidentyfikowano potrzeby ekranów React i formularza Flutter.
- [ ] Nie ma jeszcze projektu Maven/Spring Boot.
- [ ] Nie ma schematu Flyway dla Javy.
- [ ] Nie ma działającego PostgreSQL spiętego z nowym backendem.
- [ ] Nie ma kalkulatora ani testów golden cases w Javie.
- [ ] Nie ma kontraktu OpenAPI nowego backendu.
- [ ] Żaden frontend nie korzysta jeszcze z prawdziwego API.

## 3. Docelowe podejście architektoniczne

### 3.1. Stos startowy

- Java 21 LTS 
- Spring Boot 4.x w kompatybilnej stabilnej wersji;
- Maven 3.9.x i Maven Wrapper;
- Spring MVC;
- Jakarta Bean Validation;
- Spring Data JPA + Hibernate dla zapisu;
- PostgreSQL;
- Flyway i jawne migracje SQL;
- springdoc/OpenAPI;
- Spring Boot Actuator;
- Spring Modulith do weryfikacji granic;
- JUnit, AssertJ, Mockito, Spring Test i Testcontainers PostgreSQL.

Nie dodajemy na starcie WebFlux, R2DBC, Axona, event sourcingu, brokera, Redis, H2 ani osobnej bazy odczytowej.

### 3.2. Modularny monolit

Jeden projekt Maven, jeden proces, jeden JAR i jeden PostgreSQL. Podział według funkcji biznesowych:

```text
pl.climbbetter
├── identity
├── catalog
├── training
├── reporting
└── shared
```

Każdy moduł ma publiczne API oraz ukryte `internal`:

```text
training/
├── TrainingOperations.java
├── TrainingQueries.java
└── internal/
    ├── web/
    ├── application/
    │   ├── command/
    │   ├── query/
    │   └── port/
    ├── domain/
    │   └── calculation/
    └── persistence/
```

### 3.3. Clean Architecture w praktyce Spring

Mapowanie znanego układu .NET:

| .NET | Java/Spring |
| --- | --- |
| `Api` | `internal.web` + klasa startowa i konfiguracja |
| `Application` | `internal.application.command/query/port` |
| `Domain` | `internal.domain` |
| `Infrastructure` | `internal.persistence` i adaptery techniczne |

Na starcie nie tworzymy czterech osobnych modułów Maven. Te granice obowiązują wewnątrz modułów biznesowych i są sprawdzane przez Spring Modulith.

### 3.4. Lekki CQRS

- osobne commandy i query;
- konkretny handler dla przypadku użycia;
- command handler otwiera transakcję zapisu i pracuje na agregacie;
- query handler zwraca dedykowaną projekcję read-only;
- kontroler wstrzykuje konkretny handler albo małą fasadę modułu;
- brak klona `IMediator`, generycznego command busa i osobnej bazy read.

## 4. Zasady realizacji backlogu

- Zadanie zmienia `[ ]` na `[x]` dopiero po teście i weryfikacji Definition of Done.
- Każda migracja jest forward-only i ma test uruchomienia na pustym PostgreSQL.
- Każdy endpoint ma DTO, przykład OpenAPI i test integracyjny.
- Obliczenia domenowe są testowane bez kontekstu Springa.
- Encje JPA nigdy nie są odpowiedzią HTTP.
- Backend nie ufa wyliczeniom wysłanym przez klienta.
- Mock frontendu znika dopiero po działającym endpointcie i obsłużeniu stanów UI.

## 5. Etap 0 — zamknięcie modelu v1

Blokuje implementację kalkulatora i finalną pierwszą migrację.

- [x] Zamknąć `DEC-006` i `DEC-008` w [CB_project_decisions.md](CB_project_decisions.md).
- [x] Zatwierdzić oddzielne skale `BOULDER_FONT` i `ROUTE_FRENCH`, zakres `1–9c/9C` oraz wspólne punkty/indeksy.
- [x] Zatwierdzić profile EDL i `baseEdl` z bieżącego modelu.
- [x] Rozdzielić styl na `resultType`, `ascentMode`, `familiarityBand` i wynikową `StyleRule` bez `isCompleted`.
- [x] Przyjąć ostrzeżenie, jawne potwierdzenie i audyt override dla OS/Flash sprzecznego z historią.
- [x] Zatwierdzić floor/cap relative effort poza zakresem `-6..5`.
- [x] Zatwierdzić Pyramid Support, próg małej próbki oraz confidence v1; mapowanie `BOULDER` oraz `ROUTE/CIRCUIT` jest zatwierdzone.
- [x] Ogólny poziom dyscypliny liczyć ze wszystkich ukończonych wspinaczek poza rozgrzewkami, niezależnie od OS/Flash/Fast RP/RP, a profil stylów wyprowadzać osobno.
- [x] Zatwierdzić `edlCount`, `moveIntensity`, `classicLoad` i `adjustedLoad` jako nazwy kanoniczne.
- [x] Ustalić skalę i rounding `BigDecimal`.
- [x] Zatwierdzić średnią intensywności ważoną wykonanymi ruchami.
- [x] Zatwierdzić quick location jako prywatne Area `DRAFT`, ukryte domyślnie w „My Areas”.
- [x] Zatwierdzić automatyczne tworzenie prywatnego Area/Climb z client ID dla szybkiego logowania.
- [ ] Przygotować 5–10 ręcznie policzonych golden cases z realnych sesji.
- [ ] Dodać przypadki błędne: brak poziomu, skrajny diff, warmup, niespójny wynik.

Rezultat: [CB_model.md](CB_model.md) i [CB_climbing_effort_valuation.md](CB_climbing_effort_valuation.md) są jednoznaczną specyfikacją v1.

## 6. Etap 1 — bootstrap aplikacji

- [x] Sprawdzić lokalne wersje JDK, Docker Desktop i wsparcie IDE.
- [x] Wygenerować pojedynczy projekt Spring Boot w `backend/`.
- [x] Dodać `pom.xml`, `mvnw`, `mvnw.cmd` i `.mvn/wrapper`.
- [x] Ustawić `release/toolchain` wybranej Javy.
- [x] Dodać minimalne zależności startowe.
- [ ] Utworzyć pakiet `pl.climbbetter` i moduły funkcjonalne.
- [ ] Dodać `application.yml`, `application-local.yml` i `application-test.yml`.
- [ ] Konfigurację sekretów oprzeć na zmiennych środowiskowych.
- [ ] Wyłączyć Open Session/EntityManager in View.
- [ ] Ustawić `ddl-auto=validate`.
- [ ] Dodać wspólny `Clock` pracujący w UTC.
- [ ] Dodać Actuator i health/readiness.
- [ ] Dodać springdoc oraz Swagger UI.
- [ ] Ustawić jednolite logowanie i correlation/request ID.
- [ ] Dodać pierwszy smoke test kontekstu.
- [ ] Dodać test `ApplicationModules.verify()`.
- [x] Uruchomić `mvnw.cmd verify` na Windows.
- [ ] Napisać `backend/README.md` z komendami developerskimi.

Rezultat: aplikacja startuje bez logiki biznesowej, ma zielony build, health check i OpenAPI.

## 7. Etap 2 — PostgreSQL od pierwszych dni

- [ ] Dodać `infra/compose.yaml` z PostgreSQL i healthcheckiem.
- [ ] Dodać `.env.example` bez sekretów.
- [ ] Ustalić nazwę bazy, użytkownika developerskiego i port lokalny.
- [ ] Połączyć profil `local` ze zmiennymi środowiskowymi.
- [ ] Dodać Flyway i migracje tworzące schematy `identity`, `catalog`, `training`.
- [ ] Dodać minimalne tabele zgodne z [CB_model.md](CB_model.md).
- [ ] Dodać FK, unique, check constraints i indeksy.
- [ ] Dodać kolumny optimistic locking.
- [ ] Dodać deterministycznego dev-usera.
- [ ] Dodać zatwierdzone słowniki z deterministycznymi UUID.
- [ ] Dodać poziomy dev-usera.
- [ ] Dodać źródło, wersję algorytmu, `sampleSize`, `confidence`, support score, liczności warstw i okno historii do poziomu użytkownika.
- [ ] Dodać pełny seed wycen `1–9c/9C` dla obu skal.
- [ ] Dodać seed macierzy `StyleRule` dla rezultatu, OS/Flash/RP i familiarity bandów.
- [ ] Dodać fixtures: Bronx, Garaż, pełne Area z Jury, draft `Zimny Dół` i kilka wspinaczek.
- [ ] Dodać `clientAreaId` i status rozwiązania draftu Area.
- [ ] Przygotować pola źródła/zewnętrznego ID pod przyszłe oficjalne pakiety Area.
- [ ] Dodać Testcontainers PostgreSQL z `@ServiceConnection`.
- [ ] Dodać test: Flyway buduje pustą bazę.
- [ ] Dodać test: Hibernate poprawnie waliduje schemat.
- [ ] Dodać testy constraints i najważniejszych indeksów/unikalności.
- [ ] Potwierdzić start backendu na lokalnym compose.

Rezultat: istnieje jedna odtwarzalna baza dla local i testów; H2 nie istnieje w projekcie.

## 8. Etap 3 — czysty model domenowy i kalkulator

- [ ] Zaimplementować typowane identyfikatory i value objects.
- [ ] Zaimplementować `TrainingSession` jako aggregate root.
- [ ] Zaimplementować `TrainingEntry` i `CalculationSnapshot`.
- [ ] Zaimplementować niezmienniki ruchów, kolejności, czasu i wyniku.
- [ ] Zdefiniować `CalculationPrecision`.
- [ ] Zdefiniować `calculationModelVersion = 1`.
- [ ] Zaimplementować wybór poziomu użytkownika dla daty i dyscypliny.
- [ ] Zaimplementować wyznaczanie brakującego poziomu po zakończeniu sesji.
- [ ] Zaimplementować wersjonowany estimator poziomu odporny na pojedynczy odstający wynik.
- [ ] Zaimplementować `PyramidLevelEstimator` z wagami `1.00/0.50/0.25`, capami `2/4/8` i progiem `3.00`.
- [ ] Zaimplementować confidence `UNASSESSED/LOW/MEDIUM/HIGH` z wersjonowanymi progami oraz zachować dowody potrzebne do wyjaśnienia wyniku.
- [ ] Dodać testy typowej piramidy oraz przypadku `50 × 6a + 1 × 6c`.
- [ ] Dodać golden case sezonu: `1 × 7a`, `3 × 6c`, `2 × 6b+`, `5 × 6b` → ogólny poziom `6c+` i osobny profil stylów.
- [ ] Po każdej zakończonej sesji aktualizować estymację poziomu dla przyszłych obliczeń zgodnie z wersją algorytmu.
- [ ] Zaimplementować wybór dokładnie jednego relative effort bandu.
- [ ] Dodać floor `0.20`, cap `5.00` i `outsideCalibratedRange`.
- [ ] Zaimplementować klasyfikację znajomości stylu z historii dwóch lat.
- [ ] Zaimplementować rozdzielone `resultType`, `ascentMode`, `familiarityBand` i resolver `StyleRule`.
- [ ] Liczyć wszystkie wcześniejsze próby i przejścia; uwzględniać wcześniejsze wpisy bieżącej sesji.
- [ ] Zaimplementować progi znajomości `0`, `1–10`, `11–20`, `>20`.
- [ ] Wyliczać osobno `contactCountBeforeTwoYears` dla znajomości i `attemptOrdinalAllTime` dla osiągnięcia Fast RP.
- [ ] Ustawiać `fastRp` dla ukończonego RP w drugiej/trzeciej próbie całej znanej historii, bez dwuletniego okna.
- [ ] Zachować OS i Flash osobno w historii/osiągnięciach przy wspólnym mnożniku loadu.
- [ ] Wykrywać OS/Flash sprzeczne z historią i zwracać domenowy kod konfliktu wymagający jawnego potwierdzenia.
- [ ] Po potwierdzeniu zapisywać deklarację, wcześniejszą liczbę kontaktów i audyt override.
- [ ] Zaimplementować `edlCount` i `moveIntensity`.
- [ ] Zaimplementować `classicLoad` i `adjustedLoad`.
- [ ] Zaimplementować szczególną semantykę warmup.
- [ ] Zaimplementować przeliczenie `SessionSummary`.
- [ ] Liczyć `averageMoveIntensity` jako średnią ważoną `executedMoves`.
- [ ] Zaimplementować tworzenie kompletnego snapshotu.
- [ ] Dodać testy jednostkowe każdego wzoru i niezmiennika.
- [ ] Dodać zatwierdzone golden cases bez wyliczania expected tym samym kodem.
- [ ] Porównać wyniki Javy z arkuszem na rzeczywistych sesjach.

Rezultat: cały model obliczeń można uruchomić testem bez Springa i bez bazy.

## 9. Etap 4 — persystencja i publiczne słowniki

- [ ] Zaimplementować mapowanie domena ↔ JPA dla sesji i wpisów.
- [ ] Zaimplementować adaptery repozytoriów.
- [ ] Dodać repozytoria odczytu Area, Climb i słowników.
- [ ] Dodać port `CurrentUserProvider`.
- [ ] Dodać implementację dev-user wyłącznie dla profili `local` i `test`.
- [ ] Dodać odczyt Areas dostępnych użytkownikowi.
- [ ] Dodać obsługę minimalnego prywatnego draftu Area.
- [ ] Dodać późniejsze uzupełnienie lub merge draftu z Area użytkownika/oficjalnym bez utraty historii.
- [ ] Dodać wyszukiwanie/sugestie wspinaczek w Area.
- [ ] Dodać odczyt Grades, reguł stylu i EffortProfiles.
- [ ] Dodać testy nieaktywnego/archiwalnego słownika i katalogu.
- [ ] Dodać test izolacji prywatnego Area/Climb pomiędzy użytkownikami.

Rezultat: formularz może pobrać wszystkie dane wyboru z PostgreSQL.

## 10. Etap 5 — pierwszy pionowy zapis i odczyt

Rekomendowany kontrakt: atomowe utworzenie ukończonej sesji wraz z listą wpisów.

- [ ] Zdefiniować request/response records bez encji JPA.
- [ ] Zaimplementować command utworzenia sesji z wpisami.
- [ ] Zaimplementować jeden transakcyjny handler.
- [ ] Gdy brakuje poziomu, najpierw zwalidować wszystkie wpisy, wyznaczyć tymczasowy poziom z całej sesji, a dopiero potem policzyć jej snapshoty.
- [ ] Rozwiązać znane `climbId`.
- [ ] Rozwiązać `clientClimbId`.
- [ ] Obsłużyć quick location: `clientAreaId` + nazwa tworzą prywatny draft Area.
- [ ] Obsłużyć wpis roboczy: `clientClimbId` tworzy prywatny, niezweryfikowany Climb pod wybranym/draftowym Area.
- [ ] Generować nazwę sesji, gdy użytkownik jej nie poda, i zapisywać `sessionNameSource`.
- [ ] Policzyć i zapisać snapshot każdego wpisu.
- [ ] Zapisać snapshot nazwy lokalizacji, źródła poziomu i flagi wyjścia poza relative effort.
- [ ] Przetwarzać wpisy w `entryOrder`, aby wcześniejsza próba sesji wpływała na znajomość następnej.
- [ ] Zapisać `contactCountBeforeTwoYears`, `attemptOrdinalAllTime`, `familiarityBand`, `ascentMode` i `fastRp` w snapshocie.
- [ ] Policzyć i zapisać summary sesji.
- [ ] Dodać idempotencję `clientSessionId`.
- [ ] Zapisać fingerprint payloadu dla wykrycia innego retry pod tym samym ID.
- [ ] Identyczny retry zwracać bez duplikatu.
- [ ] Inny payload pod tym samym client ID kończyć `409 Conflict`.
- [ ] Dodać Bean Validation dla kształtu requestu.
- [ ] Dodać domenową walidację spójności.
- [ ] Dodać do komendy jawne potwierdzenie konfliktu historii OS/Flash; bez niego zwracać `409` z kodem `ASCENT_MODE_HISTORY_CONFLICT`.
- [ ] Dodać `@RestControllerAdvice` i RFC 9457 `ProblemDetail`.
- [ ] Dodać `GET` szczegółów sesji jako read DTO.
- [ ] Dodać stronicowaną listę sesji z filtrem dat.
- [ ] Dodać przykłady OpenAPI.
- [ ] Wyeksportować specyfikację OpenAPI do repozytorium.

Testy integracyjne:

- [ ] migracja i seed na pustej bazie;
- [ ] prawidłowy atomowy zapis sesji;
- [ ] rollback całej sesji przy jednym błędnym wpisie;
- [ ] zgodność DB, snapshotu i summary z golden case;
- [ ] idempotentny retry;
- [ ] konflikt innego payloadu;
- [ ] niezmienność odczytu po zmianie katalogu;
- [ ] zachowanie lokalnej `sessionDate` niezależnie od strefy klienta;
- [ ] brak odczytu cudzej sesji.

Rezultat: curl/Swagger zapisuje prawdziwy trening do PostgreSQL i zwraca wszystkie wyniki.

## 11. Etap 6 — integracja pierwszych klientów

- [ ] Udostępnić stabilne CORS dla lokalnego Reacta.
- [ ] Ustalić adresowanie emulatora Android (`10.0.2.2`) i urządzenia fizycznego.
- [ ] Ustalić wersjonowanie `/api/v1`.
- [ ] Podłączyć Flutter do słowników i zapisu sesji.
- [ ] Podłączyć Flutter Board do listy sesji.
- [ ] Podłączyć React Session Details.
- [ ] Podłączyć React feed/listę sesji.
- [ ] Wykonać demo Flutter → Java/PostgreSQL → React/Flutter.
- [ ] Dodać test kontraktu chroniący oba klienty przed przypadkową zmianą DTO.

Rezultat: główny treningowy mock znika z produkcyjnego przepływu obu klientów.

## 12. Etap 7 — edycja i bezpieczeństwo historii

- [ ] Dodać aktualizację metadanych sesji.
- [ ] Dodać add/update/delete wpisu.
- [ ] Po każdej mutacji atomowo odtwarzać snapshot i summary.
- [ ] Dodać optimistic locking i czytelny `409`.
- [ ] Dodać archiwizację Area, Sector i Climb.
- [ ] Dodać test, że archiwizacja nie zmienia historii.
- [ ] Zdefiniować pełny model `CalculationRevision`.
- [ ] Dodać jawną reewaluację wybranego zakresu z powodem.
- [ ] Zachować poprzedni i nowy snapshot dla każdej zmiany wpływającej na wynik.
- [ ] Dodać audyt użytkownika i czasu reewaluacji.

Rezultat: użytkownik poprawia błędy bez cichego niszczenia przeszłości.

## 13. Etap 8 — read modele React i analityka podstawowa

- [ ] Zaimplementować Calendar Month agregujący po `sessionDate`.
- [ ] Zaimplementować Calendar Day, później Week.
- [ ] Zaimplementować Dashboard Summary.
- [ ] Zaimplementować profil wspinacza z najwyższym OS, Fast RP i maksymalnym RP osobno dla balda i drogi.
- [ ] Zwracać historię/liczbę Flash oraz wyprowadzone `fastRp` bez tworzenia osobnego mnożnika loadu.
- [ ] Zwracać `maxRpVsOsGap`, `fastRpVsOsGap`, `maxRpVsFastRpGap`, attempt ordinal najlepszego RP oraz sample size/confidence potrzebne do interpretacji.
- [ ] Rozdzielić w read modelu peak grade, poziom poparty piramidą, confidence i przyszłą kompletność piramidy.
- [ ] Zaimplementować rozkład według wyprowadzonego charakteru sesji.
- [ ] Zaimplementować Area Summary z licznikami i statusami użytkownika.
- [ ] Dodać `UserAreaStatus` i `UserClimbStatus`.
- [ ] Zdefiniować i przetestować streak.
- [ ] Dodać filtry zakresu czasu, Area i rodzaju wspinaczki.
- [ ] Sprawdzić plany zapytań PostgreSQL i dodać indeksy na podstawie pomiarów.
- [ ] Dopiero przy realnej potrzebie użyć `JdbcClient`, native SQL albo jOOQ.

Rezultat: Dashboard, Calendar, Areas i Session Details nie korzystają z produkcyjnych mocków.

## 14. Etap 9 — wsparcie pełnego offline-first

- [ ] Zatwierdzić format client UUID dla sesji, wpisu i wspinaczki.
- [ ] Zaprojektować batch/sync contract albo idempotentne komendy pojedyncze.
- [ ] Zwracać mapowanie client UUID → server UUID.
- [ ] Dodać jawny stan synchronizacji i wersje rekordów.
- [ ] Zdefiniować tombstones dla usunięć offline.
- [ ] Zdefiniować kolejność operacji i retry z backoff.
- [ ] Zdefiniować politykę konfliktu dwóch urządzeń.
- [ ] Dodać testy powtórnej dostawy, zmiany kolejności i częściowej awarii.
- [ ] Dodać obserwowalność kolejki synchronizacji.

Rezultat: mobile może bezpiecznie zapisywać bez sieci i synchronizować bez duplikatów.

## 15. Etap 10 — auth i bezpieczeństwo

- [ ] Wybrać dostawcę OIDC.
- [ ] Dodać Spring Security jako OAuth2 Resource Server.
- [ ] Mapować subject tokenu na stabilny `User.id`.
- [ ] Zastąpić local `CurrentUserProvider` implementacją z claims.
- [ ] Zagwarantować, że local provider nie uruchomi się na produkcji.
- [ ] Dodać testy autoryzacji każdego zasobu użytkownika.
- [ ] Dodać bezpieczną politykę CORS i nagłówków.
- [ ] Dodać ograniczenia rozmiaru requestu oraz podstawowy rate limiting na brzegu, jeśli potrzebny.
- [ ] Zweryfikować logi pod kątem PII i sekretów.

## 16. Etap 11 — CI, jakość i utrzymanie

- [ ] Dodać workflow `mvnw verify` dla każdego pull requestu.
- [ ] Uruchamiać test Spring Modulith i Testcontainers w CI.
- [ ] Weryfikować brak różnicy wygenerowanego OpenAPI.
- [ ] Dodać formatter/static analysis bez nadmiernej konfiguracji.
- [ ] Dodać coverage jako informację, nie jako cel zastępujący testy jakościowe.
- [ ] Dodać logi strukturalne, health/readiness i podstawowe metryki.
- [ ] Dodać backup/restore runbook przed przechowywaniem wartościowych danych.
- [ ] Ustalić środowiska local, test, później staging/prod.
- [ ] Dodać obraz kontenera backendu.
- [ ] Dodać skan zależności i kontrolowane aktualizacje.
- [ ] Dokumentować każdą decyzję zmieniającą model i migracje.

## 17. Późniejsze funkcje

- [ ] osobny przyszły moduł planowania: sezon/makrocykl, mezo-/mikrocykle i jednostki, bez pól v1 w sesji;
- [ ] oficjalne, wersjonowane pakiety Area/Sector/Climb pobierane do katalogu i scalane z prywatnymi draftami;
- [ ] goals, achievements i challenges po ustaleniu reguł produktu;
- [ ] bardziej zaawansowane statystyki z sekcji Propozycje modelu wyceny;
- [ ] eventy Spring Modulith dla niezależnych reakcji po zakończeniu sesji;
- [ ] import danych historycznych;
- [ ] kontrolowane porównanie wersji kalkulatora;
- [ ] społeczność dopiero po ustabilizowaniu rdzenia treningowego.

## 18. Critical path do pierwszego demo

```text
1. Zamknięte decyzje modelu + golden cases
2. Spring Boot + Maven Wrapper + health/OpenAPI
3. PostgreSQL Compose + Flyway + Testcontainers
4. Czysty kalkulator i snapshoty
5. Seed: user + Area + słowniki + poziom
6. GET słowników i katalogu formularza
7. Atomowy POST ukończonej sesji
8. GET listy i szczegółów
9. Flutter zapisuje i odświeża Board
10. React pokazuje tę samą sesję
```

Calendar, cele, auth produkcyjny i pełny sync offline nie mogą opóźniać punktu 10.

## 19. Definition of Done pierwszego pionowego przepływu

- [ ] PostgreSQL startuje jedną udokumentowaną komendą.
- [ ] Pusta baza przechodzi wszystkie migracje Flyway.
- [ ] `/actuator/health` zwraca stan zdrowy.
- [ ] OpenAPI opisuje rzeczywisty kontrakt.
- [ ] Golden tests kalkulatora są zielone.
- [ ] Testcontainers sprawdza zapis, rollback, snapshot, summary i idempotencję.
- [ ] Flutter pobiera słowniki i zapisuje sesję.
- [ ] Flutter Board odczytuje zapisaną sesję.
- [ ] React odczytuje i pokazuje tę samą sesję.
- [ ] Punkty i load na obu klientach pochodzą z backendu.
- [ ] Zintegrowane ekrany nie importują produkcyjnych mocków.
- [ ] Buildy i testy trzech projektów są zielone.
