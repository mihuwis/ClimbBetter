# ClimbBetter — dokumentacja

Aktualizacja: 2026-11-09. Dokumentacja porównana z bieżącym kodem; wyniki wcześniejszych uruchomień mają własne daty.

**Działa lokalnie:** React → Java → PostgreSQL, CRUD sesji, obliczenia v3, Statistics (piramida i podsumowanie miesiąca/roku), Google/Firebase oraz podstawowy profil. **Produkcja i GitHub Actions są przygotowanym planem** — brak jeszcze workflowów i obrazu aplikacji. Flutter pozostaje prototypem.

| Temat                    | Gdzie czytać                                                                                                                                                            |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Zasady pracy             | [Współpraca](context/collaboration.md), [konwencje](context/conventions.md)                                                                                             |
| Stan i dalsza praca      | [Postęp](project-management/progress.md), [priorytety](project-management/priorities.md), [roadmapa](project-management/roadmap.md)                                     |
| Produkt i sesja          | [Produkt](business/product.md), [domena](business/domain.md), [rejestrowanie](business/training.md)                                                                     |
| Wzory i osiągnięcia      | [Model obciążenia](business/grading-and-scoring.md), [piramida](business/achievement-pyramid.md)                                                                        |
| Budowa aplikacji         | [Architektura](documentation/architecture.md), [backend](documentation/backend.md), [web](documentation/frontend-web.md), [mobile](documentation/frontend-mobile.md)    |
| Kontrakty i dane         | [API](documentation/api.md), [baza](documentation/database.md), [katalog](documentation/catalog-climb-scenarios.md)                                                     |
| Statistics               | [Piramida: API](documentation/achievement-pyramid-backend.md), [Training summary](documentation/period-training-summary.md), [poziom v3](documentation/recency-load.md) |
| Uruchomienie i logowanie | [Development](documentation/development.md), [Firebase i konta modelowe](documentation/firebase-authentication-local.md)                                                |
| Produkcja i CI/CD        | [Environments plan](documentation/Environments_plan.md)                                                                                                                 |
| Decyzje i dowody         | [Rejestr decyzji](documentation/decisions.md), [weryfikacja](documentation/backend-verification.md), [historia prac](project-management/work-log.md)                    |
| Odtwarzanie i materiały  | [Historia treningów](documentation/training-history-export.md), [notatki do pracy](documentation/engineering-thesis-notes.md), [archiwum](archive/README.md)            |

Każdy temat ma jedno źródło: wzory w modelu, zadania w priorytetach, pytania w rejestrze decyzji, uruchomienie w development. Plan nie oznacza implementacji; obecność testu nie oznacza jego wykonania. Pełna dokumentacja sprzed skrócenia jest w [kopii z 2026-11-09](archive/documentation-2026-11-09/README.md). Historyczne kopie dawnej dokumentacji aplikacji i stare plany nie są bieżącym backlogiem.

Obecny `docs/` jest aktywnym warsztatem pracy inżynierskiej. [Przegląd i nowe wersje rozdziałów](../docs/aktualizacja_dokumentow_2026-11-09.md) oraz [spis treści 03](../docs/spis_tresci_szkic_03.md) dotyczą pracy dyplomowej; nie utożsamiamy ich z dawnymi plikami zachowanymi w `archive/source-snapshot/docs`. Oryginały rozdziałów pozostają bez zmian.
