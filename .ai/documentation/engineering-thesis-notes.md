# Materiał do pracy inżynierskiej

Aktualizacja: 2026-11-09. Notatki z implementacji, nie gotowy rozdział. Źródła: [architektura](architecture.md), [decyzje](decisions.md), [datowane testy](backend-verification.md).

Aktywne materiały pracy są w `docs/`: [rozdział 1/02](../../docs/rozdzial_1_wersja_02.md), [rozdział 2/02](../../docs/rozdzial_2_wersja_02.md), [spis treści 03](../../docs/spis_tresci_szkic_03.md) i [raport aktualizacji](../../docs/aktualizacja_dokumentow_2026-11-09.md). Te wersje zachowują oryginały autora, aktualizując statystyki, model, Firebase i osobny widok planowanego wdrożenia.

| Problem                           | Rozwiązanie i dowód                                                            | Ograniczenie                                                     |
| --------------------------------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------- |
| Podział systemu przy małej skali  | Modularny monolit; pięć modułów i `ApplicationModulesTests`                    | jeden proces/baza; resolvery przekraczają własność tabel modułów |
| Inny model komend i ekranów       | Lekki CQRS, komendy treningu, projekcje Dashboard/Statistics i JdbcClient      | więcej typów i jawnego SQL; bez osobnej bazy odczytu             |
| Historia po zmianie algorytmu     | Snapshoty v1/v2/v3, kalibracje, revisions i Flyway V1–V24                      | większy zapis i konieczność zgodności odczytu starszych wersji   |
| Powtórzenie po utracie odpowiedzi | Stabilne client IDs, fingerprint, wersja edycji i odzyskiwalny szkic           | nie rozwiązuje pełnej kolejności różnych równoległych sesji      |
| Ocena pracy a osiągnięcia         | Load obejmuje próby/wędkę, raport osiągnięć je wyklucza; oddzielne estymatory  | heurystyczna kalibracja, nie model fizjologiczny                 |
| Aktualność odniesienia            | V3: zanik dowodów, najnowszy Climb, snapshot pochodzenia i interpolacja        | maksimum małej próbki oraz próg 10 mogą skakać                   |
| Tożsamość i prywatna historia     | Firebase Admin/Security, issuer/UID → UUID, dwa konta i testy izolacji         | zależność od zewnętrznego Auth; ADMIN/prod nie są gotowe         |
| Zgodność bazy i czasu             | PostgreSQL Testcontainers, Flyway, wstrzykiwany Clock UTC i lokalna data sesji | Docker wymagany; jakość czasu nadal do doprecyzowania            |

Przykłady do opisania: próba → przejście tej samej wspinaczki; retry i korekta z audytem; różnica między pierwszym reprezentantem piramidy, najlepszym stylem okresu i najnowszym dowodem v3; replay bez przepisywania wycen; mapowanie Google bez utraty UUID/historii.

Diagramy i wzory mają własne źródła: [komponenty/ERD/przepływy](diagrams/), [model](../business/grading-and-scoring.md). Plan produkcji/CI/CD i mobile offline opisujemy jako rozwój. Nie przedstawiamy systemu jako pełnej Clean Architecture, gotowego wdrożenia prod ani zweryfikowanej naukowo oceny regeneracji.
