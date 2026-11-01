# Uruchomienie i weryfikacja

Status: zweryfikowane uruchomienie testowe i lokalne; aktualizacja 2026-10-11.

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

Web korzysta z prawdziwego API i wymaga uruchomionego backendu. Vite działa na http://localhost:5173 i przekazuje `/api` do `127.0.0.1:8080`. `CB_API_PROXY_TARGET` może zmienić cel proxy; `VITE_API_URL` ustawia bezpośredni adres API i zwykle nie jest potrzebne. Zajęty 5173 kończy uruchomienie błędem zamiast zmieniać port. Zweryfikowano Node 22.19.0 i npm 11.6.2. Dostępne są `npm test` (model/szkic) i `npm run test:e2e` (headless Edge, domyślnie kontrolowane odpowiedzi HTTP). [Pełny opis i test rzeczywistej bazy](frontend-web.md).

## Java

Ostatnia pełna weryfikacja: 2026-10-17, Maven `test`, **51 testów, 0 failures/errors/skipped, BUILD SUCCESS**. [Zakres i raport](backend-verification.md).

Projekt deklaruje Java 21; wrapper pobiera Maven zgodnie z `backend/.mvn/wrapper/maven-wrapper.properties`. W katalogu `backend`:

```powershell
.\mvnw.cmd verify
```

Test kontekstu korzysta z Testcontainers PostgreSQL i wymaga wcześniej uruchomionego Docker Desktop. Na początku nowego dnia, przed pełnym `./mvnw test`, sprawdzamy `docker version`; wynik powinien zawierać sekcje `Client` i `Server`. Local i testy używają obrazu `postgres:17.6-alpine`.

Testcontainers nadal nie wymaga Compose: tworzy tymczasowy kontener, a `@ServiceConnection` przekazuje połączenie do Spring Boot, Flyway i JPA. Dane testowe znikają wraz z kontenerem. Trwałe środowisko local jest osobnym serwisem z [infra/compose.yml](../../infra/compose.yml) i nazwanym wolumenem.

Pełne `./mvnw test` z 2026-10-10 wykonało 13 testów bez failures, errors i skipped. Obejmuje to test integracyjny PostgreSQL dla `DashboardQuery` oraz dwa testy kontraktu kontrolera.

## Swagger UI i OpenAPI

Aktualizacja 2026-10-17: dodano `springdoc-openapi-starter-webmvc-ui` w wersji `3.1.1`, zgodnie z [instrukcją springdoc](https://springdoc.org/getting-started.html). Kompilacja aplikacji i testów przeszła; odpowiedzi HTTP dokumentacji czekają na sprawdzenie po uruchomieniu backendu.

W Bash, w katalogu `backend`, przy działającej lokalnej bazie PostgreSQL:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

- Swagger UI: <http://localhost:8080/swagger-ui.html> (przekierowuje do interfejsu).
- Specyfikacja OpenAPI JSON: <http://localhost:8080/v3/api-docs>.
- Specyfikacja OpenAPI YAML: <http://localhost:8080/v3/api-docs.yaml>.

Dokumentacja obejmuje `/api/v1/**` i generuje modele z kontrolerów oraz DTO. Przyciski `Try it out` i `Execute` wysyłają rzeczywiste żądania do uruchomionego backendu, w profilu local korzystającego z trwałej bazy. Po dodaniu zależności trzeba ponownie uruchomić backend.

## Lokalny PostgreSQL

Z głównego katalogu repozytorium:

```bash
docker compose -f infra/compose.yml up -d
docker compose -f infra/compose.yml ps
```

Serwis mapuje host `127.0.0.1:5433` na port PostgreSQL `5432` w kontenerze. Port hosta 5433 wybrano, ponieważ 5432 był już używany przez inną lokalną instancję. Nazwany wolumen zachowuje dane po `stop`, `down` i restarcie Docker Desktop; `down -v` usuwa dane i nie jest zwykłym poleceniem zatrzymania.

Profil `local` ma wartości domyślne zgodne z Compose: URL `jdbc:postgresql://127.0.0.1:5433/climbbetter`, użytkownika `climbbetter` i hasło przeznaczone wyłącznie do lokalnego kontenera. Opcjonalne `CB_DB_URL`, `CB_DB_USERNAME` i `CB_DB_PASSWORD` nadpisują te wartości, np. dla innego środowiska deweloperskiego.

Uruchomienie z katalogu `backend` nie wymaga ręcznego ustawiania zmiennych:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Uruchomienie z 2026-10-11 połączyło się z PostgreSQL 17.6, zastosowało migracje do `V6` i wystartowało na porcie 8080. DBeaver potwierdził tabelę `catalog.grades` wraz z danymi obu skal. Wcześniejsze ręczne `curl http://localhost:8080/api/v1/dashboard/sessions` zwróciło `[]`. Swagger UI dodano 2026-10-17; adresy podano powyżej.

## Lokalne dane przykładowe i wybór użytkownika

Przykładowa historia Mihu obejmuje też 14 sesji od 1 września do 15 października 2026, po dwie tygodniowo. Zapisano je przez serwisy aplikacji, a GET Dashboardu potwierdził ich odczyt. [Raport i założenia danych DEMO](mihu-demo-history.md).

2026-10-17 bezpośrednio do trwałej bazy Compose dodano dane ze skryptu [local-sample-catalog.sql](../../infra/sql/local-sample-catalog.sql). Nie jest on migracją Flyway ani danymi środowiska produkcyjnego. Stałe UUID i `ON CONFLICT (id) DO NOTHING` pozwalają ponowić skrypt bez duplikatów i bez nadpisywania zmian użytkownika.

Konto `Mihu` ma UUID `00000000-0000-0000-0000-000000000002`; istniejący `Local Developer` zachowuje UUID kończące się `0001`. Publiczne Area `Bronx` należy do Mihu i ma typ `ARTIFICIAL`/`BOULDER_GYM`, zgodnie z potwierdzeniem użytkownika z 2026-10-17. Area `Garaż` jest prywatne, typu `ARTIFICIAL`/`HOME_WALL`.

Wszystkie sześć Climb jest prywatnych i należy do Mihu:

- Bronx: `Zielona po glizdach` (`5`), `Zielona kule` (`5+`), `niebieska w rogu` (`6A`); `BOULDER_FONT`, typ/profil `BOULDER`, proponowane 8 ruchów zgodnie z domyślną wartością DEC-027.
- Garaż: `zielony bazowy` (`6a`), `fiolet bazowy` (`6b`), `pomarańcz bazowy` (`6c`); `ROUTE_FRENCH`, typ/profil `CIRCUIT`, 38 ruchów każdy.

Transakcja SQL została zatwierdzona. Zapytania odpowiadające filtrom katalogu potwierdziły: Local Developer widzi 1 Area i 0 Climb, a Mihu 2 Area i 6 Climb. Nie potwierdzono jeszcze tych danych przez HTTP. Nie dodano sesji treningowych.

Później 2026-10-17 rozszerzono ten sam skrypt o 10 prywatnych baldów Mihu na Bronxie: `niebieski01`–`niebieski05` i `Pomarańczowy01`–`Pomarańczowy05`, o wycenach 6A/6A+/6B i sugestii 8 ruchów. Zapis w trwałej bazie potwierdzono przez COMMIT i odczyt SQL: Bronx zawiera teraz 13 prywatnych baldów Mihu. Publiczny katalog Doliny Bolechowickiej dodano osobno, zgodnie z [opisem importu](outdoor-catalog.md).

Profil `local` pozwala wybrać tożsamość przez `CB_LOCAL_USER_ID`, domyślnie pozostawiając Local Developer. Po zatrzymaniu wcześniej uruchomionego backendu, w Bash z katalogu `backend`, uruchomienie jako Mihu wygląda tak:

```bash
CB_LOCAL_USER_ID=00000000-0000-0000-0000-000000000002 ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Zwykłe polecenie bez tej zmiennej nadal wybiera Local Developer, o ile zmienna nie została wcześniej wyeksportowana w terminalu. To wybór jednej tożsamości dla całego procesu deweloperskiego, nie logowanie ani przełączanie kont z przeglądarki. Reguły widoczności filtrują po UUID aktywnego użytkownika; publiczne Area nie odsłania cudzych prywatnych Climb. Testowy profil domyślnie pozostaje przy Local Developer, a dane przykładowe nie są ładowane do Testcontainers.

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
