# Notatki architektoniczne do pracy inżynierskiej

Status: żywy rejestr decyzji i dowodów z implementacji; aktualizacja 2026-10-11.

Ten dokument zbiera materiał, który może później posłużyć do opisania projektu w pracy inżynierskiej. Nie jest gotowym rozdziałem pracy. Każde twierdzenie o implementacji powinno mieć oparcie w kodzie, migracji, teście albo zapisanej decyzji. Planowane rozwiązania są oznaczone osobno.

## Kontekst i główne założenia

ClimbBetter ma rejestrować trening wspinaczkowy przy małej liczbie pytań zadawanych użytkownikowi, a następnie wyliczać porównywalne metryki obciążenia i intensywności. Najtrudniejszą częścią nie jest samo CRUD, lecz zachowanie znaczenia danych historycznych mimo zmian słowników, algorytmów i preferowanych skal wspinaczkowych.

Przyjęto modularny monolit w Spring Boot. Pozwala on rozdzielić odpowiedzialności domenowe bez kosztu wdrażania i utrzymywania kilku usług. Granice modułów kontroluje Spring Modulith, a aplikacja i baza nadal stanowią jeden wdrażany system.

## Decyzje warte opisania

| Temat | Problem | Zastosowane rozwiązanie i uzasadnienie | Koszt lub ograniczenie | Dowód / stan |
| --- | --- | --- | --- | --- |
| Modularny monolit | System obejmuje tożsamość, katalog wspinaczek, trening i raportowanie, ale na obecnym etapie mikroserwisy zwiększyłyby złożoność operacyjną. | Pakiety `identity`, `catalog`, `training`, `reporting` i `shared` są modułami Spring Modulith. Publiczne API modułu ma być małe, a implementacja ukryta w `internal`. | Granice są logiczne, lecz moduły współdzielą proces i bazę. Bez dyscypliny łatwo utworzyć zależności omijające publiczne API. | `package-info.java` modułów i test `ApplicationModules.verify()`; zaimplementowane. |
| Fasada domenowych obliczeń | Zapis sesji potrzebuje kilku współpracujących kalkulatorów, których szczegóły nie powinny stać się publicznym API. | `TrainingMetricsFacade` udostępnia jedno wejście do obliczania wpisu ocenianego, rozgrzewki i podsumowania sesji. Wewnętrzne kalkulatory oraz ich pomocnicze typy pozostają schowane. | Fasada może z czasem urosnąć. Trzeba pilnować, aby koordynowała reguły, a nie przejęła wszystkich odpowiedzialności serwisu zapisu. | [TrainingMetricsFacade](../../backend/src/main/java/pl/climbbetter/training/internal/model/TrainingMetricsFacade.java) jest używana przez transakcyjny serwis zapisu; potwierdzone testem pionowym. |
| Lekki CQRS | Dane potrzebne na Dashboardzie mają inny kształt niż dane przyjmowane przy zapisie sesji. | Odczyt ma osobny `DashboardQuery`, projekcję `DashboardSessionSummary` i zapytanie przez `JdbcClient`. Model zapisu jest budowany oddzielnie wokół komend i serwisu transakcyjnego. | To rozdzielenie zwiększa liczbę typów. Nie wprowadzamy osobnej bazy odczytowej ani infrastruktury komunikatów. | Strona odczytu działa; `POST` i pełna strona zapisu są jeszcze w budowie. |
| Typy `Command` zamiast ogólnego DTO | Jeden obiekt przesyłany między warstwami mógłby mieszać żądanie HTTP, przypadek użycia i odpowiedź. | Nazwa `Command` mówi, że obiekt opisuje intencję zmiany stanu. Odpowiedzi i read modele mają osobne nazwy wynikające z ich roli. | Powstaje więcej wyspecjalizowanych typów, lecz ich przeznaczenie jest czytelne. | `CreateTrainingSessionCommand`, `TrainingEntryCommand`, `DashboardSessionSummary`; częściowo zaimplementowane. |
| Trwałe snapshoty wyników | Ponowne przeliczenie dawnych sesji według zmienionych tabel mogłoby zmienić historię użytkownika. | Przy wpisie przechowywane są użyte dane wejściowe, wersje reguł i obliczone metryki. Dashboard może czytać historyczny wynik bez odtwarzania dawnego stanu słowników. | Snapshot duplikuje część danych i wymaga świadomego wersjonowania reguł. | Migracje `V5`, `V9` i `V10`; zapis przez API jeszcze nie istnieje. |
| Kanoniczne skale i aliasy | Użytkownicy chcą wpisywać wyceny w różnych skalach, ale każda nowa skala nie powinna tworzyć nowego algorytmu punktowego. | Obliczenia używają kanonicznych `ROUTE_FRENCH` i `BOULDER_FONT`. Skale takie jak V-scale lub Kurtyki mają być wersjonowanym mapowaniem wejścia i prezentacji na kanoniczną wycenę. | Mapowania są przybliżone i muszą mieć wersję; nie można przedstawiać ich jako obiektywnej równoważności. | DEC-030 i migracja `V6`; aliasy są decyzją, ich tabela nie jest jeszcze wdrożona. |
| Resolver jako osobna odpowiedzialność | Komenda użytkownika zawiera etykiety i deklaracje, a kalkulator potrzebuje jednoznacznych danych katalogowych, historii i mnożników. | Resolvery tłumaczą wejście na istniejący Area/Climb/Grade, profil wysiłku, historię, poziom i reguły obliczeń. Kalkulatory pozostają niezależne od SQL. | Duża liczba małych klas może utrudnić nawigację. Uzasadnia je tylko jedna, jasno nazwana odpowiedzialność i użycie w przepływie zapisu. | Resolvery korzystające z `JdbcClient` istnieją; ich wspólne spięcie w `POST` jest następne. |
| Flyway jako właściciel schematu | Automatyczne generowanie tabel z encji utrudnia kontrolę i odtwarzanie zmian bazy. | Każda zmiana schematu ma wersjonowaną migrację, a Hibernate używa `ddl-auto: validate`. | Migracje trzeba pisać i utrzymywać ręcznie. W zamian środowiska są odtwarzalne i zmiany podlegają przeglądowi. | Migracje `V1`–`V11`; potwierdzone na Testcontainers i lokalnym PostgreSQL. |
| `JdbcClient` bez obowiązkowych encji JPA | Read model i proste operacje słownikowe nie wymagają pełnego cyklu życia encji. | Jawny SQL mapuje dane bezpośrednio na projekcje lub rekordy potrzebne przypadkowi użycia. JPA pozostaje dostępne, gdy pojawi się problem wymagający agregatu i śledzenia encji. | SQL wiąże implementację z relacyjnym modelem, a mapowania trzeba utrzymywać ręcznie. | Dashboard i resolvery używają `JdbcClient`; zaimplementowane. |
| Wstrzykiwany `Clock` | Kod zależny od bieżącej daty jest trudny do deterministycznego testowania i łatwo miesza strefy czasowe. | Spring dostarcza `Clock.systemUTC()`, a dzień treningowy jest reprezentowany przez `LocalDate`. Test może podmienić zegar bez zmiany logiki. | Granicę dnia w strefie użytkownika trzeba będzie jawnie określić, gdy produkt jej rzeczywiście potrzebuje. | `TimeConfiguration` i test strefy UTC; zaimplementowane. |
| PostgreSQL w Testcontainers | Test na bazie innej niż produkcyjna mógłby przepuścić różnice składni SQL, typów i ograniczeń. | Testy integracyjne uruchamiają PostgreSQL `17.6-alpine`, zgodny z obrazem lokalnego Compose. Każdy przebieg może zacząć od pustej bazy i zastosować Flyway. | Testy wymagają Docker Desktop i są wolniejsze od testów jednostkowych. | Konfiguracja Testcontainers, `infra/compose.yml` i potwierdzone przebiegi integracyjne. |
| Idempotencja przez identyfikatory klienta | Ponowienie żądania po utracie odpowiedzi nie może utworzyć drugiej sesji lub wspinaczki. | Klient nadaje stabilne UUID, a baza przechowuje mapowanie w obrębie użytkownika. Docelowy serwis zapisu ma traktować ponowienie jako tę samą operację. | Wymaga ograniczeń unikalności i jednoznacznej polityki odpowiedzi na zmienione ponowienie. | Fundament istnieje w schemacie; zachowanie endpointu nadal do implementacji i testu. |

## Granice i uczciwe zastrzeżenia

Nie należy nazywać obecnego rozwiązania pełną Clean Architecture. Kod korzysta z części jej zasad: domena obliczeń nie zna Spring MVC ani SQL, przypadki odczytu mają własne API, a szczegóły są ukrywane w `internal`. Jednocześnie resolvery w module `training` wykonują bezpośrednie zapytania do tabel katalogu. To pragmatyczny wybór dla jednego procesu i jednej bazy, ale osłabia ścisłą własność danych między modułami.

Przepływ backendowy `POST → PostgreSQL → GET Dashboard` jest potwierdzony testem integracyjnym. Nie istnieje jeszcze integracja webowego formularza z backendem. W pracy należy oddzielić ten działający pion od pełnego scenariusza użytkownika obejmującego lokalny szkic, obsługę błędów i ponowienie zapisu.

Minimalny `ClimberLevelResolver` jest obecnie przybliżeniem. Docelowa metoda Pyramid Support oraz kalibracja punktów powinny być opisane jako rozwój algorytmu, a nie jako obecnie zakończona funkcja.

## Materiał do późniejszych rozdziałów

W opisie analizy i projektu warto pokazać:

1. problem domenowy: sesja, wpis, próba, przejście, rozgrzewka, historia kontaktów i różne skale wycen;
2. diagram komponentów modularnego monolitu oraz uzasadnienie granic modułów;
3. sekwencję zapisu sesji: walidacja użytkownika, rozwiązanie katalogu i historii, obliczenia, zapis snapshotu i odczyt Dashboardu;
4. model relacyjny oraz sposób wersjonowania słowników i reguł;
5. przykład obliczenia metryk dla jednego wpisu i agregacji sesji;
6. strategię testów: czyste testy reguł, integracja z prawdziwym PostgreSQL i test pionowy `POST → GET`;
7. ocenę rozwiązania: prostota wdrożenia, odtwarzalność, testowalność, koszty rozdrobnienia typów i miejsca dalszego rozwoju.

Diagramy robocze znajdują się w [katalogu diagrams](diagrams/). Decyzje wraz z uzasadnieniami zapisuje [rejestr decyzji](decisions.md), a potwierdzony stan implementacji opisują [backend](backend.md), [baza](database.md) i [postęp](../project-management/progress.md).

## Zasada aktualizacji

Po większej decyzji architektonicznej dopisujemy problem, wybrane rozwiązanie, odrzucone rozsądne alternatywy, konsekwencje oraz dowód w kodzie lub teście. Po implementacji zmieniamy status z planu na stan potwierdzony. Dzięki temu późniejsza praca będzie oparta na rzeczywistym procesie projektowym, a nie na opisie dopisanym po zakończeniu aplikacji.
