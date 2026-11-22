# Achievement pyramid — API i web

Aktualizacja: 2026-11-09. Backend, widoki SQL i ekran Statistics wdrożone. [Reguły oraz wzór estymatora](../business/achievement-pyramid.md) · [datowane testy](backend-verification.md).

## Odczyt

```http
GET /api/v1/statistics/pyramid?view=BOULDER&period=LAST_TWO_YEARS&style=RP&asOf=2026-11-09
```

Domyślne: `view=BOULDER`, `period=LAST_TWO_YEARS`, `style=RP`; alternatywy `ROUTE_AND_CIRCUIT`/`COMBINED`, `ALL_TIME`, `OS`. Opcjonalne `asOf` to data ISO (rok 1–9999), domyślnie dzień `Clock` UTC. Web wysyła lokalny dzień jawnie. Dwuletnie okno zaczyna się od `asOf.minusYears(2)`; granice są włączne.

Odpowiedź: `filters`, `windowStart` (NULL dla all-time), `windowEnd`, `uniqueClimbCount`, `layers`, `peak`, `levelAssessment`, `gradeCalibrationVersions`, powody niedostępności oceny/maksimum. Warstwa zawiera klucz, wycenę/indeks albo punkty/wersję kalibracji oraz liczność. Pojedyncza skala zachowuje zerowe stopnie między skrajnymi osiągnięciami.

Klucz pojedynczej skali: `grade:BOULDER_FONT:9`; wspólny: `points:1:92.5`. COMBINED zachowuje dokładne punkty i oddzielne kalibracje; `levelAssessment=null`, powód `COMBINED_SCALE`. Przy mieszanych kalibracjach `peak=null`, powód `MIXED_POINT_CALIBRATIONS`.

Ocena ma wycenę/indeks, metodę, confidence, sample size, support i liczniki dowodów, `algorithmVersion=pyramid-support-v1`. Ocenia wybrany raport, nie poziom dla loadu.

## Lista warstwy

`GET /api/v1/statistics/pyramid/climbs` przyjmuje te same filtry, `bucketKey` z raportu, `offset=0`, `limit=50` (maks. 100). Zwraca filtry, klucz, `totalCount`, offset/limit, nullable `nextOffset` i `items`: UUID wspinaczki, nazwa/miejsce, oryginalna skala/wycena lub punkty, kalibracja, data, styl i źródłowa sesja/wpis. Reprezentant odpowiada filtrom raportu. Poprawna pusta warstwa daje pustą stronę.

Odczyty używają bieżącego UUID, transakcji read-only REPEATABLE_READ i widoków V21/V22. Błędne filtry/klucze/paginacja → `400 INVALID_PYRAMID_REQUEST`. Osobne żądania HTTP mogą zobaczyć zmianę historii i wymagać odświeżenia.

## Zachowanie webu

`/statistics` domyślnie otwiera piramidę. URL przechowuje filtry, `asOf`, warstwę i offset. Zmiana filtra resetuje listę; kliknięcie/klawiatura otwierają warstwę. Powrót z sesji odtwarza wybór i ponownie pobiera raport. Refresh odświeża też otwartą listę.

Wykres używa liczności z API, bez własnego przeliczania osiągnięć. Mała próbka/fallback są oznaczone jako poziom prowizoryczny; COMBINED wyjaśnia brak wspólnej oceny. Obsłużone: loading, błąd/retry, brak osiągnięć, puste warstwy, telefon i fokus listy. Pełny ręczny odbiór wszystkich scenariuszy pozostaje w [priorytetach](../project-management/priorities.md).
