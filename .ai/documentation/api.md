# API i przepływ danych

Status: CRUD sesji, katalog i odczyt kalendarza potwierdzone pełnym zestawem 52 testów backendu, bez failures/errors/skipped. Aktualizacja: 2026-10-17. [Raport weryfikacji](backend-verification.md).

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
