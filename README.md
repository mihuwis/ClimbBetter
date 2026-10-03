# ClimbBetter

Aplikacja do rejestrowania treningów wspinaczkowych, analizy obciążenia i postępów. Docelowo także planowanie i propozycje treningów.

## Stan projektu

Backend Java ma szkielet Spring Boot, konfigurację i testy podstawowe; logika treningowa i jej API pozostają do odtworzenia. Web React i mobile Flutter są prototypami na danych przykładowych. Bieżący priorytet to Java i pierwszy rzeczywisty przepływ web: Record session → zapis → Dashboard → szczegóły treningu.

## Struktura

| Katalog | Przeznaczenie |
| --- | --- |
| `backend/` | rozwijany backend Java |
| `backend-net/` | archiwalny prototyp .NET jako materiał odniesienia |
| `frontend-web/` | React, TypeScript i Vite |
| `frontend-mobile/` | Flutter |
| `infra/` | miejsce na infrastrukturę; obecnie bez Compose |
| `.ai/` | zasady współpracy, wiedza biznesowa, dokumentacja techniczna i zarządzanie projektem |

## Dokumentacja i uruchomienie

- [Mapa dokumentacji i pełny schemat folderów](.ai/README.md).
- [Uruchomienie i weryfikacja web, Javy oraz Fluttera](.ai/documentation/development.md).
- [Aktualne priorytety](.ai/project-management/priorities.md) i [stan projektu](.ai/project-management/progress.md).
- [Decyzje i pytania drugiej iteracji](.ai/documentation/decisions.md).
- [Punkt wejścia do zasad współpracy](AGENTS.md).

Porządkowanie dokumentacji jest w iteracji 2 z 3 (2026-10-03). `.ai` wraz z [archiwum](.ai/archive/README.md) ma być wersjonowana w Git. Oryginalne `docs` pozostają poza Git do czasu usunięcia ich przez użytkownika; bieżące informacje rozwijamy w `.ai`.
