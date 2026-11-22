# API i przepływ danych

Aktualizacja auth 2026-11-08: API wymaga `Authorization: Bearer <Firebase ID token>` w trybie firebase/emulator. Nowe `POST /api/v1/identity/bootstrap` idempotentnie zakłada konto CB, `GET /api/v1/identity/me` zwraca `userId`, `displayName`, `profileComplete`, a `PUT /api/v1/identity/me` przyjmuje `displayName`. Pierwszy login Mihu używa odczytu i kontrolowanego powiązania UUID, bez automatycznego bootstrapu. Błędy:401 brak/zły token,409 `ACCOUNT_NOT_PROVISIONED`,403 `PROFILE_INCOMPLETE`,503 `AUTH_UNAVAILABLE`. [Pełny kontrakt, testy i uruchomienie](firebase-authentication-local.md). Poniższe wpisy zachowują własne daty.

Status: dodany raport Training summary miesiąc/rok; aktualizacja 2026-11-07. Pełny zestaw backendu 210/210 przeszedł. Stan UI i odbioru raportu: [Training summary](period-training-summary.md). Kontrakt zapisu v3 i wcześniejsze przyrosty pozostają poniżej.

## Training summary — 2026-11-07

`GET /api/v1/statistics/summary` przyjmuje `period=MONTH|YEAR` (domyślnie MONTH), wymagany `year` od 1 do 9998 i `discipline=ALL|BOULDER|ROUTE_AND_CIRCUIT` (domyślnie ALL). Dla MONTH wymagany jest `month` od 1 do 12; YEAR odrzuca parametr month. Nie przyjmuje userId. Przykłady: `?period=MONTH&year=2026&month=11&discipline=ALL` oraz `?period=YEAR&year=2026&discipline=BOULDER`. Nieprawidłowy lub brakujący parametr daje 400 ProblemDetail z kodem `INVALID_PERIOD_SUMMARY_REQUEST`.

Odpowiedź `PeriodTrainingSummary` zawiera `filters`, włączne `windowStart`/`windowEnd`, `summary`, `uniqueClimbCount`, `warmupMoves`, `buckets`, `disciplineBreakdown`, `styleBreakdown` i `highlights`. Metrics obejmują liczbę sesji i aktywnych dni, czas łączny/średni, ruchy oraz sumę/średnią/maksimum Classic i Adjusted Load. Miesiąc zwraca komplet dni, rok komplet 12 miesięcy; każdy bucket ma granice, metrics i lekką listę sesji z ID do otwarcia źródła. Puste koszyki mają sumy i liczniki 0, a średnie/maksima NULL. Średnie są liczone z odpowiednich sum sesji, nie ze średnich koszyków.

DEC-048: po filtrze dyscypliny czas pozostaje pełnym czasem pasujących sesji, natomiast ruchy i load obejmują tylko pasujące wpisy. All obejmuje ruchy rozgrzewki; rozgrzewka nie ma dyscypliny i ma zerowy load. Wędka i próby są treningiem w ruchach/loadzie. Style koła są rozłączne: po filtrze okresu i dyscypliny wybieramy najlepszy udany OS > FLASH > RP dla każdego climbId, bez TOP_ROPE i ATTEMPT. Rocznego koła nie tworzymy przez sumowanie miesięcznych unikalności. Highlights zawiera sesje o najwyższych loadach oraz maksima osiągnięć osobno dla obu skal. To odczyt zapisanych snapshotów, bez przeliczania wycen lub historycznego loadu. [Pełne zasady i plan](period-training-summary.md).

## Aktualny kontrakt modelu v3 — 2026-11-04

Nowy `POST /api/v1/training/sessions` i jawny `PUT /api/v1/training/sessions/{sessionId}` zapisują `calculationModelVersion = v3`, również dla rozgrzewki. Edycja v1/v2 przechodzi na v3 z audytem przed/po i przelicza wyłącznie edytowaną sesję. GET i identyczne retry istniejącego POST zachowują wcześniejsze snapshoty oraz wersje; nie uruchamiają ponownych obliczeń. Adresy HTTP `/api/v1` pozostają bez zmian. Details, Dashboard i Calendar mogą zwrócić `v1`, `v2`, `v3` lub `mixed`.

V3 zachowuje wycenę, punkty, intensywność profilu, crux i Classic Load z v2. Zmienia poziom odniesienia dla mnożnika względnego wysiłku, a przez niego Adjusted Load. Historia różnych wspinaczek ma domyślny horyzont 730 dni i wagę kwadratowo malejącą z wiekiem; przy mniej niż 10 kwalifikujących wspinaczkach pozostaje maksimum bez ważenia. TOP_ROPE nie buduje historii poziomu, lecz nadal otrzymuje load. Raportowy poziom piramidy zachowuje odrębne zasady. [Reguły i zakres](recency-load.md).

Oceniany wpis v3 dodaje do `entries[].calculation`:

- `effectiveCurrentLevelIndex` i `effectiveRelativeGradeDifference`: dziesiętne wartości rzeczywiście użyte do wyznaczenia mnożnika;
- `levelReferenceSource` i `levelEstimatorVersion`: źródło poziomu oraz wersję estymatora z ustawieniami;
- `levelReferenceHorizonDays`, `levelReferenceSampleSize` i `levelReferenceHistoryWeight`: horyzont, liczbę kwalifikujących różnych wspinaczek i wykorzystaną wagę historii.

Wszystkie siedem pól ma `null` dla v1/v2 i WARMUP. Dotychczasowe `currentLevelIndex` pozostaje całkowitą podłogą efektywnego indeksu, a `relativeGradeDifference` różnicą względem tej podłogi; do analizy v3 służą nowe pola dziesiętne. Klient nie podaje metadanych poziomu w komendzie i nie przelicza ich sam. Pozostają danymi technicznymi odpowiedzi, bez nowych pól formularza lub elementów UI.

Efektywny indeks ma 10 miejsc po przecinku. Interpolowany `relativeEffortMultiplier` jest zaokrąglany do 4 miejsc metodą HALF_UP przed liczeniem loadu i zapisem, aby zapisany snapshot pozwalał odtworzyć wynik. Poniższe sekcje v2 zachowują opis wcześniejszego przyrostu.

## Statistics i piramida — 2026-11-03

GET `/api/v1/statistics/pyramid` przyjmuje view=`BOULDER|ROUTE_AND_CIRCUIT|COMBINED`, period=`ALL_TIME|LAST_TWO_YEARS`, style=`RP|OS` i datę asOf. Zwraca warstwy, liczbę różnych wspinaczek, maksimum i raportowy poziom z metodą/confidence. GET `/api/v1/statistics/pyramid/climbs` przyjmuje te same filtry, bucketKey oraz offset/limit i zwraca pierwsze kwalifikujące przejścia warstwy. Wędka nigdy nie wchodzi do osiągnięć, a RP piramidy obejmuje OS/Flash/RP. Odczyty istniejących maksimów i Fast RP także stosują nową kwalifikację; trening i load pozostają zachowane. Dla COMBINED nie wyznaczamy wspólnego poziomu, różne kalibracje mają oddzielne grupy. [Pełny kontrakt i przykłady](achievement-pyramid-backend.md).

## Kontrakt modelu v2 przy wdrożeniu — 2026-11-01

`POST /api/v1/training/sessions` oraz `PUT /api/v1/training/sessions/{sessionId}` wyliczają zapisywane wpisy według `calculationModelVersion = v2`. Edycja sesji v1 przechodzi na v2 i zachowuje stan przed/po w audycie; nie przelicza pozostałych sesji. Dotychczasowe wpisy v1 pozostają czytelne przez GET, a identyczne ponowienie POST zwraca istniejącą sesję bez ponownego liczenia. Adresy `/api/v1` pozostają bez zmian — wersja obliczeń jest metadanymi wyniku, nie wersją HTTP API.

Details, Dashboard i lista kalendarza zwracają `calculationModelVersion`: `v1`, `v2` albo `mixed`; dla pustej listy wpisów przyjmują `v1`. Odczyt korzysta z zapisanych wyników. Pole nie oznacza przeliczenia ani sprowadzenia wyników różnych wersji do jednej skali.

Oceniany wpis v2 w `entries[].calculation` zawiera snapshot `profileIntensityIndex`, `gradePoints`, `gradeCalibrationVersion` i dotychczasowe wyniki oraz mnożniki. Pola `baseEdl` i `edlCount` są zachowane do odczytu v1; w nowych wpisach v2 mają `null`. Profile cruxowe dodatkowo zwracają `crux` typu `CruxDetails`: `hardMoveCount`, `easyMoveCount`, `hardMoveIntensity`, `easyMoveIntensity`, `baseMoveGradeLabel`, `hardMoveGradeLabel` i `easyMoveGradeLabel`. Dla pozostałych profili `crux = null`. Rozgrzewka nadal ma zerowe loady, bez indeksu profilu i bez danych cruxa.

`GET /api/v1/catalog/effort-profiles` zwraca `profileIntensityIndex` obok zachowanego `baseEdl`. Aktywne indeksy to 10 dla balda i krótkiej drogi baldowej, 7 dla średniej ciągowej, średniej cruxowej i obwodu oraz 5 dla długiej ciągowej i długiej cruxowej. `SHORT_CONTINUOUS_ROUTE` jest nieaktywny i nie jest oferowany do nowych wpisów. Frontend nie otrzymał zmian; nowe pola odpowiedzi są dodatkiem do dotychczasowego kontraktu.

V20 aktualizuje wyłącznie opisy profili dostępne w słowniku API; nie zmienia indeksów ani wyników. Wzory, podział cruxa i kalibracja Font mają jedno źródło w [modelu wyceny](../business/grading-and-scoring.md). Przed sprawdzeniem UI użytkownik musi uruchomić lub zrestartować backend z nowym kodem. Dalsza część dokumentu zachowuje wcześniejszy opis kontraktu; wzmianki o EDL dotyczą modelu v1.

Aktualny kontrakt CRUD i katalogu opisuje [backend-crud-handoff](backend-crud-handoff.md), a wdrożony przepływ React i jego weryfikację [frontend-web](frontend-web.md). Poniższe wcześniejsze zapisy o testach oczekujących zachowują kontekst historyczny i są zastąpione pełnym zielonym wynikiem.

Uzupełnienia dla React: `GET /api/v1/identity/me` zwraca `userId` i `displayName` bieżącej tożsamości. `GET /api/v1/training/sessions?from=YYYY-MM-DD&to=YYYY-MM-DD` zwraca podsumowania widocznych, nieusuniętych sesji włącznie z datami granicznymi, bez limitu 20 Dashboardu. Oba parametry są wymagane; odwrócony zakres lub różnica granic ponad 366 dni daje 400. Szkic i licznik Start now pozostają w przeglądarce; rozpoczęcie nie tworzy zasobu serwera.

## Stan i odpowiedzialność

Katalog skalny z 2026-10-17: dodano sektory, filtr `sectorId`, długość w metrach i mapowanie Kurtyki w POST katalogu. Lokalna baza ma publiczne 28 pozycji Filaru Abazego. [Kontrakt i zakres weryfikacji](outdoor-catalog.md).

Scenariusze katalogowe uzgodnione 2026-10-17: [A — nowe i znane wspinaczki w sesji; B — zapamiętanie wspinaczek bez sesji](catalog-climb-scenarios.md). Dodano osobny POST Climb i GET pojedynczej pozycji; kompilacja potwierdzona, testy do uruchomienia.

Wcześniej potwierdzono `POST /api/v1/training/sessions`, `GET /api/v1/training/sessions/{sessionId}`, `GET /api/v1/dashboard/sessions` oraz `GET /api/v1/dashboard/personal-bests`. Dodano `PUT` i `DELETE` sesji oraz katalog Area/Climb, wycen i profili pod `/api/v1/catalog`. Dokładne URL, parametry i stan weryfikacji zapisuje handoff. Błędy per pole i docelowa paginacja pozostają dalszym zakresem (`OPEN-14`).

2026-10-17 dodano automatyczne OpenAPI i Swagger UI: `/v3/api-docs` oraz `/swagger-ui.html`. [Uruchomienie i pełne adresy](development.md#swagger-ui-i-openapi). Kompilacja przeszła; interfejs i wygenerowana specyfikacja oczekują na sprawdzenie HTTP po uruchomieniu backendu.

| Operacja                     | Potrzeba                                       | Status                                         |
| ---------------------------- | ---------------------------------------------- | ---------------------------------------------- |
| Odczyt słowników             | wyceny i profile formularza                    | kod dodany; testy do uruchomienia              |
| Wyszukanie Area/Climb        | wybór po UUID, filtrowanie po miejscu i nazwie | kod dodany; testy do uruchomienia              |
| Podgląd obliczeń szkicu      | wynik bez tworzenia sesji i draftów w bazie    | propozycja OPEN-04                             |
| Utworzenie ukończonej sesji  | atomowy zapis wszystkich wpisów                | wcześniej przetestowane; regresja do wykonania |
| Lista sesji i Details        | Dashboard i chronologiczne snapshoty           | wcześniej przetestowane; regresja do wykonania |
| Edycja i usuwanie sesji      | wersja, przeliczenie edytowanej sesji i audyt  | kod dodany; testy do uruchomienia              |
| Reewaluacja dalszej historii | osobna jawna operacja                          | późniejszy przyrost                            |
| Calendar / profile / sync    | dalsze przekroje i integracja mobile           | późniejsze przyrosty                           |

Nie projektujemy endpointu rozpoczynającego sesję dla bieżącego `Record session`.

## Wejście ukończonej sesji

To lista wymaganych pojęć, nie zamrożony schemat JSON:

| Grupa            | Dane klienta                                                                                                          |
| ---------------- | --------------------------------------------------------------------------------------------------------------------- |
| Tożsamość ------ | stabilny `clientSessionId` ------------------------------------------------------------------------------------------ |
| Data i czas ---- | `sessionDate`, `timeZoneId`, faktyczny czas treningu i potrzebne start/koniec; sposób wyrażenia precyzji do ustalenia |
| Miejsce -------- | dokładnie jedno Area dla całej sesji: istniejące `areaId` albo `clientAreaId` z nazwą szybkiej lokalizacji (DEC-018)  |
| Opis ----------- | opcjonalne `sessionName`, notatka sesji ----------------------------------------------------------------------------- |
| Wpisy ---------- | uporządkowana lista, własne `clientEntryId`, `entryOrder` ----------------------------------------------------------- |
| Wspinaczka ----- | `climbId` albo stabilne `clientClimbId` i dane prywatnej drogi ------------------------------------------------------ |
| Fakty próby ---- | typ wspinania, wycena, profil, rezultat, tryb, pełne i wykonane ruchy, opcjonalna notatka --------------------------- |
| Wyjątek historii | jawne potwierdzenie OS/Flash sprzecznego z historią, jeżeli wystąpi ------------------------------------------------- |

Klient nie dostarcza wiążącego `userId`, poziomu wyliczonego, punktów, mnożników, loadu ani cache'u summary. Backend identyfikuje użytkownika, waliduje dostęp i rozwiązuje obiekty katalogu.

W pierwszym przyroście klient przekazuje `climbType` i kanoniczne `gradeLabel`, a backend wyprowadza `BOULDER_FONT` dla `BOULDER` oraz `ROUTE_FRENCH` dla `ROUTE` i `CIRCUIT`. Rozszerzenie o inne systemy doda `gradeSystemCode`: etykieta V-scale albo skali Kurtyki zostanie zmapowana na kanoniczny `Grade`, zachowując jednocześnie oryginalny system i etykietę w snapshocie. System wejściowy nie dostarcza własnych punktów ani indeksu.

Backend sprawdza zgodność każdego istniejącego Climb i jego opcjonalnego Sector z Area sesji. Nowe prywatne wspinaczki tworzy w tym samym Area. Wpis z innego Area jest błędem walidacji całego atomowego zapisu; sama filtracja selektora w UI nie wystarcza. Różne sektory tego samego Area są dozwolone. Inne Area oznacza osobny request utworzenia sesji z własnym `clientSessionId`, również dla tej samej daty.

## Obliczenie i transakcja

Backend sprawdza cały request i tożsamość retry, odczytuje odpowiednią historię, rozwiązuje stabilne ID oraz potrzebny poziom odniesienia. Liczy wpisy według kolejności domenowej, uwzględniając wcześniejsze wpisy tej sesji. Następnie zapisuje sesję, nowe obiekty katalogowe, wpisy, snapshoty i summary jako jedną operację atomową. Błąd jednego wpisu nie może zostawić częściowego treningu ani osieroconych draftów.

Przy braku poziomu dotychczasowy model pozwala wyznaczyć go po pierwszej sesji i użyć do jej obliczenia. Moment obowiązywania, preview i trudne przypadki estymacji pozostają otwarte (`OPEN-07`, `OPEN-11`). Kolejność między sesjami, wpisy historyczne i ochrona przed równoległą zmianą historii wymagają jawnej polityki (`OPEN-12`).

## Idempotencja i błędy

Ustalone zachowanie: ponowienie zapisu pod tym samym `clientSessionId` nie tworzy drugiej sesji ani dodatkowych kontaktów w historii. Ukończonego treningu nie zmieniamy ukrytym retry o innej treści; służy do tego jawna edycja. Szkic pozostaje lokalnie do potwierdzenia sukcesu.

Wdrożona idempotencja używa unikalności `(userId, clientSessionId)` i SHA-256 znormalizowanej komendy. Identyczne ponowienie zwraca istniejące `sessionId`, a inna treść pod tym samym kluczem daje `409 CLIENT_SESSION_ID_CONFLICT`. Współbieżne pierwsze żądania nadal wymagają atomowej obsługi kolizji ograniczenia unikalnego.

Konflikt historii OS/Flash zwraca `409 ASCENT_MODE_HISTORY_CONFLICT`, aby frontend wyświetlił ostrzeżenie. Ponowiony zapis danego wpisu z jawnym potwierdzeniem konfliktu zachowuje deklarację OS/Flash, mnożnik `1.50`, rzeczywistą znajomość i audyt override (DEC-031, DEC-039). Pozostały kontrakt błędów nadal używa `ProblemDetail` i wymaga rozwinięcia walidacji per pole/wpis oraz konfliktu wersji edytowanej sesji.

## Odczyt dla Dashboardu

Wdrożony endpoint `GET /api/v1/dashboard/sessions` zwraca maksymalnie 20 sesji bieżącego użytkownika, sortowanych po `sessionDate` i `createdAt` malejąco. Nie przyjmuje `userId`; serwis pobiera go przez `CurrentUserProvider`. Read model zawiera `sessionId`, `sessionName`, `sessionDate`, `areaName`, `durationMinutes`, `totalMoves`, `classicLoad`, `adjustedLoad`, `averageMoveIntensity`, `maxBoulderGrade` i `maxRouteGrade`.

Kontroler i serializacja mają test MVC, a zapytanie ma test integracyjny z PostgreSQL. Pusta lokalna baza zwraca `[]`. Nie wdrożono jeszcze paginacji sterowanej przez klienta, odpowiedzi błędów ani danych dla pozostałych sekcji Dashboardu.

Potrzebne są stabilne ID sesji, nazwa, lokalna data, miejsce, czas i podsumowanie z `adjustedLoad`, `classicLoad` oraz ruchami. `Max grade` dotyczy ukończonych wspinaczek, z rozdzieleniem skal balda i drogi/obwodu; przy samych próbach brak ukończonej wyceny. Nie zwracamy jednej porównanej wyceny z nieporównywalnych skal.

Scenariusz Bronx potwierdził średnią intensywność ocenianych ruchów. Personal bests obejmują maksimum sesji, rekord życiowy, najlepszy wynik z ostatnich 12 miesięcy, najwyższe RP, najwyższe OS i Flash oraz różnicę indeksów `RP - OS`, zawsze osobno dla skal. Rekord stopnia zwraca co najmniej etykietę wyceny, datę osiągnięcia i `sessionId`. Największa liczba ruchów na jednej wspinaczce nie jest rekordem. Dla analizy pracy projektowej ruchy wymagają kontekstu wyceny, profilu, intensywności i loadu; nie dodajemy samodzielnego `maxMovesOnSingleClimb` do DTO.

Katalog wyszukuje Area i Climb po nazwie, ale odpowiedzi oraz komendy używają UUID jako tożsamości. Nazwa nie jest kluczem i nie może automatycznie scalać podobnych obiektów.

Edycja historycznej sesji przelicza wyłącznie edytowany agregat i zapisuje audyt. Nie uruchamia automatycznej kaskady przez wszystkie późniejsze sesje. Ich snapshoty pozostają historyczne; przyszła reewaluacja wielu sesji musi być osobną, jawną operacją z podglądem zakresu i skutków.

W kodzie i URL używamy terminu `personal best`, ponieważ techniczne `record` jest wieloznaczne. Endpoint wyników życiowych to `GET /api/v1/dashboard/personal-bests`; odpowiedź rozdziela `boulder` i `route`, a w każdej skali zawiera `overall`, `os`, `flash`, `rp` i `rpVsOsGap`. Brak osiągnięcia w danej kategorii ma wartość `null`.

DEC-045, wdrożenie 2026-11-03: TOP_ROPE jest wyłączone z rekordów osobistych, maksimów osiągnięć sesji i Fast RP. Wędka pozostaje w treningu i loadzie. Raport piramidy liczy różne wspinaczki po filtrze okresu i stylu, niezależnie od limitu 20 sesji Dashboardu. Piramida RP obejmuje OS/Flash/RP, OS tylko OS; ta semantyka widoku nie zmienia istniejącego pola personal best `rp`. [Kontrakt i weryfikacja](achievement-pyramid-backend.md).

Pierwszy read model Dashboardu dostarcza tekstowe metryki z prawdziwych danych i nie musi dostarczać serii do wykresu. Rozkład prób/przejść według wycen wymaga osobnego zatwierdzenia; jeśli zostanie dodany, skale pozostają rozdzielone (DEC-022). Inne sekcje Dashboardu nie poszerzają automatycznie pierwszego zakresu API.

## Odczyt dla Details

Wdrożony `GET /api/v1/training/sessions/{sessionId}` zwraca nagłówek i podsumowanie sesji oraz płaską listę wpisów uporządkowaną po `entryOrder`. Każdy oceniany wpis zachowuje stabilne `climbId`, snapshot nazwy, wyceny i profilu, ruchy, obliczenia oraz kontekst historii. WARMUP ma dane nieoceniane jako `null` i zerowe loady. Brak sesji i sesja innego użytkownika zwracają to samo `404`.

Frontend grupuje listę po `climbId`, zachowując `entryOrder` do pokazania chronologii. Grupowanie po samej nazwie jest niedopuszczalne. Powrót do tej samej drogi po innej musi być możliwy do odtworzenia bez równoległej, zduplikowanej struktury grup w odpowiedzi API.

Próba prezentuje rezultat, tryb, ruchy, Classic Load, Adjusted Load oraz rozwijane EDL, intensywność, poziom i mnożniki. Różnych historycznych wycen/profili nie zastępujemy jedną aktualną wartością w nagłówku grupy. Opis drogi i notatka próby są odrębne; miniatura jest opcjonalna, a upload zdjęć poza automatycznym zakresem etapu.

Otwarte pozostają opcjonalne opisy i media katalogowe oraz sposób oznaczenia różnicy między aktualnymi danymi katalogu a historycznym snapshotem (`OPEN-06`).

## Scenariusze akceptacyjne kontraktu

Próba → przejście tej samej drogi; wpisy tej drogi rozdzielone inną; te same nazwy w różnych miejscach; retry po utracie odpowiedzi; inny payload pod tym samym ID; jeden niepoprawny wpis; sesja tylko z próbami; sesja mieszana; brak poziomu; konflikt OS/Flash; zmiana/archiwizacja katalogu; odczyt po zmianie strefy i kontrola właściciela.

DEC-018: wpisy z różnych sektorów jednego Area przechodzą walidację; wpis wskazujący Climb z innego Area powoduje odrzucenie całego zapisu bez częściowych danych. Dwie osobne sesje w różnych Area mogą mieć tę samą `sessionDate`.

Źródła: [model](../archive/source-snapshot/docs/CB_model.md), [backend](../archive/source-snapshot/docs/CB_Backend.md), [ustalenia web](../archive/source-snapshot/docs/CB_web_stage_1.md). Starsze endpointy z `old-doc` zachowano jako historię, nie skopiowano ich jako istniejącego API Javy.
