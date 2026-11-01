# Backend CRUD — kontrakt do integracji React

Data: 2026-10-17. Status: pełny zestaw 51 testów backendu przeszedł, bez failures/errors/skipped. CRUD i katalog są potwierdzone w zakresie opisanych testów. [Raport weryfikacji](backend-verification.md). Wcześniejsze wzmianki o oczekujących testach w opisie prac są zastąpione tym wynikiem.

## Endpointy sesji

- `POST /api/v1/training/sessions`: dotychczasowy kontrakt; odpowiedź `201` z `sessionId` i nagłówkiem `Location`.
- `GET /api/v1/training/sessions/{sessionId}`: pełne Details z `clientSessionId`, `version` i uporządkowanymi wpisami.
- `PUT /api/v1/training/sessions/{sessionId}`: zastępuje cały agregat; odpowiedź `200` z nowymi Details.
- `DELETE /api/v1/training/sessions/{sessionId}?expectedVersion=…`: odpowiedź `204` bez treści.

Treść PUT:

```json
{
  "expectedVersion": 0,
  "changeReason": "Correct executed moves",
  "session": {
    "clientSessionId": "UUID z Details",
    "sessionName": "Evening training",
    "sessionDate": "2026-10-17",
    "timeZoneId": "Europe/Warsaw",
    "startedAt": "2026-10-17T16:00:00Z",
    "endedAt": "2026-10-17T17:00:00Z",
    "areaId": "UUID z katalogu albo Details",
    "entries": [
      {
        "clientEntryId": "UUID z Details albo nowy UUID dla nowego wpisu",
        "entryOrder": 0,
        "resultType": "ASCENT",
        "climbId": "UUID z katalogu albo Details",
        "climbType": "BOULDER",
        "gradeLabel": "5",
        "effortProfileCode": "BOULDER",
        "ascentMode": "OS",
        "totalMoves": 8,
        "executedMoves": 8
      }
    ]
  }
}
```

Powyższe wartości opisowe `UUID …` trzeba zastąpić prawdziwymi UUID. `expectedVersion` zawsze pochodzi z ostatnich Details, a nie ze stałej w UI. Nowe sesje zaczynają od wersji `0`; wcześniej zapisane zachowują swoją wersję. Edycja i usunięcie zwiększają wersję raz.

`clientSessionId` jest stałe. Istniejące Area wskazujemy przez `areaId`; nowe przez `clientAreaId` i `areaName`. Nie wysyłamy obu ID naraz. Analogicznie Climb używa `climbId` albo `clientClimbId` z nazwą. POST nadal potrafi szybko utworzyć prywatne pozycje bez osobnego endpointu tworzenia katalogu.

PUT usuwa stare wpisy w obrębie transakcji przed odczytem historii i wyznaczaniem poziomu, następnie zapisuje przeliczone wpisy oraz summary. Błąd wycofuje całą zmianę wraz z wersją i audytem. Nie przeliczamy automatycznie późniejszych sesji (DEC-038).

Edycja zapisuje powód oraz pełne Details przed i po zmianie do tabeli z `V13`. Usuwanie ustawia `deleted_at` z `V14`, zachowuje wpisy i poprzedni audyt oraz dodaje rewizję usunięcia. Usunięte sesje są pomijane przez Details, Dashboard, personal bests, historię kontaktów i wyznaczanie poziomu. Ponowny POST z ich `clientSessionId` daje konflikt, więc retry nie odtwarza usuniętej sesji. Katalogowe Area/Climb pozostają dostępne po usunięciu sesji.

Błędy: nieaktualna wersja daje `409 SESSION_VERSION_CONFLICT`; sprzeczność OS/Flash z historią daje `409 ASCENT_MODE_HISTORY_CONFLICT`; niepoprawne dane dają `400`. Brak, usunięta sesja i sesja innego użytkownika są nierozróżnialnym `404`. Ponowne DELETE usuniętej sesji również daje `404`.

## Katalog formularza

Rozszerzenie 2026-10-17: dodano niezależny `POST /api/v1/catalog/areas/{areaId}/climbs` oraz `GET /api/v1/catalog/areas/{areaId}/climbs/{climbId}`. Pozwalają zapamiętać prywatne wspinaczki bez sesji i potem używać ich w treningu. Szczegóły i przykład Swagger: [scenariusze A/B](catalog-climb-scenarios.md). Kompilacja przeszła; testy tego rozszerzenia pozostają do uruchomienia.

- `GET /api/v1/catalog/areas?q=…&limit=20`: lista `id`, `name`, `areaType`, `facilityType`, `isPublic`, `resolutionStatus`.
- `GET /api/v1/catalog/areas/{areaId}/climbs?q=…&climbType=BOULDER&limit=20`: lista `id`, `areaId`, `name`, `climbType`, `gradeScaleCode`, `gradeLabel`, `effortProfileCode`, `suggestedMoveCount`, `isPublic`.
- `GET /api/v1/catalog/grades?climbType=BOULDER`: uporządkowane wyceny `id`, `scaleCode`, `label`, `gradeIndex`. `ROUTE` i `CIRCUIT` korzystają z `ROUTE_FRENCH`, `BOULDER` z `BOULDER_FONT`.
- `GET /api/v1/catalog/effort-profiles`: aktywne profile `code`, `displayName`, `description`, `baseEdl`.

Odpowiedzi endpointów list są tablicami JSON. Wyszukiwanie dopasowuje fragment nazwy bez rozróżniania wielkości liter; puste `q` zwraca pierwsze pozycje. `%` i `_` są zwykłymi znakami, nie wildcardami. Sortowanie jest po nazwie i UUID, limit mieści się w `1–50`. `climbType` w wyszukiwaniu Climb jest opcjonalne. Katalog obsługuje tworzenie i odczyt Climb; edycja/usuwanie katalogu, paginacja i sektory pozostają poza tym zakresem.

Katalog obejmuje własne prywatne i publiczne pozycje, w tym własne szybkie drafty. Ukrywa archiwalne i scalone Area oraz archiwalne Climb. Cudze prywatne Area i brak Area dają takie samo `404` przy pobieraniu wspinaczek. Dostęp do publicznego Area nie odsłania cudzych prywatnych Climb.

Nazwy nie są tożsamością. Przy wyborze pozycji React przechowuje UUID, a dane katalogowe są edytowalnymi podpowiedziami formularza; historyczne Details zachowują snapshoty.

## Uruchomienie i weryfikacja

Profil backendu `local` dopuszcza CORS z `http://localhost:5173` i `http://127.0.0.1:5173`, dla metod GET/POST/PUT/DELETE/OPTIONS i nagłówka Content-Type. Jest to konfiguracja lokalnej integracji Vite. Produkcyjne uwierzytelnianie pozostaje odłożone; obecny `CurrentUserProvider` działa w profilach local/test.

Nowy `TrainingSessionCrudIntegrationTest` zawiera przebieg create → read → update → delete, wybór istniejących Area/Climb, zmianę loadu, audyt, konflikt wersji, rollback błędnej edycji, ukrycie usuniętej sesji i ochronę przed jej odtworzeniem przez POST. Drugi scenariusz sprawdza cudze sesje oraz prywatność i archiwizację katalogu. Testy wykonują rzeczywiste transakcje serwisu bez zewnętrznej transakcji testowej i sprzątają swoje dane przez `finally`.

Przed uruchomieniem testów Docker Desktop musi działać, a `docker version` zwracać Client i Server. W Bash, w katalogu `backend`:

```bash
./mvnw -Dtest=TrainingSessionCrudIntegrationTest,TrainingSessionFlowIntegrationTest test
```

Testy nie zostały uruchomione przez asystenta. Ich sukces jest warunkiem potwierdzenia działania nowych migracji i CRUD. Po nim przechodzimy do integracji React; recent personal bests, równoległe pierwsze retry, produkcyjny auth i dalsza analityka pozostają poza tym przyrostem.

Na kolejne polecenie użytkownika z 2026-10-17 dodano Swagger UI pod `/swagger-ui.html` oraz OpenAPI JSON pod `/v3/api-docs`. Kompilacja aplikacji i testów przeszła; HTTP czeka na sprawdzenie. [Instrukcja uruchomienia](development.md#swagger-ui-i-openapi).

Źródła: polecenie użytkownika z 2026-10-17; kontrolery i serwisy w `backend/src/main/java/pl/climbbetter`; migracje `V13`–`V14`; `TrainingSessionCrudIntegrationTest`.
