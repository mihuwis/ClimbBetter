# Historia prac i pomiary

Data utworzenia: 2026-10-03. To ewidencja zdarzeń i rzeczywiście znanego czasu, nie odtworzenie nakładu z dat Git.

## Znane zdarzenia

2026-10-17, APP-02 — na polecenie użytkownika wdrożono React punkty 1–5: Dashboard na API, grupowane Details/chronologia, edycja i usuwanie wpisów oraz sesji, kalendarz, ręczna sesja i Start now z timerem. Użytkownik potwierdził angielski UI. Podział `app`/`pages`/domenowe `features`, usunięcie mocków, trwały szkic użytkownika wraz z niedokończonym wpisem i dokładnym żądaniem retry. API uzupełniono o bieżącą tożsamość i listę sesji w zakresie dat. Weryfikacja: build/lint sukces, model 4/4, przeglądarka 6/6, prawdziwy CRUD React → API → PostgreSQL 1/1; pełny backend 52/52. Sprawdzono desktop/mobile; sesje testów SMOKE usunięto przez API, pozostawiając audyt. Status: gotowe do oceny użytkownika. Estymata oraz czas użytkownika/asystenta: nieznane, nie były mierzone. [Raport](../documentation/frontend-web.md).

| Data       | Źródło                                    | Rezultat / zdarzenie                                                                                                                   | Nakład       | Granica dowodu                                                                              |
| ---------- | ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ------------ | ------------------------------------------------------------------------------------------- |
| 2026-07-12 | commit `119841d` ------------------------ | zapis etapu prototypu mobile --------------------------------------------------------------------------------------------------------- | nieznany --- | opis historycznego commitu; nie aktualny wynik testów ------------------------------------- |
| 2026-08-15 | dokumenty modelu i wyceny --------------- | konsolidacja specyfikacji, dawna iteracja 4 z 4 -------------------------------------------------------------------------------------- | nieznany --- | stan dokumentacji ------------------------------------------------------------------------- |
| 2026-08-15 | commit `da1324a` ------------------------ | migracja kierunku backendu do Java 21 ------------------------------------------------------------------------------------------------ | nieznany --- | historyczny commit ------------------------------------------------------------------------ |
| 2026-08-16 | commity `91ae403`, `5cca895` ------------ | bootstrap Java/Spring i Clock -------------------------------------------------------------------------------------------------------- | nieznany --- | historyczne commity; obecność plików potwierdzona 2026-10-03 ------------------------------ |
| 2026-08-18 | commit `fe7ec39` ------------------------ | dodanie testów Clock ----------------------------------------------------------------------------------------------------------------- | nieznany --- | kod testów istnieje; nie uruchamiano go teraz --------------------------------------------- |
| 2026-09-22 | `CB_web_stage_1.md` i aktualizacje planów | web-first, lokalny szkic, atomowy zapis, Dashboard i Details ------------------------------------------------------------------------- | nieznany --- | data dokumentu, nie implementacji --------------------------------------------------------- |
| 2026-10-03 | polecenie użytkownika i nowe `.ai` ------ | iteracja 1: struktura, materiały, archiwum, status, diagramy i otwarte decyzje ------------------------------------------------------- | niezmierzony | zweryfikowano wymagane pliki, 21 kopii SHA-256 i aktywne linki; bez implementacji aplikacji |
| 2026-10-03 | decyzja użytkownika DEC-018 ------------- | zamknięto OPEN-03: jedna sesja to jedno Area; zapisano regułę i objaśnienie dla użytkowników, ujednolicono formularz, API, model i ERD | niezmierzony | aktualizacja dokumentacji; bez implementacji reguły w kodzie ------------------------------ |
| 2026-10-03 | decyzje użytkownika DEC-019–022 --------- | rozpoczęto iterację 2: `.ai` w Git, nowe zasady uprawnień, wymagany odzyskiwalny szkic i Dashboard bez dekoracyjnego wykresu ---------- | niezmierzony | odpowiedzi zapisano w aktywnej dokumentacji; bez zmian kodu aplikacji --------------------- |

Potwierdzenie APP-01 z 2026-10-03: użytkownik zaimplementował `FamiliarityBand.fromPriorContactCount` i uruchomił celowany test granic oraz wyjątku. Wynik: 7 testów, 0 failures, 0 errors. Zakres dowodu obejmuje wyłącznie tę regułę domenową.

Weryfikacja APP-01 z 2026-10-04: po uruchomieniu Docker Desktop pełne `./mvnw test` wykonało 10 testów bez failures, errors i skipped. Zakres obejmuje `FamiliarityBand`, kontekst Spring, zegar UTC i granice Modulith; nie obejmuje jeszcze migracji ani zapisu treningu.

Weryfikacja APP-01 z 2026-10-10: pełne `./mvnw test` wykonało 13 testów bez failures, errors i skipped. Lokalna aplikacja zastosowała migracje `V1`–`V4` w trwałym PostgreSQL 17.6, a ręczny GET Dashboardu zwrócił pustą listę z bazy.

## Weryfikacja 2026-10-17 — APP-01

Użytkownik potwierdził działanie Dockera i sukces `./mvnw -Dtest=TrainingSessionFlowIntegrationTest test`, uruchomionego w Bash z katalogu `backend`. Obecny test konfliktu OS potwierdza `409 ASCENT_MODE_HISTORY_CONFLICT`, brak zapisanej sesji po odrzuceniu oraz udane ponowienie z `historyConflictConfirmed = true` i zapis obu flag audytu. Test nie sprawdza jeszcze mnożnika `1.50` po potwierdzeniu ani wariantu Flash. Nie przekazano liczbowego podsumowania testów; pełnego zestawu dziś jeszcze nie potwierdzono. Rzeczywisty czas pracy pozostaje nieznany; około 5 godzin to budżet dnia.

## Ewidencja kolejnych prac

2026-10-17 — na polecenie „domknij testy” asystent sprawdził Docker i uruchomił pełny Maven `test`. Wynik: 51 testów, 0 failures/errors/skipped, BUILD SUCCESS, czas Maven 24,362 s. PostgreSQL 17.6-alpine uruchomiony przez Testcontainers; migracje do V15 przeszły. CRUD, kontrola dostępu, rollback, retry, OS/Flash i scenariusze katalogowe A/B przeszły bez potrzeby poprawek. Zaktualizowano priorytet na integrację React. [Raport](../documentation/backend-verification.md). Czas użytkownika nieznany.

2026-10-17 — przygotowano historię DEMO Mihu. Po rozszerzeniu przez użytkownika zakresu dat zapisano łącznie 14 sesji (wtorki/czwartki 1 września–15 października): 4 Garaż, 6 Bronx, 4 Bolechowicka. Pierwszym pięciu sesjom poprawiono daty przez serwis edycji z audytem i dodano dziewięć kolejnych; nie powstało 19 rekordów. Serwis aplikacji obliczył snapshoty i loady. SQL potwierdził dwa treningi tygodniowo przez siedem tygodni, 2070 ruchów, sześć rozgrzewek po 40 i przejścia 6B w drugiej/trzeciej próbie sesji. GET Dashboardu zwrócił HTTP 200 i 14 sesji. Testów automatycznych nie uruchamiano. [Dane i raport](../documentation/mihu-demo-history.md). Czas nieznany.

2026-10-17 — na polecenie użytkownika dodano bezpośrednio do local 10 prywatnych baldów Mihu na Bronxie: niebieski01–05 i Pomarańczowy01–05, wyceny 6A/6A+/6B, sugestia 8 ruchów. Rozszerzono `infra/sql/local-sample-catalog.sql`; wykonanie wstawiło dokładnie 10 nowych rekordów. COMMIT i odczyt SQL potwierdziły łącznie 13 prywatnych baldów Mihu na Bronxie. Bez zmian kodu aplikacji i bez uruchamiania testów. Czas nieznany.

2026-10-17 — na polecenie użytkownika rozszerzono katalog o Sector, kraj/region, długość w metrach i regułę sugestii 2 ruchy/m z ręcznym nadpisaniem. Dodano mapowanie KURTYKA v1 do ROUTE_FRENCH, zachowując oryginalne etykiety; projekt może pozostać bez wyceny. Flyway zastosował V15 do local. Transakcja importu zapisała publiczną Dolinę Bolechowicką, Filar Abazego i 28 pozycji (27 wycenionych); SQL potwierdził długości, ruchy i dostęp do 28 dróg dla obu kont. Kompilacja przeszła; przygotowano test reguły ruchów i rozszerzono test katalogu, bez uruchamiania testów. [Raport](../documentation/outdoor-catalog.md). Czas nieznany.

2026-10-17 — scenariusze A/B użytkownika: dodano samodzielny POST prywatnego Climb w dostępnym Area, odczyt pojedynczej pozycji, bezpieczne ponowienie po `clientClimbId` i błąd `409 CLIENT_CLIMB_ID_CONFLICT`. Zapis katalogowy nie tworzy kontaktów ani sesji. Osobny `CatalogClimbScenariosIntegrationTest` obejmuje trzy nowe i trzy wcześniej znane Climb w sesji oraz trzy zapisane przed treningiem. Kompilacja aplikacji i testów przeszła; testów nie uruchomiono. DEC-040 zapisuje zakres i rozdzielenie katalogu od planowania. Czas nieznany.

2026-10-17 — bezpośrednio do trwałego PostgreSQL Compose wstawiono konto Mihu, publiczne Area Bronx, prywatne Area Garaż i sześć prywatnych Climb użytkownika Mihu. Transakcja SQL zakończyła się COMMIT; odczyt potwierdził nazwy z polskimi znakami, wyceny i ruchy oraz widoczność: Local Developer 1 Area/0 Climb, Mihu 2 Area/6 Climb. Skrypt zachowano w `infra/sql/local-sample-catalog.sql`. Dodano wybór tożsamości profilu local przez `CB_LOCAL_USER_ID`; kompilacja przeszła, bez uruchamiania testów. Odczyt danych przez HTTP pozostaje do sprawdzenia po restarcie backendu. Rzeczywisty czas nieznany.

2026-10-17 — dodano Swagger UI/OpenAPI przez springdoc `3.1.1`, konfigurację tytułu i filtrowanie dokumentacji do `/api/v1/**`. `test-compile` z pominięciem uruchamiania testów zakończyło się `BUILD SUCCESS`. Adresy: `/swagger-ui.html`, `/v3/api-docs`, `/v3/api-docs.yaml`. Nie sprawdzono jeszcze odpowiedzi HTTP działającej aplikacji. Czas nieznany.

2026-10-17, APP-01 — na wyraźne polecenie użytkownika asystent przejął implementację bieżącego zakresu CRUD. Dodano edycję i usuwanie sesji z wersją i audytem, katalog Area/Climb, słowniki wycen/profili, walidację DTO i lokalny CORS. Przygotowano osobny test CRUD z rollbackiem, konfliktem wersji i ochroną dostępu; poprawiono izolację wcześniejszego testu OS/Flash. Kompilacja aplikacji i testów (`test-compile`, bez ich uruchamiania) zakończyła się `BUILD SUCCESS`. Działanie nowych migracji i testów integracyjnych nie jest jeszcze potwierdzone. Rzeczywisty czas pracy pozostaje nieznany. [Szczegóły](../documentation/backend-crud-handoff.md).

Każdy nowy wpis powinien mieć: datę, ID przyrostu/zadania, krótki rezultat, status przed/po, oszacowanie (jeśli przyjęte), rzeczywisty nakład, źródło pomiaru i wynik sprawdzenia. Oddzielamy czas użytkownika, pracę asystenta i czas oczekiwania. Jeśli czas nie był mierzony lub podany, wpisujemy `nieznany`.

| Data       | ID              | Rezultat                                                                      | Status                          | Estymata | Czas użytkownika | Czas asystenta | Weryfikacja                                                      |
| ---------- | --------------- | ----------------------------------------------------------------------------- | ------------------------------- | -------- | ---------------- | -------------- | ---------------------------------------------------------------- |
| 2026-10-03 | DOC-02 / APP-01 | rama `.ai`, decyzje DEC-018–022 i `FamiliarityBand` z testami                 | sesja zakończona; prace trwają  | brak     | 4 h              | nieznany       | archiwum 21/21, linki, kompilacja i `FamiliarityBandTest` 7/7    |
| 2026-10-10 | APP-01          | GET Dashboardu, local PostgreSQL, model obliczeń i agregacji, rozgrzewka, migracja wpisów `V5` oraz początek komendy zapisu | odczyt i obliczenia działają; `POST` pozostaje | brak | około 5 h łącznie | nieznany | pełny zestaw testów przeszedł przed `V5`; po `V5` przeszedł test integracyjny Dashboardu; komenda wpisu nie jest jeszcze obecna w źródłach |
| 2026-10-11 | APP-01          | skale i reguły `V6`–`V12`, transakcyjny POST, retry z fingerprintem, rollback, intensywność Dashboardu, personal bests i Details | backend obsługuje zapis oraz podstawowy odczyt Dashboard/Details; katalog i edycja pozostają | brak | około 7 h | nieznany | pełny `./mvnw test` potwierdzony po Details; pionowy test obejmuje PostgreSQL, Dashboard, personal bests, Details, retry, konflikt i rollback |

## Obserwacje do wykresów

Po ustaleniu jednostki w OPEN-18 zapisujemy na koniec pracy lub iteracji stan zakresu i pozostałą pracę. Do burnup dopisujemy każdą zmianę całego zakresu. Nie zmieniamy wcześniejszych obserwacji w celu poprawienia przebiegu wykresu; korektę oznaczamy osobno.

| Data       | Iteracja/przyrost           | Jednostka   | Zakres na start | Zmiana zakresu | Ukończone narastająco | Pozostałe     | Źródło                       |
| ---------- | --------------------------- | ----------- | --------------- | -------------- | --------------------- | ------------- | ---------------------------- |
| 2026-10-03 | przed planowaniem APP-01/02 | nieustalona | nieoszacowane   | nieoszacowane  | niezmierzone -------- | nieoszacowane | przegląd dokumentacji i kodu |

Interpretacja i bieżąca prognoza: [progress](progress.md). Zakres przyrostów: [roadmap](roadmap.md).
