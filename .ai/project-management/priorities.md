# Bieżące priorytety

Data aktualizacji: 2026-10-17. To krótka lista zakresu, nie zestaw poleceń do wykonania jednocześnie.

Aktualny zakres użytkownika: ocena wdrożonych punktów React 1–5 — Dashboard, Details z edycją/usuwaniem, Calendar, ręczne dodanie i Start now. Asystent otrzymał zgodę na implementację; UI pozostaje angielski. Build/lint, 4 testy modelu, 6 scenariuszy przeglądarkowych i pełny CRUD przez realny backend przeszły; backend ma 52 zielone testy. Następny krok: ocena użytkownika i poprawki wygody interfejsu. Rozbudowa analityki, recent bests i ochrona równoległego pierwszego retry są odłożone. [Raport React](../documentation/frontend-web.md), [kontrakt API](../documentation/backend-crud-handoff.md).

## TO DO NOW

Katalog skalny: V15 potwierdzona również na pustym PostgreSQL Testcontainers; testy sektorów, metrów i aliasów wycen przeszły w pełnym zestawie. Publiczne 28 pozycji Filaru Abazego pozostaje w local. [Raport](../documentation/outdoor-catalog.md).

Scenariusze A/B są potwierdzone przez zielony `CatalogClimbScenariosIntegrationTest`: nowe i znane Climb w sesji oraz prywatne Climb zapisane przed treningiem. [Kontrakt i przykład Swagger](../documentation/catalog-climb-scenarios.md).

- [ ] Dokończyć porządkowanie dokumentacji w trzech iteracjach: pierwsza zweryfikowana, druga w toku po zapisaniu DEC-018–022; pozostałe tematy są w [rejestrze decyzji](../documentation/decisions.md).
- [x] Podłączyć Dashboard, Details, edycję/usuwanie, kalendarz i oba formularze sesji do API; potwierdzić trwały lokalny szkic i rzeczywisty zapis/odczyt przez UI.
- [x] Dashboard z Adjusted Load i najwyższymi ukończonymi wycenami per skala; Details grupowane po wspinaczce z zachowaniem chronologii, ruchów i obu loadów.
- [ ] Zebrać uwagi użytkownika do pięciu ekranów i poprawić ergonomię. Szczegóły testów i ograniczenia: [frontend-web](../documentation/frontend-web.md).

Ustalone: jedna sesja obejmuje jedno Area (DEC-018); `.ai` jest wersjonowane w Git (DEC-019); asystent może czytać i wykonywać uzgodnione testy, lecz nie zmienia kodu bez zatwierdzenia (DEC-020); lokalny odzyskiwalny szkic jest wymagany (DEC-021); pierwszy Dashboard używa tekstowych metryk bez dekoracyjnego wykresu (DEC-022).

Pierwszy backendowy pion zapisu i odczytu jest potwierdzony: `POST /api/v1/training/sessions → PostgreSQL → Dashboard/Personal Bests/Details`. Test zapisuje sesję Bronx, odczytuje podsumowanie, średnią intensywność, wyniki życiowe i trzy chronologiczne wpisy. Potwierdzono również atomowy rollback, bezpieczne identyczne ponowienie, konflikt `409` dla zmienionego żądania i `404` Details.

Wcześniejsza kolejność z 2026-10-11 (zastąpiona powyższym zakresem z 2026-10-17):

1. następny dzień rozpocząć od Docker Desktop, `docker version` i celowanego `./mvnw -Dtest=TrainingSessionFlowIntegrationTest test`;
2. domknąć weryfikację `ASCENT_MODE_HISTORY_CONFLICT`: 2026-10-17 użytkownik potwierdził test OS, rollback i ponowienie z jawnym potwierdzeniem oraz flagami audytu; pozostają asercja mnożnika `1.50` i wariant Flash;
3. dodać małe endpointy wyboru istniejących Area i Climb, zachowując możliwość szybkiego utworzenia nowych w POST sesji;
4. uzgodnić i rozpocząć edycję całej sesji z ponownym obliczeniem oraz audytem; edycja pojedynczego wpisu w UI korzysta z tego samego kontraktu agregatu;
5. domknąć ochronę równoległego retry i scenariusz sesji zawierającej wyłącznie próby;
6. rozszerzyć personal bests o najlepsze wyniki z ruchomego okna ostatnich 12 miesięcy.

Ustalono: recent personal best używa 12 miesięcy; Area i Climb zachowują UUID, a nazwa służy do wyszukiwania; edycja nie przelicza automatycznie późniejszych sesji; konflikt OS/Flash używa `409 ASCENT_MODE_HISTORY_CONFLICT`. Przed katalogiem pozostaje ustalenie minimalnego kształtu wyników wyszukiwania oraz obsługi prywatnych i publicznych pozycji.

## TO DO LATER

- Pozostałe intencje dodawania sesji i edycja z audytem historii.
- Podstawowa analityka; rozbudowa już podłączonych Calendar i Areas według uwag użytkownika.
- Integracja Fluttera, trwałość lokalna i synchronizacja offline.
- Planowanie, cykle i generowanie propozycji treningowych.
- Rozszerzenie katalogu, oficjalne pakiety i zdjęcia.
- Logowanie produkcyjne, wdrożenie i utrzymanie przed publicznym udostępnieniem.
- Społeczność i zaawansowana analityka po ustabilizowaniu rdzenia.

Warunki ukończenia przyrostów: [roadmap](roadmap.md). Faktyczny stan: [progress](progress.md).



## Start dnia pracy 

Dzisiaj pracujemy nad backendem Java, mam około 5 godziny. Stwr zplan działań na dzisiaj w oparciu o poprzednie ustalenia.
 Przeczytaj AGENTS.md, .ai\documentation\decisions.md, priorytety, postęp i potrzebne dokumenty oraz sprawdź obecny kod. Przypomnij mi o uruchomieniu Docker Desktop i sprawdzeniu `docker version` przed testami integracyjnymi: Testcontainers uruchamia PostgreSQL w kontenerze. Ja koduję i uruchamiam testy. Ty wyjaśniasz, robisz review i aktualizujesz dokumentację po potwierdzonych zmianach. Prowadź mnie po jednym małym kroku. Pamiętaj, że uczę się Javy i Reacta i nie znam jeszcze wszystkich możliwości tych języków. Pomagaj.


## Koniec dnia pracy

Sesja z 2026-10-10 zakończona po około 5 godzinach łącznej pracy. Potwierdzono model obliczeń i agregacji, rozgrzewkę, `V5` oraz integracyjny odczyt po migracji. Następna sesja zaczyna się od brakującego `TrainingEntryCommand`, a celem jest zapis scenariusza Bronx i odczyt jego podsumowania na Dashboardzie. Szczegóły oraz granice potwierdzenia zapisano w [work-log](work-log.md).

Sesja z 2026-10-11 zakończona po około 7 godzinach. Ukończono backendowy zapis scenariusza Bronx, migracje do `V12`, rollback i idempotencję z fingerprintem, średnią intensywność, personal bests oraz Details. Pełny zestaw testów przeszedł. Następny dzień zaczyna się od konfliktu OS/Flash, a następnie przechodzi do katalogu Area/Climb potrzebnego formularzowi webowemu. Szczegóły zapisano w [work-log](work-log.md).
