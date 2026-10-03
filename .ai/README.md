# ClimbBetter — mapa współpracy i dokumentacji

Data: 2026-10-03. Status: iteracja porządkowania **2 z 3 w toku**.

To punkt wejścia do wiedzy o projekcie. Opisujemy osobno ustalenia produktu, stan kodu i zamierzenia. Utworzenie dokumentu lub diagramu nie oznacza wdrożenia funkcji.

## Uzgodniona struktura

```text
AGENTS.md                         # krótki punkt wejścia do zasad współpracy
README.md                         # opis projektu, uruchomienie, link do dokumentacji

.ai/
├── README.md                     # mapa dokumentacji: gdzie szukać informacji
│
├── context/
│   ├── collaboration.md          # jak pracujemy i komunikujemy się
│   └── conventions.md            # nazewnictwo i zasady pracy z kodem
│
├── business/
│   ├── product.md                # cel aplikacji, odbiorcy, zakres MVP
│   ├── domain.md                 # pojęcia i reguły biznesowe
│   ├── grading-and-scoring.md    # wyceny wspinaczkowe i punktacja
│   └── training.md               # ocena treningów i założenia rozwoju
│
├── documentation/
│   ├── architecture.md           # ogólny obraz systemu i zależności
│   ├── backend.md
│   ├── frontend-web.md
│   ├── frontend-mobile.md
│   ├── database.md               # model danych, relacje, ograniczenia
│   ├── api.md                    # komunikacja i kontrakty
│   ├── development.md            # uruchomienie i weryfikacja projektu
│   ├── decisions.md              # decyzje techniczne i ich uzasadnienie
│   └── diagrams/                 # diagramy przepływu, ERD i potrzebne UML
│
├── project-management/
│   ├── priorities.md             # krótkie TO DO NOW / TO DO LATER
│   ├── roadmap.md                # przyrosty aplikacji i warunki ukończenia
│   ├── progress.md               # co działa, czego brakuje, prognoza
│   └── work-log.md               # historia prac i dane do wykresów
│
└── archive/                      # materiały historyczne
```

Diagramy w pierwszej iteracji: [komponenty](documentation/diagrams/components.md), [przepływ rejestrowania](documentation/diagrams/record-session-flow.md), [sekwencja zapisu](documentation/diagrams/save-session-sequence.md), [ERD](documentation/diagrams/data-model.md). Archiwum ma własny [indeks źródeł](archive/README.md) i manifest sum SHA-256.

## Od czego zacząć

| Potrzeba                                             | Dokument                                                                                                              |
| ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| Zasady naszej współpracy --------------------------- | [collaboration](context/collaboration.md), [conventions](context/conventions.md) ------------------------------------ |
| Po co budujemy aplikację i co obejmuje pierwszy etap | [product](business/product.md) -------------------------------------------------------------------------------------- |
| Sesja, próba, droga, miejsce, snapshot ------------- | [domain](business/domain.md) ---------------------------------------------------------------------------------------- |
| Dokładne tabele, wzory i przykłady ----------------- | [grading-and-scoring](business/grading-and-scoring.md) -------------------------------------------------------------- |
| Record session i przyszłe planowanie treningów ----- | [training](business/training.md) ------------------------------------------------------------------------------------ |
| Budowa systemu i zastosowanie wzorców -------------- | [architecture](documentation/architecture.md) ----------------------------------------------------------------------- |
| Java / React / Flutter ----------------------------- | [backend](documentation/backend.md), [web](documentation/frontend-web.md), [mobile](documentation/frontend-mobile.md) |
| Relacje danych / kontrakt / uruchomienie ----------- | [database](documentation/database.md), [api](documentation/api.md), [development](documentation/development.md) ----- |
| Co zostało ustalone, co nadal wymaga decyzji ------- | [decisions](documentation/decisions.md) ----------------------------------------------------------------------------- |
| Co robimy teraz ------------------------------------ | [priorities](project-management/priorities.md) ---------------------------------------------------------------------- |
| Kolejne przyrosty i trzy iteracje dokumentacji ----- | [roadmap](project-management/roadmap.md) ---------------------------------------------------------------------------- |
| Faktyczny postęp / czas / prognoza ----------------- | [progress](project-management/progress.md), [work-log](project-management/work-log.md) ------------------------------ |

## Jak czytać statusy i źródła

- **Ustalone**: wynika z wyraźnej decyzji użytkownika lub z zatwierdzonych źródeł. Nie oznacza implementacji.
- **Potwierdzone w kodzie**: znaleziono odpowiednią implementację lub konfigurację w przeglądzie z 2026-10-03. Uruchomienie i wynik testów podajemy osobno.
- **Plan / propozycja**: kierunek do realizacji lub omówienia, nie działająca funkcja.
- **Otwarte**: wymaga rozstrzygnięcia; identyfikator `OPEN-*` prowadzi do rejestru decyzji.
- **Historyczne**: źródło zachowane dla ciągłości, nie bieżące polecenie ani aktualna checklista.

Wyraźne bieżące ustalenia użytkownika mają pierwszeństwo. Wymagania web z 2026-09-22 zastępują wcześniejsze plany rozpoczynające integrację od Fluttera; nie zatwierdzają automatycznie akapitów oznaczonych w nich jako propozycje. Reguły wyceny po konsolidacji z 2026-08-15 zastępują wcześniejsze tabele z `old-doc`. Przy konflikcie nie zgadujemy: zapisujemy go w `decisions.md`.

## Zasada utrzymania

Wzory mają jedno źródło w `business/grading-and-scoring.md`, priorytety w `project-management/priorities.md`, a otwarte decyzje w `documentation/decisions.md`. Inne pliki odsyłają do nich. Status kodu aktualizujemy razem z dowodem; czas pracy zapisujemy tylko, gdy był mierzony lub podany przez użytkownika.

W iteracji 1 zachowano oryginalne `docs`, `.agents` i stare plany na miejscu oraz wykonano kopie w `archive`. DEC-019 ustala, że bieżąca `.ai` wraz z archiwum wchodzi do Git. Oryginały w `docs` pozostają poza Git do czasu usunięcia ich przez użytkownika; zachowane kopie nie są drugą aktywną dokumentacją.
