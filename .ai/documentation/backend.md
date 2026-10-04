# Backend Java

Status: bootstrap częściowo wykonany; przegląd 2026-10-03. Uruchomiono pierwszy celowany test domenowy; nie uruchamiano jeszcze pełnego zestawu testów ani aplikacji.

## Stan potwierdzony w repozytorium

| Element             | Dowód                                                                                                                                      |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| Maven i Spring Boot | [pom.xml](../../backend/pom.xml): Java 21, Spring Boot 4.1.0, BOM Modulith 2.1.0 --------------------------------------------------------- |
| Maven Wrapper ----- | `mvnw`, `mvnw.cmd`, konfiguracja dystrybucji Maven 3.9.16 -------------------------------------------------------------------------------- |
| Start aplikacji --- | [ClimbbetterBackendApplication](../../backend/src/main/java/pl/climbbetter/ClimbbetterBackendApplication.java) --------------------------- |
| Moduły ------------ | `package-info.java` w `identity`, `catalog`, `training`, `reporting`, `shared` ----------------------------------------------------------- |
| Profile i JPA ----- | `application.yml`, `application-local.yml`, testowy `application-test.yml`; `open-in-view: false`, `ddl-auto: validate` ------------------ |
| Czas -------------- | [TimeConfiguration](../../backend/src/main/java/pl/climbbetter/shared/internal/time/TimeConfiguration.java) udostępnia `Clock.systemUTC()` |
| Testy ------------- | context load, strefa UTC, `ApplicationModules.verify()` i konfiguracja Testcontainers PostgreSQL ----------------------------------------- |
| Zależności -------- | MVC, validation, JPA, Flyway/PostgreSQL, Actuator, Modulith oraz testowe startery -------------------------------------------------------- |

Wersje powyżej opisują pliki repozytorium, nie ocenę aktualności bibliotek. Istnienie zależności Actuator nie oznacza sprawdzenia odpowiedzi health. Testcontainers używa obecnie `postgres:latest`; wersja docelowa jest otwarta (`OPEN-13`).

Pierwszy potwierdzony element domeny `training` to `FamiliarityBand.fromPriorContactCount`. Mapuje granice `0`, `1–10`, `11–20` i `21+`, a ujemną liczbę odrzuca. Celowany `FamiliarityBandTest` zakończył się wynikiem 7 testów, 0 failures i 0 errors. To dowód działania tej jednej reguły, nie całego modułu treningowego.

## Czego jeszcze nie ma w Javie

Nie znaleziono biznesowych kontrolerów i endpointów, DTO sesji, agregatów treningowych, encji JPA, kalkulatora, migracji SQL, seedów ani OpenAPI/springdoc. `infra` nie zawiera Compose. Nie potwierdzono połączenia z istniejącą lokalną bazą użytkownika.

Stary `CB_Backend.md` jednocześnie nazywał katalog pustym i odhaczał bootstrap. Bieżący opis opiera się na kodzie; dawne checklisty pozostają w archiwum.

## Bieżący zakres backendu

Odtwarzamy Javę dla webowego przepływu z [priorytetów](../project-management/priorities.md): katalog i słowniki formularza, atomowy zapis ukończonej sesji, odczyt listy oraz szczegółów. Nie zaczynamy od endpointu `Start session`. Dashboard wymaga Adjusted Load i maksymalnych ukończonych wycen per skala; Details wymagają stabilnych ID, kolejności i snapshotów prób.

Zapis ma uwzględnić powtórzony request, rollback całego treningu przy błędzie, rozwiązywanie ID klienta, prywatne drafty katalogowe, historię poprzedzającą każdy wpis i konflikt OS/Flash. Mechanizm współbieżności i dokładny kontrakt są jeszcze do ustalenia.

## Granice implementacji

- Domena i kalkulator powinny być testowalne bez Springa i bazy.
- Command handler wyznacza granicę transakcji; query zwraca projekcję dla odbiorcy.
- Nie serializujemy encji JPA jako odpowiedzi HTTP.
- Flyway ma tworzyć schemat; Hibernate wyłącznie go waliduje.
- `CurrentUserProvider` jest planowanym portem. Dev-user może działać tylko w local/test; `userId` nie pochodzi ze zwykłego body requestu.
- Reguły dziesiętne, wynik i snapshot odpowiadają [specyfikacji wyceny](../business/grading-and-scoring.md).

## Dowód ukończenia pierwszego przyrostu

Migracje budują pusty PostgreSQL; kalkulator zgadza się z zatwierdzonymi przykładami. Test integracyjny zapisuje próbę i przejście tej samej drogi, sprawdza wyniki i idempotentne ponowienie, a błędny wpis powoduje rollback. GET zwraca listę i pełne dane historyczne; frontend prezentuje ten sam wynik po ponownym otwarciu.

To kryteria przyszłej implementacji, nie lista testów wykonanych teraz. Pełne wcześniejsze checklisty zachowano w [CB_Backend](../archive/source-snapshot/docs/CB_Backend.md); ich kolejność Flutter-first została zastąpiona przez web-first.
