# Architektura

Aktualizacja: 2026-11-09. Źródła: bieżący kod, migracje V1–V24 i [decyzje](decisions.md).

System działa jako modularny monolit Java/Spring Boot z PostgreSQL. React korzysta z rzeczywistego API; Flutter jest osobnym prototypem na mockach. `backend-net` zachowuje historyczny prototyp .NET. Compose uruchamia obecnie tylko lokalną bazę.

| Moduł Java  | Wdrożona odpowiedzialność                                                            |
| ----------- | ------------------------------------------------------------------------------------ |
| `identity`  | Firebase Admin, Spring Security, mapowanie UID → UUID, profil, `CurrentUserProvider` |
| `catalog`   | miejsca, sektory, wspinaczki, wyceny/aliasy i widoczność katalogu                    |
| `training`  | atomowy CRUD, historia kontaktów, obliczenia v3, snapshoty i revisions               |
| `reporting` | Dashboard/personal bests, piramida, podsumowanie miesiąca/roku                       |
| `shared`    | zegar UTC, konfiguracja HTTP i OpenAPI                                               |

Publiczne API modułów jest oddzielone od `internal`; granice sprawdza Spring Modulith. Lekki CQRS rozdziela komendy od projekcji ekranów w tej samej bazie. Odczyty i zapis korzystają z jawnego SQL przez `JdbcClient`; obecność JPA w zależnościach nie oznacza modelu opartego na encjach.

Zapis: lokalny szkic → Bearer Firebase → wewnętrzny UUID → walidacja katalogu/historii → kalkulatory → transakcja sesji, wpisów i snapshotów. `TrainingMetricsFacade` ukrywa kalkulatory. GET zwraca utrwalone wyniki, raporty agregują snapshoty. Retry, wersja rekordu i audyt chronią różne rodzaje zmian.

To nie jest pełna Clean Architecture: resolvery treningu odczytują również tabele katalogu. Nie ma brokera, event sourcingu ani osobnej bazy raportowej. Trwały `UserGradeReference`, mobile offline i outbox pozostają planami. Produkcja z GitHub Actions/GHCR: [Environments plan](Environments_plan.md).

Diagramy: [komponenty](diagrams/components.md), [Record session](diagrams/record-session-flow.md), [sekwencja zapisu](diagrams/save-session-sequence.md), [ERD](diagrams/data-model.md). Uzasadnienia do pracy: [notatki](engineering-thesis-notes.md).
