# ClimbBetter

Aplikacja do rejestrowania treningów wspinaczkowych, analizy obciążenia i postępów. Docelowo także planowanie i propozycje treningów.

## Uruchomienie aplikacji lokalnie

Backend z Firebase Auth uruchamiamy z katalogu głównego CB w PowerShell. Podaj ścieżkę do pobranego pliku JSON konta usługi projektu `climbbetter-dev`, przechowywanego poza repozytorium:

```powershell
.\infra\start-local-api.ps1 -CredentialsFile 'C:\path\outside\repository\firebase-dev-admin.json'
```

Frontend w osobnym terminalu:

```powershell
Set-Location frontend-web
npm.cmd run dev
```

Aplikacja: `http://localhost:5173`. Użytkownika rozpoznaje token Google/Firebase, a dotychczasowy UUID Mihu zachowujemy przez kontrolowane powiązanie. `CB_LOCAL_USER_ID` dotyczy wyłącznie jawnego trybu debugowania `stub`.

[Instrukcja Firebase, pierwszego powiązania Mihu i kont modelowych bez dodatkowych e-maili](.ai/documentation/firebase-authentication-local.md).

## Stan projektu

Backend Java obsługuje CRUD sesji, obliczenia, audyt i katalog wspinaczek w PostgreSQL. React korzysta z API: Dashboard, szczegóły z edycją/usuwaniem, kalendarz, dodanie ukończonej sesji i Start now z trwałym lokalnym szkicem. Ten zakres przeszedł testy i oczekuje oceny użytkownika. Flutter pozostaje prototypem. [Raport i uruchomienie React](.ai/documentation/frontend-web.md).

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
