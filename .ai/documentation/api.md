# API

Aktualizacja: 2026-11-15. Kontrakt potwierdzony w kontrolerach i komendach `backend/src/main/java/pl/climbbetter`. Pełne pola DTO: OpenAPI działającego backendu pod `/v3/api-docs`; [uruchomienie](development.md).

## Tożsamość

`/api/v1/**` wymaga `Authorization: Bearer <Firebase ID token>` w standardowym trybie. Użytkownik wynika z tokenu i mapowania na UUID; nie przyjmujemy dowolnego `userId`. Bootstrap może użyć poprawnej, jeszcze niepowiązanej tożsamości. Health jest publiczny, Swagger/OpenAPI publiczne wyłącznie w local/test. [Zasady i konfiguracja Firebase](firebase-authentication-local.md).

| Metoda i ścieżka                  | Wynik                                                                 |
| --------------------------------- | --------------------------------------------------------------------- |
| `POST /api/v1/identity/bootstrap` | idempotentne utworzenie/pobranie konta CB                             |
| `GET /api/v1/identity/me`         | `userId`, `displayName`, `profileComplete`; bez tworzenia konta       |
| `PUT /api/v1/identity/me`         | `{ "displayName": "Mihu" }`; nazwa 1–80 znaków, potwierdzenie profilu |

## Sesje i Dashboard

| Metoda i ścieżka                                              | Zachowanie                                                      |
| ------------------------------------------------------------- | --------------------------------------------------------------- |
| `POST /api/v1/training/sessions`                              | atomowy zapis; `201`, `{sessionId}`, nagłówek Location          |
| `GET /api/v1/training/sessions?from=YYYY-MM-DD&to=YYYY-MM-DD` | podsumowania do kalendarza                                      |
| `GET /api/v1/training/sessions/{id}`                          | Details: wersja, identyfikatory i płaskie wpisy w `entryOrder`  |
| `PUT /api/v1/training/sessions/{id}`                          | `{expectedVersion, changeReason, session}`; `200` z Details     |
| `DELETE /api/v1/training/sessions/{id}?expectedVersion=…`     | soft delete z audytem; `204`                                    |
| `GET /api/v1/dashboard/sessions`                              | ostatnie 20 sesji; statystyki nie są ograniczone tą listą       |
| `GET /api/v1/dashboard/personal-bests`                        | rekordy lifetime/recent (12 miesięcy), osobno dla skal i stylów |

POST i pole `session` w PUT przyjmują `clientSessionId`, opcjonalną nazwę, `sessionDate`, `timeZoneId`, `startedAt`, `endedAt`, miejsce i niepustą listę `entries`. Istniejące miejsce wskazujemy `areaId`, nowe — `clientAreaId` + `areaName`. Nie podajemy obu ID. PUT zachowuje `clientSessionId` z Details.

Wpis ma `clientEntryId`, `entryOrder`, `resultType`, dodatnie `executedMoves`, opcjonalne `notes`. Oceniany wpis dodaje `climbId` albo `clientClimbId` + `climbName`, `climbType`, kanoniczne `gradeLabel`, `effortProfileCode`, `ascentMode`, dodatnie `totalMoves`; ROUTE także `protectionMode`. WARMUP pomija pola oceny. ASCENT wymaga równej liczby ruchów. `historyConflictConfirmed=true` jest jawną zgodą na konflikt OS/Flash, nie domyślnym polem każdego zapisu.

Backend sam ustala punkty, znajomość, poziom, mnożniki i load. Nowy POST/PUT używa v3; `calculation` zawiera snapshot profilu, opcjonalny `crux`, efektywny poziom/różnicę oraz źródło, wersję estymatora, horyzont, sample size i masę historii. Odpowiedzi odróżniają `v1`, `v2`, `v3`, `mixed`; stare pola mogą być NULL. [Model](../business/grading-and-scoring.md), [kolumny snapshotów](database.md).

Retry z tym samym ID i treścią zwraca zapisany wynik bez przeliczenia. Inna treść po sukcesie wymaga PUT. Edycja przelicza cały agregat i audyt, bez kaskady późniejszych sesji. Brak/cudza/usunięta sesja daje `404`; usunięty client ID pozostaje zarezerwowany.

POST i PUT odrzucają nakładanie czasu aktywnych sesji użytkownika zgodnie z [DEC-053](decisions.md): `409 SESSION_TIME_CONFLICT`. Odpowiedź nie zatwierdza części sesji; klient może poprawić czas i ponowić zapis z zachowaniem szkicu. Identyczny retry potwierdzonego POST nadal zwraca istniejący wynik.

## Katalog

Prefix `/api/v1/catalog`: `GET /areas`, `GET /areas/{areaId}/sectors`, `GET /areas/{areaId}/climbs`, `GET /areas/{areaId}/climbs/{climbId}`, `POST /areas/{areaId}/climbs`, `GET /grades?climbType=…`, `GET /effort-profiles`.

Listy zwracają tablice. Wyszukiwanie `q` dopasowuje fragment nazwy bez wielkości liter; `%` i `_` są literalne. `limit` domyślnie 20, zakres 1–50, kolejność nazwa/UUID. Climb może filtrować `climbType` i `sectorId`. Dane katalogowe są podpowiedziami; UUID wskazuje tożsamość, snapshot sesji zachowuje użyte wartości. [Scenariusze, metry i aliasy](catalog-climb-scenarios.md).

## Raporty i błędy

- `GET /api/v1/statistics/pyramid` oraz `/pyramid/climbs`: [parametry, paginacja i odpowiedzi](achievement-pyramid-backend.md).
- `GET /api/v1/statistics/summary`: [miesiąc/rok, metryki i reguły](period-training-summary.md).
- Błędy biznesowe używają `ProblemDetail` z `code`. Ważne konflikty `409`: `CLIENT_SESSION_ID_CONFLICT`, `SESSION_VERSION_CONFLICT`, `SESSION_TIME_CONFLICT`, `ASCENT_MODE_HISTORY_CONFLICT`, `CLIENT_CLIMB_ID_CONFLICT`; walidacja `400`. [Kody auth](firebase-authentication-local.md).

Nie ma endpointu Start ani wdrożonego preview. Czas rejestrowania i roboczy stan pozostają po stronie klienta do końcowego zapisu.
