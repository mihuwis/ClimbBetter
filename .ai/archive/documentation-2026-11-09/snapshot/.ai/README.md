# ClimbBetter — mapa współpracy i dokumentacji

Data aktualizacji: 2026-11-08. Status: iteracja porządkowania **2 z 3 w toku**.

Zakończony przyrost: [Firebase Authentication na lokalnym devie — wynik i uruchomienie](documentation/firebase-authentication-local.md). Google, zabezpieczenie API, profil i mapowanie UID działają; backend220/220, web build/lint/30 model/37 browser oraz4 testy auth i1 test rzeczywistego UI/API przeszły. Użytkownik potwierdził prawdziwe logowanie Google, profil Mihu i dotychczasową historię. V24 na właściwej bazie zachowała17 aktywnych sesji/137 wpisów oraz całą historię i katalog. Osobne konta modelowe działają bez skrzynek. DEC-050–052; bez resetu/replay.

Aktualizacja 2026-11-08: [Environments plan — dev local i production](documentation/Environments_plan.md) skrócony dla pierwszych 5–10 osób. Użytkownik rezygnuje z QA/staging i akceptuje samodzielne utrzymanie. Porównanie tanich VPS/Neon, replikacja i backupy chroniące dane; bez zakupu i wdrożenia. DEC-049 oraz OPEN-21–25 zachowują ustalenia i otwarte decyzje.

Wdrożono kolejny raport Statistics: **Training summary** dla miesiąca i roku, sześć kafli, wykres słupkowo-liniowy, pierścienie dyscyplin i udanych stylów oraz wyróżnienia z linkami do sesji. DEC-048: pełny czas pasujących sesji i najlepszy udany styl na climbId w okresie. Backend 210/210, web build/lint/30 model/37 browser oraz realny browser UI/API przeszły. Sprawdzono 18 raportów na 17 sesjach/137 wpisach bez zmian historii i bazy. Do użycia na8080 potrzebny restart backendu; bez migracji i ponownego importu. [Opis i weryfikacja](documentation/period-training-summary.md).

Najnowszy przyrost: Adjusted Load v3 z kwadratowym zanikiem dowodów poziomu przez 730 dni, bez wędki i z zachowanym maksimum przy próbce poniżej 10. Wyceny/punkty i historyczne wyniki bez zmian. 176 testów backendu, build/lint/24 model web i realny browser CRUD v3 przeszły; local ma V23. Po restarcie backendu na 8080 ukończono właściwy chronologiczny replay: 16 aktywnych sesji/133 wpisy v3, wejścia i intensywności zachowane. Dashboard i wszystkie 12 widoków piramidy sprawdzone przez HTTP. [Model i dowody](documentation/recency-load.md).

Operacja zabezpieczenia i odtworzenia danych 2026-11-04 zakończona: po kopii SQL i odwracalnym usunięciu aktywnej historii Mihu odtworzono 16 sesji/133 wpisy przez API nowego kalkulatora. Pełna weryfikacja potwierdziła zgodność wejść, niezmienione oryginalne 35 sesji/278 wpisów/46 revisions oraz katalog i dane innych kont. Kopia i dziennik replay pozostają zachowane. [Raport i lokalizacja kopii](documentation/training-history-export.md).

Najnowsze usprawnienie sesji: Repeat climb oraz dokładna liczba ruchów dla Completed (DEC-046, np. 8/8). Build/lint, 24 model/31 browser/136 backend przeszły; historia i schema bazy bez zmian. [Opis](documentation/frontend-web.md#repeat-climb-i-completed--2026-11-04).

Usprawnienie po ocenie UI 2026-11-04: ręczny Start ustawia End +1 h; Repeat session otwiera datę i edytowalną kopię wpisów w tym samym miejscu. Nowe ID i osobne odzyskiwalne szkice, bez zmian backendu/bazy. Build/lint, 16 testów modelu i 26 browser przeszły. [Opis](documentation/frontend-web.md#ręczna-data-i-repeat-session--2026-11-04).

Najnowszy przyrost 2026-11-04: frontend Statistics z piramidą wdrożony, z miejscem na kolejne raporty okresowe. Build/lint, 11 testów modelu, 22 przeglądarkowe i odczyt 12 kombinacji przez prawdziwy backend przeszły; historia pozostała identyczna. [Raport i uruchomienie](documentation/statistics-frontend.md). Pozostaje ręczna ocena interfejsu oraz wspólne dodanie sesji v2.

Przyrost 2026-11-03: backend i baza piramidy osiągnięć wdrożone. Raport, listy, ważony estimator i wykluczenie wędki z osiągnięć przeszły 120 testów; local ma V22, historia pozostała niezmieniona. Potwierdzono 12 kombinacji lokalnego HTTP. Frontend wdrożono następnego dnia. [Kontrakt i weryfikacja](documentation/achievement-pyramid-backend.md).

Bieżący stan 2026-11-01: backend i baza modelu v2 są wdrożone — `profileIntensityIndex`, crux ceil(15% wykonanych), snapshoty, audyt i wersje w API. Lokalny Flyway: V20, historia wyników zachowana. Pełny zestaw 74 testów backendu przeszedł. Frontend v2 również wdrożony: bez EDL, z intensywnością i cruxem w Details, aktywnymi profilami oraz zachowaniem szkiców/retry. Build/lint, 6 testów modelu i 13 przeglądarkowych przeszły. Następnie wspólne uruchomienie UI i dodanie sesji przez backend V20. [Raport frontendu](documentation/frontend-web.md#weryfikacja-v2--2026-11-01), [model v2](business/grading-and-scoring.md#zatwierdzona-zmiana-v2--edl-zastępujemy-przez-profileintensityindex), [weryfikacja backendu](documentation/backend-verification.md), DEC-043–044 w [rejestrze decyzji](documentation/decisions.md).

Aktualny przyrost 2026-10-17: React 1–5 jest podłączony do API i sprawdzony, w tym CRUD przez prawdziwy PostgreSQL; oczekuje ocena użytkownika. [Raport i uruchomienie web](documentation/frontend-web.md). Backend ma 52 zielone testy; [kontrakt CRUD](documentation/backend-crud-handoff.md) i [weryfikacja backendu](documentation/backend-verification.md).

To punkt wejścia do wiedzy o projekcie. Opisujemy osobno ustalenia produktu, stan kodu i zamierzenia. Utworzenie dokumentu lub diagramu nie oznacza wdrożenia funkcji.

Ustalenia 2026-10-24: [logowanie Google i profil użytkownika we frontendzie](documentation/frontend-authentication.md) — struktura plików, podział Firebase/API/PostgreSQL i przepływ logowania; plan implementacji.

Ustalenie 2026-11-01, wdrożone w raporcie i Statistics 2026-11-03–04: [piramida osiągnięć](business/achievement-pyramid.md), DEC-045 — jedna wspinaczka raz, cała historia/ostatnie dwa lata, RP i OS we wszystkich trzech widokach; wędka wyłącznie treningiem, nigdy osiągnięciem. Metodę ważonego Pyramid Support potwierdzono; integracja poziomu z loadem pozostaje w OPEN-20. Wspólne dodanie sesji v2 przez UI pozostaje do wykonania.

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
│   ├── achievement-pyramid.md    # unikalne osiągnięcia, okresy, RP/OS, wykluczenie wędki
│   └── training.md               # ocena treningów i założenia rozwoju
│
├── documentation/
│   ├── architecture.md             # ogólny obraz systemu i zależności
│   ├── engineering-thesis-notes.md # decyzje i dowody do pracy inżynierskiej
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
| Materiał o architekturze do pracy inżynierskiej ---- | [engineering thesis notes](documentation/engineering-thesis-notes.md) ----------------------------------------------- |
| Java / React / Flutter ----------------------------- | [backend](documentation/backend.md), [web](documentation/frontend-web.md), [mobile](documentation/frontend-mobile.md) |
| Relacje danych / kontrakt / uruchomienie ----------- | [database](documentation/database.md), [api](documentation/api.md), [development](documentation/development.md) ----- |
| Środowiska, hosting, bezpieczeństwo i wdrożenia      | [Environments plan](documentation/Environments_plan.md)                                                               |
| Co zostało ustalone, co nadal wymaga decyzji ------- | [decisions](documentation/decisions.md) ----------------------------------------------------------------------------- |
| Co robimy teraz ------------------------------------ | [priorities](project-management/priorities.md) ---------------------------------------------------------------------- |
| Kolejne przyrosty i trzy iteracje dokumentacji ----- | [roadmap](project-management/roadmap.md) ---------------------------------------------------------------------------- |
| Faktyczny postęp / czas / prognoza ----------------- | [progress](project-management/progress.md), [work-log](project-management/work-log.md) ------------------------------ |

## Jak czytać statusy i źródła

- **Ustalone**: wynika z wyraźnej decyzji użytkownika lub z zatwierdzonych źródeł. Nie oznacza implementacji.
- **Potwierdzone w kodzie**: znaleziono odpowiednią implementację lub konfigurację w przeglądzie repozytorium. Uruchomienie i wynik testów podajemy osobno wraz z datą.
- **Plan / propozycja**: kierunek do realizacji lub omówienia, nie działająca funkcja.
- **Otwarte**: wymaga rozstrzygnięcia; identyfikator `OPEN-*` prowadzi do rejestru decyzji.
- **Historyczne**: źródło zachowane dla ciągłości, nie bieżące polecenie ani aktualna checklista.

Wyraźne bieżące ustalenia użytkownika mają pierwszeństwo. Wymagania web z 2026-09-22 zastępują wcześniejsze plany rozpoczynające integrację od Fluttera; nie zatwierdzają automatycznie akapitów oznaczonych w nich jako propozycje. Reguły wyceny po konsolidacji z 2026-08-15 zastępują wcześniejsze tabele z `old-doc`. Przy konflikcie nie zgadujemy: zapisujemy go w `decisions.md`.

## Zasada utrzymania

Wzory mają jedno źródło w `business/grading-and-scoring.md`, priorytety w `project-management/priorities.md`, a otwarte decyzje w `documentation/decisions.md`. Inne pliki odsyłają do nich. Status kodu aktualizujemy razem z dowodem; czas pracy zapisujemy tylko, gdy był mierzony lub podany przez użytkownika.

W iteracji 1 zachowano oryginalne `docs`, `.agents` i stare plany na miejscu oraz wykonano kopie w `archive`. DEC-019 ustala, że bieżąca `.ai` wraz z archiwum wchodzi do Git. Oryginały w `docs` pozostają poza Git do czasu usunięcia ich przez użytkownika; zachowane kopie nie są drugą aktywną dokumentacją.
