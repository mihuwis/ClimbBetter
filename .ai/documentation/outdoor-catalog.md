# Katalog skalny, długość i wyceny Kurtyki

Data: 2026-10-17. Status: Flyway zastosował `V15` na local i w PostgreSQL Testcontainers. Dane importu i publiczną widoczność potwierdzono SQL. Testy sektorów, metrów, ręcznego nadpisania ruchów i mapowania wycen przeszły w pełnym zestawie 51 testów. [Raport weryfikacji](backend-verification.md).

## Zapisany rejon

Publiczne Area `Dolina Bolechowicka`: `areaType = ROCK`, `countryCode = PL`, `region = Jura Krakowsko-Częstochowska`. Filar Abazego jest `Sector` w tym Area, a nie osobnym rejonem. Ma 28 publicznych Climb typu `ROUTE`, zgodnych z tabelą przekazaną przez użytkownika. Nie weryfikowano ponownie bieżących opisów dróg na portalu.

27 dróg ma oryginalną wycenę i kanoniczny odpowiednik francuski. Pozycja `projekt` zachowuje brak wyceny, długość 20 m i sugestię 40 ruchów; nie podstawiamy jej najniższej wyceny. Wszystkie pozycje mają `isPublic = true`, `ownerId = null`, `isVerified = false`. Zapytania katalogowe potwierdziły dostęp do 28 pozycji dla Mihu i Local Developer.

Skrypt importu: [bolechowicka-public-catalog.sql](../../infra/sql/bolechowicka-public-catalog.sql). To dane lokalnej bazy, nie migracja zasilająca każde środowisko. Identyfikatory są stałe, oparte na identyfikatorach pozycji w tabeli użytkownika; ponowienie nie nadpisuje edytowanych danych. Wykonanie wymaga `V15`; nie należy ręcznie wykonywać DDL poza Flyway.

## Długość i ruchy

Każdy Climb może mieć dodatnie `lengthMeters` z dokładnością do dwóch miejsc po przecinku. Przy tworzeniu pozycji katalogowej kolejność wyznaczania `suggestedMoveCount` jest następująca:

1. Jawna liczba ruchów użytkownika ma pierwszeństwo.
2. Jeżeli podano metry, domyślna sugestia to `ceil(lengthMeters × 2)`; dla długości całkowitych dokładnie 2 ruchy na metr.
3. Bez metrów BOULDER zachowuje dotychczasową sugestię 8 ruchów. ROUTE/CIRCUIT bez obu danych nie ma sugestii.

Przykłady: 15 m → 30 ruchów, 22 m → 44, 23 m → 46, 24 m → 48, 25 m → 50. Metry pozostają zapisane także po ręcznym podaniu innej liczby ruchów. Liczba ruchów z metrów to sugestia, nie fakt wykonania, EDL ani profil wysiłku. Dla niecałkowitego wyniku zaokrąglamy w górę do pełnego ruchu.

Długość nie wyznacza charakteru trudności. Import nie dopisuje profilu wysiłku, którego nie było w tabeli. Przy zapisie sesji klient nadal podaje właściwy `effortProfileCode`, `totalMoves`, `executedMoves`, rezultat i styl. `totalMoves` może zacząć od sugestii katalogowej i zostać poprawione, a `executedMoves` opisuje rzeczywisty wysiłek.

## Mapowanie wycen

Obliczenia nadal używają wyłącznie `ROUTE_FRENCH` i `BOULDER_FONT` (DEC-030). `catalog.grade_aliases` przechowuje oryginalny system, etykietę, kanoniczne ID, wersję i źródło. Aktualne mapowanie `KURTYKA` ma wersję `1`; importowany Climb zachowuje `inputGradeLabel`, `gradeSystemCode` i `gradeMappingVersion` obok kanonicznego `grade_id`.

Źródła mapowania:

- [Regulamin rankingu PZA](https://www.ranking.pza.org.pl/info/regulamin): zakres od `VI.3`, w tym `VI.5 → 7c+`, `VI.5+ → 8a`, `VI.5+/6 → 8a+`.
- [Tabela UKA](https://topo.uka.pl/content/topo/skala.html): niższe wyceny, m.in. `V- → 5a`, `V → 5b`, `VI → 6a`, `VI.2+ → 6c+`. Dla `IV` tabela obejmuje `4a–4b`; wersja 1 wybiera niższe `4a` jako jawne przybliżenie.

Mapowania są przybliżeniem między systemami, nie nową skalą punktów. Oryginalnych etykiet nie zastępujemy francuskimi. Projekty bez wyceny mogą istnieć w katalogu, lecz oceniany wpis treningowy nadal wymaga poprawnej wyceny. Przy wysyłaniu sesji obecny kontrakt używa kanonicznego `gradeLabel` zwróconego przez katalog; `inputGradeLabel` służy prezentacji oryginału. Przyjmowanie aliasów bezpośrednio w DTO wpisu i snapshoty oryginalnego wejścia sesji pozostają dalszym rozwinięciem DEC-030.

## API i Swagger

- `GET /api/v1/catalog/areas?q=Bolechowicka` — rejon wraz z `countryCode` i `region`.
- `GET /api/v1/catalog/areas/{areaId}/sectors` — skały/sektory dostępnego rejonu.
- `GET /api/v1/catalog/areas/{areaId}/climbs?sectorId={sectorId}&limit=50` — wszystkie 28 pozycji Filaru Abazego.

Climb zwraca dodatkowo `lengthMeters`, `sectorId`, `sectorName`, `gradeSystemCode`, `inputGradeLabel`, `gradeMappingVersion`. `gradeLabel` i `gradeScaleCode` pozostają kanonicznymi danymi do obliczeń; dla projektu są `null`.

Samodzielny POST prywatnej drogi obsługuje dodatkowe pola, np.:

```json
{
  "clientClimbId": "22222222-2222-4222-8222-222222222222",
  "name": "Moja droga",
  "climbType": "ROUTE",
  "gradeSystemCode": "KURTYKA",
  "gradeLabel": "VI.2+",
  "lengthMeters": 15,
  "suggestedMoveCount": null,
  "effortProfileCode": null,
  "sectorId": null
}
```

W ścieżce POST wskazujemy rzeczywiste `areaId`, a `sectorId` może wskazywać istniejący sektor tego Area. Wynikiem przykładu jest kanoniczne `6c+`, oryginalne `VI.2+` oraz 30 sugerowanych ruchów. Jawne `suggestedMoveCount`, np. 35, nadpisuje sugestię z metrów. Podanie sektora innego Area jest odrzucane. Nowy Climb z POST pozostaje prywatny; publiczność danych importowanych nadano bezpośrednio w zamówionym imporcie.

Sektory dziedziczą dostęp z Area. Archiwalne sektory są pomijane przez listy katalogowe; lista Climb nadal wymaga dostępu do Area i widoczności każdej wspinaczki. Brak sektora nie blokuje dawnych baldów/obwodów.

Przed uruchomieniem nowego kodu trzeba zrestartować backend; aktualizacja schematu nie przeładowuje już uruchomionych klas Javy. Testy do wykonania przez użytkownika: `ClimbMoveSuggestionTest` i `CatalogClimbScenariosIntegrationTest`.
