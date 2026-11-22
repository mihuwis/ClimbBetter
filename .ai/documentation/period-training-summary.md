# Training summary — miesiąc i rok

Aktualizacja: 2026-11-09. Backend i web wdrożone 2026-11-07; DEC-048. Źródła: `PeriodSummaryRequest`, `PeriodTrainingSummary`, serwis/repository reporting oraz `features/statistics`. [Wyniki testów](backend-verification.md).

## Okres i zasady agregacji

Raport Statistics `?report=summary` pokazuje miesiąc kalendarzowy (dni) albo rok (miesiące). Filtry: All, Boulders, Routes & circuits. Zakres po zapisanej `sessionDate`; sesja przez północ w całości należy do swojego dnia. Wszystkie aktywne sesje użytkownika, bez limitu Dashboardu i bez przeliczania dawnych loadów.

Po filtrze dyscypliny ruchy i load obejmują pasujące wpisy, lecz czas — **pełny czas sesji zawierających tę dyscyplinę**. Sesja mieszana wchodzi do obu filtrów, a do All raz; czas i liczba sesji filtrów nie sumują się do All. Najpierw agregujemy wpisy do sesji, aby JOIN nie powielał czasu. Rozgrzewka wnosi ruchy tylko do All, load 0 i nie ma dyscypliny.

Koło stylów: jedno `climbId` raz w całym okresie, najlepszy udany styl **OS > Flash > RP**. Najpierw okres/dyscyplina i kwalifikacja osiągnięć, potem deduplikacja. Wędka, próby i rozgrzewki nie tworzą elementu. OS potem RP w okresie → jedno OS; OS poza okresem i RP w okresie → jedno RP; nieudana próba OS potem udane RP → jedno RP. Sektory są rozłączne. Roczne koło nie jest sumą miesięcznych; piramida RP nadal ma własną definicję.

## Metryki i interfejs

- Sześć kafli: sesje, łączny/średni czas, ruchy, suma/średnia sesyjna Adjusted Load, Classic Load, aktywne dni. Dodatkowo liczba różnych ukończonych wspinaczek.
- Słupki: sesje, czas albo ruchy. Do 3 linii: suma, maksimum sesji lub średnia sesyjna loadu, osobno Classic/Adjusted. Domyślnie ruchy + suma Adjusted Load; osobne osie jednostek. Zmiana serii nie pobiera ponownie raportu.
- Średnia = suma loadu / liczba pasujących sesji. Maksimum dotyczy sumy wpisów sesji. Puste koszyki mają sumy/liczby 0, średnią/maksimum NULL; backend zwraca wszystkie dni/miesiące.
- Pierścień dyscyplin używa ocenianych ruchów, z rozgrzewką osobno poza mianownikiem. Pierścień stylów stosuje regułę powyżej.
- Wyróżnienia: największe loady sesji, najtrudniejszy bald i droga/obwód, najwyższe OS/Flash/RP dla obu skal, linki do sesji. Ranking z zapisanej wyceny, bez wędki.

Okres, dyscyplina, serie wykresu i dzień są w URL. Kliknięcie miesiąca otwiera miesiąc, dnia — sesje; tabela daje te same akcje z klawiatury. Powrót z sesji odtwarza raport i odczytuje aktualne dane. Obsługa telefonu, loading/error/empty. Recharts ładowany osobno, bez obciążenia początkowego Dashboardu.

## Kontrakt

```http
GET /api/v1/statistics/summary?period=MONTH&year=2026&month=11&discipline=ALL
```

`period=MONTH` i `discipline=ALL` są domyślne. `year` wymagany 1–9998. `month` 1–12 wymagany dla MONTH, niedozwolony dla YEAR. Dyscypliny API: `ALL`, `BOULDER`, `ROUTE_AND_CIRCUIT`. Błędny request → `400 INVALID_PERIOD_SUMMARY_REQUEST`.

Odpowiedź: `filters`, włączne `windowStart/windowEnd`, `summary`, `uniqueClimbCount`, `warmupMoves`, kompletne `buckets` z lekkimi listami sesji, `disciplineBreakdown`, `styleBreakdown`, `highlights`. Odczyt REPEATABLE_READ, istniejące snapshoty i `reporting.achievement_ascents`; bez migracji, replay i nowych pól formularza. Okres sezonowy wymaga osobnej definicji.
