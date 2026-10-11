# Bieżące priorytety

Data aktualizacji: 2026-10-11. To krótka lista zakresu, nie zestaw poleceń do wykonania jednocześnie.

## TO DO NOW

- [ ] Dokończyć porządkowanie dokumentacji w trzech iteracjach: pierwsza zweryfikowana, druga w toku po zapisaniu DEC-018–022; pozostałe tematy są w [rejestrze decyzji](../documentation/decisions.md).
- [ ] **Odtworzyć backend Java dla pierwszego użytecznego przepływu web**, uwzględniając najnowsze ustalenia Dashboardu i Details: ukończona sesja z uporządkowanymi wpisami, obliczenia/snapshoty, zapis i odczyt. Start sesji pozostaje lokalny.
- [ ] Doprecyzować mały kontrakt tego przepływu: Dashboard z Adjusted Load i najwyższymi ukończonymi wycenami per skala; Details grupowane po wspinaczce z zachowaniem chronologii, ruchów i obu loadów.

Ustalone: jedna sesja obejmuje jedno Area (DEC-018); `.ai` jest wersjonowane w Git (DEC-019); asystent może czytać i wykonywać uzgodnione testy, lecz nie zmienia kodu bez zatwierdzenia (DEC-020); lokalny odzyskiwalny szkic jest wymagany (DEC-021); pierwszy Dashboard używa tekstowych metryk bez dekoracyjnego wykresu (DEC-022).

Pierwszy pion zapisu i odczytu jest potwierdzony: `POST /api/v1/training/sessions → PostgreSQL → GET /api/v1/dashboard/sessions`. Test zapisuje sesję Bronx z rozgrzewką i dwoma przejściami oraz odczytuje summary. Potwierdzono również atomowy rollback, bezpieczne identyczne ponowienie oraz konflikt `409` dla zmienionego żądania z tym samym `clientSessionId`. Następny krok to rozszerzyć Dashboard o zapisaną średnią intensywność, a następnie o uzgodnione rekordy i szczegóły sesji.

Najbliższa kolejność:

1. dodać odczyt szczegółów jednej sesji z wpisami w kolejności;
2. osobno domknąć konflikt historii OS/Flash i ochronę przed równoległymi żądaniami zapisu;
3. po ustaleniu długości okna rozszerzyć personal bests o najlepsze wyniki ostatniego okresu.

## TO DO LATER

- Pozostałe intencje dodawania sesji i edycja z audytem historii.
- Calendar, Areas i podstawowa analityka na realnych danych.
- Integracja Fluttera, trwałość lokalna i synchronizacja offline.
- Planowanie, cykle i generowanie propozycji treningowych.
- Rozszerzenie katalogu, oficjalne pakiety i zdjęcia.
- Logowanie produkcyjne, wdrożenie i utrzymanie przed publicznym udostępnieniem.
- Społeczność i zaawansowana analityka po ustabilizowaniu rdzenia.

Warunki ukończenia przyrostów: [roadmap](roadmap.md). Faktyczny stan: [progress](progress.md).



## Start dnia pracy 

Dzisiaj pracujemy nad backendem Java, mam około 4-5 godziny. przypomina wczorajszą listę :
Dodanie brakującego TrainingEntryCommand.
Czysta kompilacja.
Minimalne resolvery wyceny, profilu, historii i poziomu dla scenariusza Bronx.
Kontroler, serwis transakcyjny i repozytorium POST.
Test POST → PostgreSQL → GET Dashboard.
Rozszerzenie Dashboardu o intensywność i uzgodnione rekordy.
 Przeczytaj AGENTS.md, .ai\documentation\decisions.md, priorytety, postęp i potrzebne dokumenty oraz sprawdź obecny kod. Przypomnij mi o uruchomieniu Docker Desktop i sprawdzeniu `docker version` przed testami integracyjnymi: Testcontainers uruchamia PostgreSQL w kontenerze. Ja koduję i uruchamiam testy. Ty wyjaśniasz, robisz review i aktualizujesz dokumentację po potwierdzonych zmianach. Prowadź mnie po jednym małym kroku. Pamiętaj, że uczę się Javy i Reacta i nie znam jeszcze wszystkich możliwości tych języków. Pomagaj.


## Koniec dnia pracy

Sesja z 2026-10-10 zakończona po około 5 godzinach łącznej pracy. Potwierdzono model obliczeń i agregacji, rozgrzewkę, `V5` oraz integracyjny odczyt po migracji. Następna sesja zaczyna się od brakującego `TrainingEntryCommand`, a celem jest zapis scenariusza Bronx i odczyt jego podsumowania na Dashboardzie. Szczegóły oraz granice potwierdzenia zapisano w [work-log](work-log.md).
