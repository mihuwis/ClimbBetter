# Uruchomienie i sprawdzanie

Aktualizacja: 2026-11-09. Polecenia z bieżących launcherów i konfiguracji. Ostatnie wyniki uruchomień: [weryfikacja](backend-verification.md).

## Wymagania

Java 21, Node 22/npm, Docker Desktop z działającym serwerem (`docker version`), Python dla narzędzi historii. Maven przez wrapper. Local i Testcontainers używają `postgres:17.6-alpine`; testy integracyjne tworzą własną bazę, bez Compose.

## Standardowy dev z Google/Firebase

Z katalogu głównego uruchom bazę:

```powershell
docker compose -f infra/compose.yml up -d
docker compose -f infra/compose.yml ps
```

Frontend: skopiuj `frontend-web/.env.example` do ignorowanego `.env.local` i uzupełnij publiczną konfigurację projektu `climbbetter-dev`. Dla Mihu zachowaj `VITE_AUTH_ACCOUNT_MODE=existing`. Konto jest już powiązane; [instrukcja auth](firebase-authentication-local.md).

Backend w osobnym terminalu; JSON konta usługi pozostaje poza repozytorium:

```powershell
./infra/start-local-api.ps1 -CredentialsFile 'C:/path/outside/repository/firebase-dev-admin.json'
```

Web w kolejnym terminalu:

```powershell
Set-Location frontend-web
npm.cmd ci
npm.cmd run dev
```

Web: `http://localhost:5173`, API: `http://127.0.0.1:8080`, health: `/actuator/health`. Vite przekazuje `/api` do 8080; zajęty port 5173 kończy start błędem. `CB_API_PROXY_TARGET` zmienia cel proxy, `VITE_API_URL` pozwala użyć bezpośredniego adresu API.

Compose ma trwały wolumen i port hosta 5433. `stop`/`down` zachowują dane; `down -v` je usuwa. Profil local obsługuje `CB_DB_URL`, `CB_DB_USERNAME`, `CB_DB_PASSWORD`; standardowy launcher wskazuje bazę `climbbetter`. Pliki `.env.local`, klucze, tokeny i backupy nie trafiają do Git. Lokalny port bazy nie jest produkcyjną konfiguracją sieciową.

Swagger: `http://127.0.0.1:8080/swagger-ui.html`, OpenAPI: `/v3/api-docs` i `/v3/api-docs.yaml`. Bearer ustawiamy w Authorize; Execute wysyła rzeczywiste operacje do bazy. Sama dostępność dokumentacji w local nie wyłącza auth API.

## Testy

W `backend`, przy działającym Dockerze:

```powershell
./mvnw.cmd verify
```

W `frontend-web`:

```powershell
npm.cmd run lint
npm.cmd run build
npm.cmd test
npm.cmd run test:e2e
```

Playwright używa headless Edge (`msedge`); przeglądarka musi być dostępna. Domyślny zestaw ma kontrolowane odpowiedzi API i nie zapisuje lokalnej historii. `CB_AUTH_E2E=1` wybiera testy SDK/emulatora z kontrolowanym API; `CB_REAL_STATISTICS_API=1` i `CB_REAL_PERIOD_SUMMARY_API=1` — odczyty raportów; `CB_REAL_API=1` — CRUD wyłącznie na osobnej bazie. Przełączniki wybierają osobne zestawy, nie wszystkie naraz.

Pełny UI → Firebase SDK → Java → baza wymaga **obu** `CB_REAL_AUTH_API=1` i `CB_AUTH_E2E=1`, działającego emulatora/API z danymi modeli oraz `CB_API_PROXY_TARGET=http://127.0.0.1:18081`. Pierwszy przełącznik wybiera plik testu, drugi ustawia web w tryb emulatora; sam `CB_REAL_AUTH_API` pozostawia w obecnym launcherze tryb stub. Ustawiamy zmienne w terminalu przed `npm.cmd run test:e2e`, usuwamy je po zakończeniu.

[Uruchomienie emulatora i modeli](firebase-authentication-local.md#konta-modelowe). Dla jawnego debugowania bez Firebase istnieje `./infra/start-local-api.ps1 -AuthMode stub`; nie jest to standardowy login ani test Firebase.

Flutter, w `frontend-mobile`: `flutter pub get`, `flutter analyze`, `flutter test`, `flutter devices`, następnie `flutter run -d <device_id>`. Gotowość SDK/platform wymaga lokalnego sprawdzenia.

Zmiany dokumentacji sprawdzamy pod kątem źródeł, linków, decyzji i diagramów. Nie trzeba ponawiać testów aplikacji dla samej redakcji.
