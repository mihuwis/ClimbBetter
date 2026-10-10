# API i przepływ danych

Status: częściowo wdrożony kontrakt Dashboardu; bez gotowego OpenAPI. Aktualizacja: 2026-10-10.

## Stan i odpowiedzialność

W Javie działa `GET /api/v1/dashboard/sessions`. Rozpoczęto kontrakt zapisu przez `CreateTrainingSessionCommand`, lecz brakujący `TrainingEntryCommand`, kontroler, serwis i repozytorium oznaczają, że `POST` jeszcze nie istnieje. Dokładne DTO Details, błędy oraz docelowa paginacja nadal wymagają ustalenia (`OPEN-14`).

| Operacja                     | Potrzeba                                               | Status                                      |
| ---------------------------- | ------------------------------------------------------ | ------------------------------------------- |
| Odczyt słowników ----------- | skale, profile i reguły potrzebne formularzowi ------- | wymagana ---------------------------------- |
| Wyszukanie Area/Sector/Climb | filtrowanie po miejscu, historia użycia, stabilne ID   | wymagana; szczegóły dopasowania proponowane |
| Podgląd obliczeń szkicu ---- | wynik bez tworzenia sesji i draftów katalogu w bazie   | propozycja `OPEN-04` ---------------------- |
| Utworzenie ukończonej sesji  | atomowy zapis wszystkich wpisów ---------------------- | wymagana ---------------------------------- |
| Lista sesji ---------------- | dane kart Dashboardu --------------------------------- | pierwszy GET wdrożony i przetestowany ----- |
| Szczegóły sesji ------------ | grupowanie wspinaczek, uporządkowane próby i snapshoty | wymagana; kształt odczytu `OPEN-06` ------- |
| Edycja / reewaluacja ------- | korekta historii z audytem --------------------------- | późniejszy przyrost ----------------------- |
| Calendar / profile / sync -- | dalsze przekroje i integracja mobile ----------------- | późniejsze przyrosty ---------------------- |

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

Backend sprawdza zgodność każdego istniejącego Climb i jego opcjonalnego Sector z Area sesji. Nowe prywatne wspinaczki tworzy w tym samym Area. Wpis z innego Area jest błędem walidacji całego atomowego zapisu; sama filtracja selektora w UI nie wystarcza. Różne sektory tego samego Area są dozwolone. Inne Area oznacza osobny request utworzenia sesji z własnym `clientSessionId`, również dla tej samej daty.

## Obliczenie i transakcja

Backend sprawdza cały request i tożsamość retry, odczytuje odpowiednią historię, rozwiązuje stabilne ID oraz potrzebny poziom odniesienia. Liczy wpisy według kolejności domenowej, uwzględniając wcześniejsze wpisy tej sesji. Następnie zapisuje sesję, nowe obiekty katalogowe, wpisy, snapshoty i summary jako jedną operację atomową. Błąd jednego wpisu nie może zostawić częściowego treningu ani osieroconych draftów.

Przy braku poziomu dotychczasowy model pozwala wyznaczyć go po pierwszej sesji i użyć do jej obliczenia. Moment obowiązywania, preview i trudne przypadki estymacji pozostają otwarte (`OPEN-07`, `OPEN-11`). Kolejność między sesjami, wpisy historyczne i ochrona przed równoległą zmianą historii wymagają jawnej polityki (`OPEN-12`).

## Idempotencja i błędy

Ustalone zachowanie: ponowienie zapisu pod tym samym `clientSessionId` nie tworzy drugiej sesji ani dodatkowych kontaktów w historii. Ukończonego treningu nie zmieniamy ukrytym retry o innej treści; służy do tego jawna edycja. Szkic pozostaje lokalnie do potwierdzenia sukcesu.

Plan techniczny proponuje unikalność `(userId, clientSessionId)`, fingerprint requestu, ten sam wynik dla identycznego ponowienia i `409` dla różnej treści. Szczegóły normalizacji fingerprintu oraz retry uzupełnionego o potwierdzenie konfliktu trzeba opisać w kontrakcie.

Plan błędów: `ProblemDetail`, walidacja per pole/wpis, rozróżnienie braku dostępu/not found, konfliktu wersji i konfliktu historii OS/Flash. Dla ostatniego wcześniejszy plan wskazuje kod `ASCENT_MODE_HISTORY_CONFLICT`. Samo potwierdzenie nie rozstrzyga macierzy mnożników (`OPEN-10`).

## Odczyt dla Dashboardu

Wdrożony endpoint `GET /api/v1/dashboard/sessions` zwraca maksymalnie 20 sesji bieżącego użytkownika, sortowanych po `sessionDate` i `createdAt` malejąco. Nie przyjmuje `userId`; serwis pobiera go przez `CurrentUserProvider`. Read model zawiera `sessionId`, `sessionName`, `sessionDate`, `areaName`, `durationMinutes`, `totalMoves`, `classicLoad`, `adjustedLoad`, `maxBoulderGrade` i `maxRouteGrade`.

Kontroler i serializacja mają test MVC, a zapytanie ma test integracyjny z PostgreSQL. Pusta lokalna baza zwraca `[]`. Nie wdrożono jeszcze paginacji sterowanej przez klienta, odpowiedzi błędów ani danych dla pozostałych sekcji Dashboardu.

Potrzebne są stabilne ID sesji, nazwa, lokalna data, miejsce, czas i podsumowanie z `adjustedLoad`, `classicLoad` oraz ruchami. `Max grade` dotyczy ukończonych wspinaczek, z rozdzieleniem skal balda i drogi/obwodu; przy samych próbach brak ukończonej wyceny. Nie zwracamy jednej porównanej wyceny z nieporównywalnych skal.

Scenariusz Bronx rozszerza plan pierwszego użytecznego Dashboardu o średnią intensywność ocenianych ruchów oraz opcjonalne rekordy oparte na faktach. Kandydaci to największa liczba ruchów, największa praca na trudnych problemach i najwyższe RP. Obecny `DashboardSessionSummary` nie zawiera jeszcze tych pól; definicje rekordów i ich DTO trzeba ustalić po potwierdzeniu zapisu podstawowych metryk.

Pierwszy read model Dashboardu dostarcza tekstowe metryki z prawdziwych danych i nie musi dostarczać serii do wykresu. Rozkład prób/przejść według wycen wymaga osobnego zatwierdzenia; jeśli zostanie dodany, skale pozostają rozdzielone (DEC-022). Inne sekcje Dashboardu nie poszerzają automatycznie pierwszego zakresu API.

## Odczyt dla Details

Odpowiedź musi zachować wszystkie wpisy, `entryOrder`, stabilną tożsamość wspinaczki i snapshot każdego wyniku. Grupowanie po samej nazwie jest niedopuszczalne. Powrót do tej samej drogi po innej musi być możliwy do odtworzenia chronologicznie.

Próba prezentuje rezultat, tryb, ruchy, Classic Load, Adjusted Load oraz rozwijane EDL, intensywność, poziom i mnożniki. Różnych historycznych wycen/profili nie zastępujemy jedną aktualną wartością w nagłówku grupy. Opis drogi i notatka próby są odrębne; miniatura jest opcjonalna, a upload zdjęć poza automatycznym zakresem etapu.

Otwarte: czy API zwraca grupy z agregatami, czy tylko dane do grupowania w webie, jak oznaczyć aktualne dane katalogu i jakie dokładnie pola snapshotu wykorzystać (`OPEN-06`).

## Scenariusze akceptacyjne kontraktu

Próba → przejście tej samej drogi; wpisy tej drogi rozdzielone inną; te same nazwy w różnych miejscach; retry po utracie odpowiedzi; inny payload pod tym samym ID; jeden niepoprawny wpis; sesja tylko z próbami; sesja mieszana; brak poziomu; konflikt OS/Flash; zmiana/archiwizacja katalogu; odczyt po zmianie strefy i kontrola właściciela.

DEC-018: wpisy z różnych sektorów jednego Area przechodzą walidację; wpis wskazujący Climb z innego Area powoduje odrzucenie całego zapisu bez częściowych danych. Dwie osobne sesje w różnych Area mogą mieć tę samą `sessionDate`.

Źródła: [model](../archive/source-snapshot/docs/CB_model.md), [backend](../archive/source-snapshot/docs/CB_Backend.md), [ustalenia web](../archive/source-snapshot/docs/CB_web_stage_1.md). Starsze endpointy z `old-doc` zachowano jako historię, nie skopiowano ich jako istniejącego API Javy.
