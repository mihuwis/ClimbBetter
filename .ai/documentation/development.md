# Uruchomienie i weryfikacja

Status: instrukcje wynikające z plików repozytorium; 2026-10-03. Poleceń build/test nie uruchamiano podczas tej iteracji dokumentacji.

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

Test kontekstu korzysta z Testcontainers PostgreSQL i wymaga wcześniej uruchomionego Docker Desktop. Na początku nowego dnia, przed pełnym `./mvnw test`, sprawdzamy `docker version`; wynik powinien zawierać sekcje `Client` i `Server`. Pobranie zależności lub obrazu może wymagać sieci. Aktualna konfiguracja testu używa nieprzypiętego `postgres:latest`; do wyboru jest wersja powtarzalna (`OPEN-13`).

Repozytorium nie potrzebuje obecnie `Dockerfile` ani Compose do tych testów. `TestcontainersConfiguration` w kodzie testowym deklaruje `PostgreSQLContainer` na podstawie publicznego obrazu `postgres:latest`. Podczas uruchomienia testu biblioteka prosi działający Docker o utworzenie tymczasowego kontenera, a `@ServiceConnection` przekazuje parametry połączenia do Spring Boot, Flyway i JPA. Nie skonfigurowano trwałego wolumenu ani stałego portu; baza służy testowi i jej danych nie traktujemy jako lokalnego środowiska developerskiego.

Pierwsza próba pełnego uruchomienia z 2026-10-04 wykazała brak działającego Dockera. Po uruchomieniu Docker Desktop ponowne `./mvnw test` zakończyło się sukcesem: `FamiliarityBandTest` 7/7, testy kontekstu i zegara 2/2 oraz test granic modułów 1/1. Łącznie wykonano 10 testów bez failures, errors i skipped.

Profil `local` odczytuje konfigurację bazy z otoczenia. Nazwy to `CB_DB_URL`, `CB_DB_USERNAME` oraz nazwa zmiennej wskazana przy `password` w [application-local.yml](../../backend/src/main/resources/application-local.yml). Nie kopiujemy wartości lokalnych poświadczeń do dokumentacji.

Po przygotowaniu bazy i zmiennych, planowane uruchomienie z tego katalogu:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

To uruchomienie szkieletu: nie ma jeszcze migracji ani treningowego API. W repozytorium nie ma `infra/compose.yaml`, dlatego nie podajemy fikcyjnej komendy Compose. Najpierw rozstrzygamy użycie istniejącej bazy lub nowej instancji (`OPEN-13`). Health jest oczekiwany z zależności Actuator, lecz odpowiedź endpointu nie była sprawdzana. Swagger UI nie jest jeszcze skonfigurowany.

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
