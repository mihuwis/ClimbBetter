# Katalog wspinaczek — scenariusze A i B

Data: 2026-10-17. Status: wszystkie 4 scenariusze `CatalogClimbScenariosIntegrationTest` przeszły w pełnym zielonym zestawie 51 testów backendu. [Raport weryfikacji](backend-verification.md). Źródła: scenariusze użytkownika z 2026-10-17, DEC-010, DEC-037, WEB-002 oraz kontrolery katalogu i treningu.

## A — trening z nowymi i wcześniej zapisanymi wspinaczkami

Użytkownik wybiera Bronx przez `GET /api/v1/catalog/areas?q=Bronx`. Rozpoczęcie i robocze wpisy sesji pozostają lokalne w kliencie. React z odzyskiwalnym szkicem jest nadal do implementacji; backend nie tworzy rekordu Start.

Nowa wspinaczka dodana podczas treningu dostaje stabilne `clientClimbId`. Kolejne próby tej samej nowej wspinaczki używają tego samego `clientClimbId`, ale odrębnych `clientEntryId` i `entryOrder`. Znane wspinaczki użytkownik znajduje przez `GET /api/v1/catalog/areas/{areaId}/climbs?q=…` i wskazuje przez `climbId`.

Po zakończeniu klient wysyła cały agregat przez `POST /api/v1/training/sessions`. Jedna lista `entries` może zawierać trzy nowe wspinaczki i trzy powtórzenia z poprzedniej sesji. Pola identyfikacji ocenianego wpisu:

- Nowy Climb: `clientClimbId`, `climbName`, `climbType`, `gradeLabel`, `effortProfileCode`, `totalMoves`.
- Istniejący Climb: `climbId`, `climbType`, `gradeLabel`, `effortProfileCode`, `totalMoves`.
- Oba warianty: własny `clientEntryId`, `entryOrder`, `resultType`, `ascentMode`, `executedMoves`, opcjonalnie notatka i jawne potwierdzenie konfliktu historii.

Backend zapisuje nowe prywatne Climb wraz z sesją w jednej transakcji. Wcześniejsze Climb zachowują UUID, a nowe są odtąd dostępne w wyszukiwaniu właściciela. Powtórzenie znanej wspinaczki zwykle używa RP; OS/Flash sprzeczne z historią wymagają potwierdzenia. Nazwy nie służą do rozpoznawania tożsamości.

Przy błędnym wpisie cały zapis jest wycofywany. Jeśli wspinaczka ma zostać zapamiętana jeszcze przed zakończeniem sesji, można użyć niezależnego zapisu katalogowego z wariantu B, a w szkicu dalej wskazywać zwrócone `climbId`.

## B — zapamiętanie wspinaczek przed treningiem

Nowy endpoint:

```http
POST /api/v1/catalog/areas/{areaId}/climbs
Content-Type: application/json
```

Przykładowa treść w Swagger UI:

```json
{
  "clientClimbId": "11111111-1111-4111-8111-111111111111",
  "name": "Żółta po krawądkach",
  "climbType": "BOULDER",
  "gradeLabel": "5+",
  "effortProfileCode": "BOULDER",
  "suggestedMoveCount": 8
}
```

`areaId` to rzeczywisty UUID Bronx z GET Area. Każda nowa wspinaczka dostaje nowy `clientClimbId`; przy ponowieniu tego samego żądania zachowujemy poprzedni. Endpoint przyjmuje pojedynczy Climb, więc dla trzech pozycji wysyłamy trzy żądania.

Odpowiedź `201` zawiera `id`, `areaId`, `name`, `climbType`, `gradeScaleCode`, `gradeLabel`, `effortProfileCode`, `suggestedMoveCount`, `isPublic` oraz nagłówek `Location`. Nowy `GET /api/v1/catalog/areas/{areaId}/climbs/{climbId}` pozwala odczytać jedną dostępną pozycję. Dotychczasowe wyszukiwanie zwraca ją również w liście.

Właściciel pochodzi z `CurrentUserProvider`; klient nie podaje ownera ani publiczności. Nowe Climb są zawsze prywatne, także w publicznym Bronxie. Brak `suggestedMoveCount` dla BOULDER daje 8; ROUTE/CIRCUIT mogą pozostawić sugestię nieokreśloną. Skale pozostają niezależne, np. bald używa `6A`, a droga/obwód `6a`.

Zapis do katalogu nie tworzy sesji, próby, przejścia, loadu ani kontaktu w historii. Zapamiętanie balda przed pierwszą wizytą nie blokuje późniejszego OS. Jest to katalog do wykorzystania w treningu; daty planowanych sesji, listy celów i moduł planowania nie wchodzą w ten przyrost.

Identyczne ponowienie tego samego `clientClimbId` zwraca to samo ID bez duplikatu. Użycie tego ID dla innych danych, innego Area lub archiwalnej pozycji daje `409 CLIENT_CLIMB_ID_CONFLICT`. Unikalne ograniczenie i `INSERT … ON CONFLICT` chronią również równoczesne próby utworzenia tej samej katalogowej pozycji. Identyczna nazwa z innym ID może tworzyć odrębny Climb zgodnie z DEC-037.

Niepoprawne dane, wycena lub profil dają `400`; nieistniejące, archiwalne, scalone lub cudze prywatne Area daje `404`. Publiczne Area pozwala dodać własny prywatny Climb.

## Weryfikacja i dalsza praca

`CatalogClimbScenariosIntegrationTest` przygotowuje Mihu i publiczny Bronx wewnątrz transakcji testowej. Scenariusz A zapisuje poprzednią sesję, wyszukuje trzy Climb i zapisuje kolejną sesję z trzema nowymi oraz trzema powtórzeniami. Scenariusz B zapisuje trzy Climb bez sesji, sprawdza ponowienie i późniejsze użycie jednego z nich w treningu. Dodatkowy test obejmuje zgodność skal, dostęp i rozdzielenie identycznych nazw. Dane testowe są wycofywane po każdej metodzie i nie trafiają do trwałej bazy local.

Kompilacja aplikacji i testów zakończyła się sukcesem. Uruchomienie testów pozostaje po stronie użytkownika, przy działającym Docker Desktop:

```bash
./mvnw -Dtest=CatalogClimbScenariosIntegrationTest test
```

Po restarcie backendu nowy POST jest dostępny w [Swagger UI](http://localhost:8080/swagger-ui.html). Dla prywatnych pozycji Mihu backend musi działać z `CB_LOCAL_USER_ID=00000000-0000-0000-0000-000000000002` zgodnie z [instrukcją local](development.md#lokalne-dane-przykładowe-i-wybór-użytkownika).
