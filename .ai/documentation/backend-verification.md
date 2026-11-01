# Weryfikacja backendu przed integracją React

Data: 2026-10-17. Status: potwierdzony pełny zielony zestaw testów backendu. Źródła: wynik Maven, raporty Surefire w `backend/target/surefire-reports` i log `backend/target/backend-test-run.log`.

## Wynik

Aktualizacja po integracji React: pełny Maven `test` ponowiono z sukcesem — **52 testy, 0 failures/errors/skipped**, 31,023 s. Log: `backend/target/frontend-integration-verification.log`. Dodatkowy test sprawdza kalendarz z 22 widocznymi sesjami, wyklucza cudzą/usuniętą, sprawdza zakres dat i bieżącą tożsamość. React potwierdzono również pełnym przebiegiem przeglądarkowym na trwałej bazie Mihu; [raport](frontend-web.md). Poniższe 51 testów dokumentuje wcześniejszą bramkę przed React.

Na polecenie użytkownika asystent sprawdził Docker (`docker version` zawierał Client i Server), a następnie wykonał pełne Maven `test`. Wynik: **51 testów, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. Czas raportowany przez Maven: 24,362 s. Nie były potrzebne poprawki kodu ani testów.

Testcontainers uruchomił tymczasowy PostgreSQL `17.6-alpine`. Flyway zbudował bazy testowe do `V15`. Testy nie korzystały z trwałej bazy Compose z danymi Mihu.

## Potwierdzony zakres

- `TrainingSessionCrudIntegrationTest` (2): create → read → update → delete, przeliczenie loadu, wersja i audyt, konflikt starej wersji, rollback błędnej edycji, wykluczenie usuniętej sesji z odczytów, kontrola właściciela i prywatność katalogu.
- `TrainingSessionFlowIntegrationTest` (6): zapis sesji, Dashboard, personal bests i Details, rollback całego zapisu, identyczne retry, konflikt zmienionej treści, OS i Flash z jawnym potwierdzeniem oraz zachowaniem mnożnika 1.50 i flag audytu.
- `CatalogClimbScenariosIntegrationTest` (4): nowe i wcześniej znane wspinaczki w treningu, zapamiętywanie Climb przed treningiem bez tworzenia kontaktów, retry katalogu i konflikt klientowego ID, dostęp, niezależność identycznych nazw, sektory, metry, ręczna liczba ruchów, mapowanie Kurtyki i projekt bez wyceny.
- `ClimbMoveSuggestionTest` (2): 2 ruchy/m, pierwszeństwo ręcznej wartości, domyślne 8 ruchów balda, zaokrąglenie i odrzucenie niepoprawnej długości.
- Pozostałe testy: kalkulatory i reguły domenowe, kontekst Spring i zegar UTC, odczyt Dashboardu, kontroler Dashboardu oraz granice modułów Modulith.

Są to wyniki istniejących scenariuszy, nie deklaracja kompletności wszystkich możliwych zachowań API. Rozszerzenia wcześniej odłożone, takie jak równoległy pierwszy POST sesji, recent personal bests i produkcyjne logowanie, zachowują swój zakres.

## Następny krok

Przechodzimy do React: najpierw Dashboard na prawdziwych danych Mihu zamiast mocków, następnie Details, formularz sesji z odzyskiwalnym szkicem oraz edycja/usuwanie. Dalsze uzupełnienia API wynikają z potrzeb konkretnego ekranu.

Polecenie do ponowienia pełnego zestawu w Bash z katalogu `backend`:

```bash
./mvnw test
```

Odczyt danych lokalnych, Swaggera i 14 sesji DEMO jest opisany osobno w [uruchomieniu](development.md) i [historii Mihu](mihu-demo-history.md).
