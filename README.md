# ClimbBetter

Aplikacja do zapisu treningów wspinaczkowych, analizy obciążenia i postępów.

Lokalnie działa React → Java → PostgreSQL: Google/Firebase i profil, CRUD sesji, trwały szkic, Calendar/Areas, model obciążenia v3 oraz Statistics (piramida i podsumowanie miesiąca/roku). Produkcja i GitHub Actions/GHCR mają przygotowany plan; wdrożenie pozostaje do wykonania. Flutter jest prototypem.

- [Uruchomienie lokalne](.ai/documentation/development.md) i [Firebase/konta modelowe](.ai/documentation/firebase-authentication-local.md).
- [Stan aplikacji](.ai/project-management/progress.md), [priorytety](.ai/project-management/priorities.md), [plan produkcji i CI/CD](.ai/documentation/Environments_plan.md).
- [Mapa dokumentacji](.ai/README.md), [decyzje](.ai/documentation/decisions.md), [zasady współpracy](AGENTS.md).

| Katalog            | Rola                                                                  |
| ------------------ | --------------------------------------------------------------------- |
| `backend/`         | Java/Spring Boot, API i obliczenia                                    |
| `frontend-web/`    | React/TypeScript/Vite                                                 |
| `frontend-mobile/` | prototyp Flutter                                                      |
| `infra/`           | PostgreSQL Compose, launchery, emulator Firebase i narzędzia historii |
| `.ai/`             | bieżąca dokumentacja i archiwum źródeł                                |
| `backend-net/`     | archiwalny prototyp .NET                                              |

Dokumentacja aplikacji jest w `.ai`, a aktywne materiały pracy inżynierskiej w `docs`: [rozdziały i przegląd aktualności](docs/aktualizacja_dokumentow_2026-11-09.md). [Archiwum](.ai/archive/README.md) zachowuje dawne źródła aplikacji i pełną kopię sprzed skrócenia 2026-11-09; nie zastępuje obecnych materiałów dyplomowych.





## Uruchomiene Backend

z poziomu katalogu backend 

powershell.exe -NoProfile -File ../infra/start-local-api.ps1 -CredentialsFile 'C:/Users/micha/developer/FIRE_BASE_private_key/Climbbetter/climbbetter-dev-firebase-adminsdk-fbsvc-79927bd78b.json'