# Uruchomienie i weryfikacja

Status: zweryfikowane uruchomienie testowe i lokalne; aktualizacja 2026-10-10.

Polecenia poniżej wykonuje użytkownik albo asystent w wyraźnie zleconym zakresie. Nie są automatycznym poleceniem ich wykonania przez agenta.

## Web

W katalogu `frontend-web`:

```powershell
npm ci
npm run dev
```

Weryfikacja obecnego projektu:

```powershell
npm run lint
npm run build
```

Skryptu `npm test` jeszcze nie ma. Projekt używa mocków i nie wymaga działającego API do samego prototypu UI. `VITE_API_URL` jest planem integracji, nie istniejącą konfiguracją. Zgodność lokalnego Node z wymaganiami zależności trzeba sprawdzić przed uruchomieniem; bieżącej instalacji nie odczytywano.

## Java

Projekt deklaruje Java 21; wrapper pobiera Maven zgodnie z `backend/.mvn/wrapper/maven-wrapper.properties`. W katalogu `backend`:

```powershell
.\mvnw.cmd verify
```

Test kontekstu korzysta z Testcontainers PostgreSQL i wymaga wcześniej uruchomionego Docker Desktop. Na początku nowego dnia, przed pełnym `./mvnw test`, sprawdzamy `docker version`; wynik powinien zawierać sekcje `Client` i `Server`. Local i testy używają obrazu `postgres:17.6-alpine`.

Testcontainers nadal nie wymaga Compose: tworzy tymczasowy kontener, a `@ServiceConnection` przekazuje połączenie do Spring Boot, Flyway i JPA. Dane testowe znikają wraz z kontenerem. Trwałe środowisko local jest osobnym serwisem z [infra/compose.yml](../../infra/compose.yml) i nazwanym wolumenem.

Pełne `./mvnw test` z 2026-10-10 wykonało 13 testów bez failures, errors i skipped. Obejmuje to test integracyjny PostgreSQL dla `DashboardQuery` oraz dwa testy kontraktu kontrolera.

## Lokalny PostgreSQL

Z głównego katalogu repozytorium:

```bash
docker compose -f infra/compose.yml up -d
docker compose -f infra/compose.yml ps
```

Serwis mapuje host `127.0.0.1:5433` na port PostgreSQL `5432` w kontenerze. Port hosta 5433 wybrano, ponieważ 5432 był już używany przez inną lokalną instancję. Nazwany wolumen zachowuje dane po `stop`, `down` i restarcie Docker Desktop; `down -v` usuwa dane i nie jest zwykłym poleceniem zatrzymania.

Profil `local` odczytuje `CB_DB_URL`, `CB_DB_USERNAME` i `CB_DB_PASSWORD`. Dla obecnego Compose URL ma postać `jdbc:postgresql://127.0.0.1:5433/climbbetter`.

Po ustawieniu zmiennych uruchomienie z katalogu `backend`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Uruchomienie z 2026-10-10 połączyło się z PostgreSQL 17.6, zastosowało migracje `V1`–`V4` i wystartowało na porcie 8080. `curl http://localhost:8080/api/v1/dashboard/sessions` zwrócił `[]`. Swagger UI nadal nie jest skonfigurowany.

## Flutter

W katalogu `frontend-mobile`:

```powershell
flutter pub get
flutter analyze
flutter test
flutter devices
```

Po przygotowaniu wybranej platformy: `flutter run -d <device_id>`. Repozytorium zawiera także target web, więc przy dostępnym urządzeniu Chrome można użyć `flutter run -d chrome`. Android SDK, licencje, emulator i aktualny wynik `flutter doctor` wymagają osobnego sprawdzenia. Historia dokumentacji nie dowodzi obecnego stanu instalacji.

## Jakość zależna od zmiany

Dla zmian dokumentacyjnych sprawdzamy pliki, źródła, odsyłacze, spójność decyzji i diagramów. Dla przyszłej implementacji dochodzą właściwe testy: czysty kalkulator na ręcznie potwierdzonych przykładach, migracje i transakcje na PostgreSQL oraz scenariusze UI i integracji. Nie raportujemy obecności pliku testowego jako zielonego wyniku.

Sekrety, wartości `.env` i lokalne bazy nie wchodzą do archiwum dokumentacji. W iteracji 1 istniejąca zmiana użytkownika w `.vscode/settings.json` pozostaje poza zakresem.
