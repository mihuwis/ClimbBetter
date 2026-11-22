# Frontend mobile

Aktualizacja: 2026-11-09. Stan z kodu `frontend-mobile/lib` i `pubspec.yaml`; Flutter nie był uruchamiany w tym przeglądzie.

Flutter pozostaje prototypem: shell, Board/Home, profil, formularz zakończonej sesji, lokalne presety i mocki. Formularz używa `setState`, zapis kończy się komunikatem. Nie ma klienta API, Firebase, trwałej bazy ani outboxa. Starsze użycie `defaultEdl` nie odpowiada obecnemu modelowi v3.

Kierunek: wspólne API z webem, lokalny zapis i później offline z synchronizacją. Planowany przepływ: widget → controller → repository → źródło lokalne/API. Sesje i wpisy zachowają client IDs, wyniki będą pochodzić z backendu. Wybór bibliotek, konflikty wielu urządzeń i tombstones pozostają w OPEN-17; historyczne propozycje nie są zależnościami projektu.

Odbiór integracji musi objąć restart aplikacji, brak sieci, retry bez duplikacji i izolację kont. [Uruchomienie narzędzi](development.md), [aktualny model](../business/grading-and-scoring.md), [plan historyczny](../archive/source-snapshot/docs/CB_frontend-mobile.md).
