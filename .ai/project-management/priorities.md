# Bieżące priorytety

Data aktualizacji: 2026-10-04. To krótka lista zakresu, nie zestaw poleceń do wykonania jednocześnie.

## TO DO NOW

- [ ] Dokończyć porządkowanie dokumentacji w trzech iteracjach: pierwsza zweryfikowana, druga w toku po zapisaniu DEC-018–022; pozostałe tematy są w [rejestrze decyzji](../documentation/decisions.md).
- [ ] **Odtworzyć backend Java dla pierwszego użytecznego przepływu web**, uwzględniając najnowsze ustalenia Dashboardu i Details: ukończona sesja z uporządkowanymi wpisami, obliczenia/snapshoty, zapis i odczyt. Start sesji pozostaje lokalny.
- [ ] Doprecyzować mały kontrakt tego przepływu: Dashboard z Adjusted Load i najwyższymi ukończonymi wycenami per skala; Details grupowane po wspinaczce z zachowaniem chronologii, ruchów i obu loadów.

Ustalone: jedna sesja obejmuje jedno Area (DEC-018); `.ai` jest wersjonowane w Git (DEC-019); asystent może czytać i wykonywać uzgodnione testy, lecz nie zmienia kodu bez zatwierdzenia (DEC-020); lokalny odzyskiwalny szkic jest wymagany (DEC-021); pierwszy Dashboard używa tekstowych metryk bez dekoracyjnego wykresu (DEC-022).

Najbliższy konkretny krok: na początku następnej sesji użytkownik uruchamia pełny zestaw testów backendu poleceniem `./mvnw test`. Wynik pokaże, czy nowy `FamiliarityBand` nie naruszył istniejącego bootstrapu, testu UTC i granic modułów. Dopiero po tym wybieramy kolejny mały typ domenowy. Preview oraz polityka wielu kart pozostają otwartą częścią `OPEN-04`. Kod pisze użytkownik.

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

Dzisiaj pracujemy nad backendem Java, mam około 2 godziny. Przeczytaj AGENTS.md, .ai\documentation\decisions.md, priorytety, postęp i potrzebne dokumenty oraz sprawdź obecny kod. Przypomnij mi o uruchomieniu Docker Desktop i sprawdzeniu `docker version` przed testami integracyjnymi: Testcontainers uruchamia PostgreSQL w kontenerze. Ja koduję i uruchamiam testy. Ty wyjaśniasz, robisz review i aktualizujesz dokumentację po potwierdzonych zmianach. Prowadź mnie po jednym małym kroku. Pamiętaj, że uczę się Javy i Reacta i nie znam jeszcze wszystkich możliwości tych języków. Pomagaj.


## Koniec dnia pracy

Sesja z 2026-10-03 zakończona po 4 godzinach pracy. Duża część czasu objęła utworzenie ramy współpracy i dokumentacji `.ai`; szczegóły oraz granice potwierdzenia zapisano w [work-log](work-log.md).
