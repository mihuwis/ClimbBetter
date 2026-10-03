# ClimbBetter API v2 - kanban

Robocza checklista dla iteracji API/DB. Po wykonaniu zadania zmieniamy `[ ]` na `[x]`.

Zasada pracy: kod pisze Michał, Codex robi review, wskazuje ryzyka i sugeruje poprawki. Zmiany w kodzie powstają tylko po wyraźnym poleceniu.

## Aktualny stan

- [x] Decyzje domenowe z [api_db_plan_iteracji.md](api_db_plan_iteracji.md) zostały przeczytane i zaakceptowane.
- [x] Ustalono finalne nazwy głównych encji: `Area`, `Grade`, `EntryStyle`, `EffortProfile`, `RelativeEffortBand`, `UserGradeReference`.
- [x] Ustalono, że sesja nie ma `primary_discipline`; charakter sesji wynika z wpisów, objętości, intensywności i loadu.
- [x] Ustalono skalę startową `Grade`: francuska od `4a` do `9c`, z wielką literą dla boulderów w UI, np. `7B`.
- [x] Ustalono, że `UserGradeReference` jest aktywnym punktem odniesienia dla `RelativeEffortBand`.
- [ ] Zweryfikować branch lokalnie po stronie użytkownika. Codex nie potwierdził brancha, bo `git branch --show-current` został zablokowany przez `dubious ownership` w sandboxie.

## Etap 1 - fundament DB

### Do zrobienia

- [x] Przeczytać aktualne decyzje domenowe w [api_db_plan_iteracji.md](api_db_plan_iteracji.md).
- [ ] Sprawdzić aktualny stan brancha i lokalnych zmian przed rozpoczęciem pracy.
- [x] Ustalić finalne nazwy encji domenowych: `Area`, `Grade`, `EntryStyle`, `EffortProfile`, `RelativeEffortBand`, `UserGradeReference`.
- [ ] Dodać minimalnego użytkownika developerskiego ze stałym `UserId`.
- [ ] Przygotować model użytkownika tak, żeby później dało się go spiąć z .NET Identity i OAuth.
- [ ] Przemodelować `TrainingSession`: `SessionDate`, `TimeZoneId`, `DurationMinutes = 60`, opcjonalne `StartedAtUtc` i `EndedAtUtc`.
- [ ] Przemodelować miejsca treningu na hierarchiczne `Area` dla skał i sztucznych ścian.
- [ ] Dodać obsługę breadcrumbów miejsca, np. `Europa -> Polska -> Jura -> Dolina Bolechowicka -> Filar Abazego`.
- [ ] Przemodelować `Climb` pod stabilną identyfikację drogi/balda.
- [ ] Dodać `ClientClimbId` i status rozwiązania drogi dla scenariuszy offline.
- [ ] Przemodelować `Difficulty` na `Grade`.
- [ ] Przemodelować `Quality` na `EntryStyle`.
- [ ] Dodać `EffortProfile`.
- [ ] Dodać `RelativeEffortBand` zamiast sztywnego clampowania różnicy wycen.
- [ ] Dodać `UserGradeReference` jako aktywny punkt odniesienia poziomu użytkownika.
- [ ] Zaktualizować konfiguracje EF Core dla nowych encji.
- [ ] Dodać indeksy i podstawowe constraints dla sesji, wpisów, miejsc, dróg i słowników.
- [ ] Dodać seedy słowników z deterministycznymi UUID.
- [ ] Dodać seed dev-usera.
- [ ] Dodać pierwsze testowe miejsca i drogi: Bronx oraz przykład skał z Doliny Bolechowickiej.
- [ ] Przygotować migrację PostgreSQL.
- [ ] Uruchomić migrację na lokalnej bazie.
- [ ] Sprawdzić, że aplikacja startuje z lokalnym PostgreSQL.
- [ ] Sprawdzić, że seedowane dane są widoczne w bazie.
- [ ] Uruchomić build backendu.
- [ ] Uruchomić istniejące testy backendu, jeśli są dostępne.
- [ ] Dać zmiany do review Codexowi.
- [ ] Wprowadzić poprawki po review.
- [ ] Uruchomić build/testy po poprawkach.
- [ ] Zrobić commit z Etapu 1.
- [ ] Wypchnąć branch do repo.

## Etap 2 - obliczenia i zapis sesji

### Do zrobienia

- [ ] Przeczytać wzory i snapshoty z [climbbetter_wymagania_api_baza.md](climbbetter_wymagania_api_baza.md).
- [ ] Wydzielić serwis kalkulacji, np. `TrainingLoadCalculator`.
- [ ] Zaimplementować `LengthDivisor = BaseEdl + 0.2 * TotalMoves`.
- [ ] Zaimplementować `MoveIntensity`.
- [ ] Zaimplementować `ClassicPoints`.
- [ ] Zaimplementować `RelativeGradeDiff`.
- [ ] Zaimplementować wybór `RelativeEffortBand`.
- [ ] Zaimplementować `AdjustedLoad`.
- [ ] Obsłużyć rozgrzewkę: ruchy liczą się do wolumenu, load i intensywność nie.
- [ ] Dodać snapshoty obliczeń do `TrainingEntry`.
- [ ] Dodać testy jednostkowe kalkulatora dla balda, drogi, próby częściowej, rozgrzewki i skrajnych różnic wyceny.
- [ ] Zmienić `CreateTrainingSession` na nowy kontrakt.
- [ ] Ustawić domyślne `DurationMinutes = 60`, jeśli request nie poda czasu trwania.
- [ ] Zmienić `AddTrainingEntry` na nowy kontrakt.
- [ ] Obsłużyć wpis z istniejącym `ClimbId`.
- [ ] Obsłużyć wpis offline z `ClientClimbId`.
- [ ] Obsłużyć wpis bez znanej drogi przez utworzenie prywatnej, niezweryfikowanej drogi użytkownika.
- [ ] Po dodaniu wpisu przeliczać cache sesji.
- [ ] Dodać `UpdateTrainingEntry`.
- [ ] Dodać `DeleteTrainingEntry`.
- [ ] Po edycji i usunięciu wpisu przeliczać cache sesji.
- [ ] Dodać jawny mechanizm reewaluacji historii po korekcie wyceny drogi.
- [ ] Dodać zapis audytu reewaluacji w `entry_calculation_revisions`.
- [ ] Dodać testy integracyjne dla: create session, add entry, update entry, delete entry, recalculation.
- [ ] Dodać test integracyjny dla wpisu offline tworzącego prywatną drogę.
- [ ] Dodać test integracyjny dla jawnej reewaluacji historii.
- [ ] Sprawdzić odpowiedzi API w Scalar/OpenAPI.
- [ ] Uruchomić build backendu.
- [ ] Uruchomić testy backendu.
- [ ] Dać zmiany do review Codexowi.
- [ ] Wprowadzić poprawki po review.
- [ ] Uruchomić build/testy po poprawkach.
- [ ] Zrobić commit z Etapu 2.
- [ ] Wypchnąć branch do repo.

## Poza zakresem tej checklisty

- [ ] Etap 3: endpointy pod frontend-web.
- [ ] Etap 4: integracja frontend-web.
- [ ] Etap 5: dalsze utwardzenie, metryki i synchronizacja mobile.
