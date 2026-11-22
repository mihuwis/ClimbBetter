# Przykładowa historia treningów Mihu

Data: 2026-10-17. Status: dane zapisane do trwałej lokalnej bazy przez serwisy aplikacji; potwierdzone SQL i GET Dashboardu. Są to dane demonstracyjne, nie deklaracja rzeczywistych treningów użytkownika.

## Zakres i wyniki

Na polecenie użytkownika przygotowano 14 sesji, po dwie tygodniowo (wtorek/czwartek) od 2026-09-01 do 2026-10-15. Rozkład: 4 Garaż, 6 Bronx, 4 Dolina Bolechowicka. Wszystkie należą do Mihu (UUID kończące się 0002), a nazwy mają prefiks `[DEMO]`.

| Data       | Miejsce / sesja                  | Czas    | Ruchy |
| ---------- | -------------------------------- | ------- | ----- |
| 2026-09-01 | Garaż — obwody                   | 90 min  | 134   |
| 2026-09-03 | Bronx — 10 baldów                | 120 min | 142   |
| 2026-09-08 | Bronx — 12 baldów                | 120 min | 165   |
| 2026-09-10 | Bolechowicka — Filar Abazego I   | 180 min | 140   |
| 2026-09-15 | Bolechowicka — Filar Abazego II  | 180 min | 164   |
| 2026-09-17 | Garaż — obwody                   | 90 min  | 138   |
| 2026-09-22 | Bronx — 9 baldów                 | 120 min | 134   |
| 2026-09-24 | Bolechowicka — Filar Abazego III | 180 min | 140   |
| 2026-09-29 | Garaż — obwody                   | 90 min  | 142   |
| 2026-10-01 | Bronx — 11 baldów                | 120 min | 157   |
| 2026-10-06 | Bolechowicka — Filar Abazego IV  | 180 min | 164   |
| 2026-10-08 | Bronx — 8 baldów                 | 120 min | 133   |
| 2026-10-13 | Garaż — obwody                   | 90 min  | 144   |
| 2026-10-15 | Bronx — 13 baldów                | 120 min | 173   |

Łącznie zapisano 2070 wykonanych ruchów. SQL potwierdził siedem kolejnych tygodni z dwiema sesjami. `GET /api/v1/dashboard/sessions` zwrócił HTTP 200 i wszystkie 14 pozycji, od 2026-10-15 do 2026-09-01.

## Przyjęte dane demonstracyjne

- Garaż: 90 minut, trzy ukończenia zielonego obwodu po 38 ruchów oraz nieukończona próba fioletowego: kolejno 20, 24, 28 i 30 z 38 ruchów. Typ i profil CIRCUIT.
- Bronx: 120 minut, rozgrzewka WARMUP po 40 ruchów bez przypisania Climb, następnie 8–13 różnych baldów na sesję (w zamówionym przedziale 8–15). Każdy bald 6B jest ukończony w drugiej lub trzeciej próbie danej sesji; poprzedzające próby mają 5 i ewentualnie 7 wykonanych ruchów. Pełne przejście ma 8 ruchów. Przejścia po próbach i znane wcześniej wspinaczki używają RP; pierwsze kontakty bez historii mogą używać OS.
- Bolechowicka: 180 minut, najpierw łatwy Mały Wallisch (4a) lub Rysa Wallischa (5a), następnie trzy inne drogi nieprzekraczające francuskiego 6b+. Przyjęto profil MEDIUM_CONTINUOUS i LEAD jako założenia demonstracyjne, nie dane importowane z topo. Długości ruchowe pochodzą z sugestii katalogowych. Powtórzenia dróg mają RP.
- Czas lokalny: Garaż i Bronx 18:00, skały 10:00; strefa Europe/Warsaw. W JSON zapisano odpowiadające tym godzinom UTC.

Warunek „druga/trzecia próba” dotyczy prób w tej sesji. Globalny `attemptOrdinalAllTime` nadal uwzględnia całą wcześniejszą historię; nie zerowano go przy powrotach na ten sam bald. Sam fakt obecności Climb w katalogu nie jest kontaktem treningowym.

## Sposób zapisu

Dane wejściowe: [mihu-sample-sessions.json](../../infra/data/mihu-sample-sessions.json). Importer: [SeedMihuTrainingSessions.java](../../infra/tools/SeedMihuTrainingSessions.java).

Importer uruchamia kontekst local bez serwera HTTP, wymusza lokalne połączenie PostgreSQL i konto Mihu, a zapis wykonuje przez `TrainingSessionService`. Backend obliczył poziomy, znajomość, intensywność, oba loady i summary; nie wpisywano wyników ręcznym SQL. Wszystkie sesje importu są objęte jedną transakcją.

Pierwsze pięć sesji zapisano początkowo jako 12–16 października według zaproponowanych założeń. Po odpowiedzi użytkownika poprawiono je na 1, 3, 8, 10 i 15 września przez obsługę edycji z audytem, zachowując ich UUID i client ID, oraz dodano dziewięć kolejnych. Wykonano to chronologicznie, aby historia i obliczenia korzystały z nowej kolejności. Nie zostało dodatkowych pięć październikowych sesji.

Stałe `clientSessionId` pozwalają ponowić identyczny import bez duplikatów. Opcjonalne `--update-existing` jawnie edytuje wyłącznie istniejące rekordy z prefiksem DEMO, przez bieżącą wersję i audyt; nie jest potrzebne przy identycznym ponowieniu. Zmiana danych w JSON bez tej flagi daje konflikt zamiast nadpisania.

Weryfikacja SQL: 14 aktywnych sesji; 4/6/4 według miejsc; sześć rozgrzewek po 40 ruchów; brak grup 6B z liczbą prób inną niż 2–3 lub liczbą przejść inną niż jedno. Odczyt Dashboardu potwierdzony przez HTTP. Nie uruchamiano zestawu testów automatycznych.

Źródła: scenariusze użytkownika i doprecyzowanie częstotliwości z 2026-10-17; istniejący katalog; zapis przez serwisy aplikacji i odczyty SQL/HTTP.
