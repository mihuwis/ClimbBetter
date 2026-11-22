# Architektura systemu

Status: stan odczytany z kodu i osobno opisany projekt docelowy; 2026-10-11. Materiał do szerszego uzasadnienia decyzji zbierają [notatki do pracy inżynierskiej](engineering-thesis-notes.md).

## Obecne repozytorium

| Katalog            | Rola i rzeczywisty stan                                                              |
| ------------------ | ------------------------------------------------------------------------------------ |
| `backend/`         | Java/Spring Boot: Dashboard GET, transakcyjny POST sesji, model obliczeń, resolvery oraz migracje `V1`–`V11` |
| `backend-net/`     | archiwalny prototyp .NET jako referencja; działania nie zweryfikowano w tej iteracji |
| `frontend-web/`    | React/TypeScript/Vite, ekrany oparte na mockach ------------------------------------ |
| `frontend-mobile/` | Flutter, prototyp UI i formularz w pamięci ----------------------------------------- |
| `infra/`           | Compose z trwałym lokalnym PostgreSQL 17.6 na porcie hosta `5433`                   |
| `.ai/`             | bieżąca wiedza, decyzje, dokumentacja i zarządzanie pracą -------------------------- |

Potwierdzono przepływ HTTP `POST → Java → PostgreSQL → GET Dashboard` na poziomie testu integracyjnego backendu. Frontend nadal korzysta z mocków i nie jest jeszcze podłączony do tego API. Diagram [komponentów](diagrams/components.md) odróżnia stan od planu.

## Plan docelowy

Jeden modularny monolit Java, jeden projekt Maven i jedna baza PostgreSQL. React i Flutter korzystają z tego samego API oraz tego samego modelu obliczeń. Nie kopiujemy mechanicznie architektury archiwalnego .NET.

| Moduł Java  | Docelowa odpowiedzialność                                                   |
| ----------- | --------------------------------------------------------------------------- |
| `identity`  | stabilna tożsamość i dostarczenie bieżącego użytkownika ------------------- |
| `catalog`   | Area, Sector, Climb, Grade, dostępność danych katalogowych ---------------- |
| `training`  | sesje, wpisy, profile, reguły stylu, poziom odniesienia, obliczenia i zapis |
| `reporting` | przekroje danych dla Dashboardu, kalendarza i analityki ------------------- |
| `shared`    | małe elementy wspólne; obecnie zegar UTC ---------------------------------- |

Moduły udostępniają wąskie publiczne API, a implementację chowają w `internal`. Istnieją deklaracje pakietów i test `ApplicationModules.verify()`. `reporting` udostępnia zapytanie Dashboardu, a `training` komendy wejściowe; serwis transakcyjny zapisu nie jest jeszcze zaimplementowany.

## Wzorce i powody użycia

| Wzorzec / zasada                      | Po co                                                       | Stan                                           |
| ------------------------------------- | ----------------------------------------------------------- | ---------------------------------------------- |
| Modularny monolit ------------------- | granice funkcji bez kosztu osobnych usług ----------------- | szkielet pakietów i test Modulith istnieją --- |
| Wstrzykiwany `Clock` ---------------- | kontrolowany czas i testowanie reguł ---------------------- | zaimplementowany zegar UTC i test strefy ----- |
| Lekki CQRS -------------------------- | osobne operacje zapisu i odczyty dopasowane do ekranów ---- | odczyt Dashboardu działa; zapis jest w budowie |
| Fasada domenowa --------------------- | jedno wejście do kalkulatorów i ukrycie szczegółów modelu -- | `TrainingSessionService` używa `TrainingMetricsFacade` podczas zapisu |
| Elementy Clean Architecture -------- | izolacja domeny obliczeń od HTTP i SQL -------------------- | częściowo wdrożone; nie jest to pełna Clean Architecture |
| Aggregate root `TrainingSession` ---- | wspólna spójność sesji, wpisów, snapshotów i summary ------ | specyfikacja, brak implementacji ------------- |
| Snapshot historycznych obliczeń ----- | odczyt dawnych wyników bez zależności od obecnych słowników | schemat `V5`/`V9`/`V10`; zapis przez API oczekuje na wdrożenie |
| Commandy i projekcje odczytu -------- | API niezależne od encji JPA i układu tabel ---------------- | komendy zapisu i projekcja Dashboardu istnieją |
| Idempotencja z ID klienta ----------- | ponowienie zapisu bez drugiej sesji ----------------------- | fundament w schemacie; obsługa `POST` pozostaje do wdrożenia |
| Optimistic locking i revisions ------ | jawne konflikty edycji i audyt zmian ---------------------- | plan ----------------------------------------- |
| Lokalna baza i outbox mobile -------- | trwałość bez sieci i powtarzalna synchronizacja ----------- | późniejszy plan ------------------------------ |

Nie zaplanowano na start osobnej bazy odczytu, event sourcingu, brokera, Redis ani generycznego command busa. Proste przypadki użycia mają własne handlery. Odczyt może używać projekcji zamiast odtwarzania pełnego agregatu.

## Najważniejszy przepływ

Web utrzymuje roboczą sesję; `Finish session` przekazuje uporządkowane fakty. Java sprawdza użytkownika, katalog i historię, oblicza wyniki oraz atomowo zapisuje trening. Read modele dostarczają Dashboard i Details. Wcześniejsze wpisy tej samej sesji uczestniczą w historii następnych. Opcjonalny preview ma używać tych samych reguł bez zapisu; jego włączenie wymaga decyzji.

Schematy: [flow](diagrams/record-session-flow.md), [sekwencja](diagrams/save-session-sequence.md), [ERD](diagrams/data-model.md). Kontrakt i transakcje: [api](api.md), [database](database.md). Źródła: [plan backendu](../archive/source-snapshot/docs/CB_Backend.md), [model](../archive/source-snapshot/docs/CB_model.md), [nowsze ustalenia web](../archive/source-snapshot/docs/CB_web_stage_1.md).
