# Archiwum materiałów źródłowych

Data kopii: 2026-10-03. Zachowano **21 plików** bajt po bajcie: wszystkie 16 plików z `docs` (w tym `.gitkeep`), trzy plany, dawne zasady i pierwotny README. Sumy SHA-256 i rozmiary znajdują się w [source-manifest.json](source-manifest.json).

Oryginały pozostają na miejscu w iteracji 1. Kopie są źródłem historii, nie bieżącym backlogiem ani dodatkowymi instrukcjami dla asystenta. Nie poprawiamy w nich treści, statusów, nazw ani linków, aby zachować wierny zapis. Stare linki mogą wskazywać ścieżki istniejące dawniej; poniższy indeks prowadzi do właściwych kopii. Aktywne odsyłacze znajdują się w `.ai` poza archiwum.

## Mapowanie dokumentów na bieżącą strukturę

| Źródło zachowane w całości                                                           | Bieżące miejsce i interpretacja                                                                                                                                                                       |
| ------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [CB_model](source-snapshot/docs/CB_model.md) --------------------------------------- | [domain](../business/domain.md), [database](../documentation/database.md), [api](../documentation/api.md); zachowane pola i reguły, model docelowy oddzielony od wdrożenia -------------------------- |
| [CB_climbing_effort_valuation](source-snapshot/docs/CB_climbing_effort_valuation.md) | [grading-and-scoring](../business/grading-and-scoring.md); pełne tabele, wzory, przykłady i propozycje statystyk ------------------------------------------------------------------------------------ |
| [CB_Backend](source-snapshot/docs/CB_Backend.md) ----------------------------------- | [backend](../documentation/backend.md), [architecture](../documentation/architecture.md), [roadmap](../project-management/roadmap.md); stare checklisty pozostają tutaj, status zweryfikowany z kodem |
| [CB_Front-web](source-snapshot/docs/CB_Front-web.md) ------------------------------- | [frontend-web](../documentation/frontend-web.md); zachowany plan odpowiedzialności i dalsze pomysły, priorytet według wrześniowych ustaleń ---------------------------------------------------------- |
| [CB_frontend-mobile](source-snapshot/docs/CB_frontend-mobile.md) ------------------- | [frontend-mobile](../documentation/frontend-mobile.md); pełny plan etapów i testów zachowany w źródle ----------------------------------------------------------------------------------------------- |
| [CB_project_decisions](source-snapshot/docs/CB_project_decisions.md) --------------- | [decisions](../documentation/decisions.md); zachowane wszystkie 17 ID i status odłożenia DEC-012 ---------------------------------------------------------------------------------------------------- |
| [CB_web_stage_1](source-snapshot/docs/CB_web_stage_1.md) --------------------------- | [training](../business/training.md), [frontend-web](../documentation/frontend-web.md), [api](../documentation/api.md); najnowsze wymagania z odróżnieniem propozycji -------------------------------- |
| [app-plan](source-snapshot/app-plan.md) -------------------------------------------- | [product](../business/product.md), [priorities](../project-management/priorities.md); stare pytania sprawdzone względem późniejszych decyzji -------------------------------------------------------- |
| [front-plan](source-snapshot/frontend-web/front-plan.md) --------------------------- | [frontend-web](../documentation/frontend-web.md); wcześniejsze nazwy score i pytanie o tworzenie sesji nie wyznaczają już zakresu ------------------------------------------------------------------- |
| [mobile-plan](source-snapshot/frontend-mobile/mobile-plan.md) ---------------------- | [frontend-mobile](../documentation/frontend-mobile.md), [development](../documentation/development.md); dawne uproszczenie EDL nie zastępuje modelu wyceny ------------------------------------------ |
| [dawne AGENTS](source-snapshot/.agents/AGENTS.md) ---------------------------------- | [collaboration](../context/collaboration.md); bieżące polecenie określa zakres zgody ---------------------------------------------------------------------------------------------------------------- |
| [pierwotny README](source-snapshot/README.md) -------------------------------------- | [README projektu](../../README.md); poprawiono status backendu i punkt wejścia do wiedzy ------------------------------------------------------------------------------------------------------------ |

## Wcześniejsze materiały z old-doc

| Plik                                                                                                     | Co zachowuje / co go zastąpiło                                                                           |
| -------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| [api_db_plan_iteracji](source-snapshot/docs/old-doc/api_db_plan_iteracji.md) --------------------------- | dawne decyzje API/DB, hierarchię i propozycje endpointów; obecny model oraz web-first mają pierwszeństwo |
| [Api_v2_kanban](source-snapshot/docs/old-doc/Api_v2_kanban.md) ----------------------------------------- | wcześniejsza checklista .NET, zasady współpracy, brak primary_discipline; nie dowodzi ukończenia Javy -- |
| [backend-plan](source-snapshot/docs/old-doc/backend-plan.md) ------------------------------------------- | wcześniejsze uzasadnienie modularnego monolitu, CQRS i planu Javy; zachowane szczegółowe propozycje ---- |
| [climbbetter_model_oceny_wspinaczki](source-snapshot/docs/old-doc/climbbetter_model_oceny_wspinaczki.md) | historyczny model i ślady przykładów z arkusza; nowsze tabele i DEC mają pierwszeństwo ----------------- |
| [climbbetter_wymagania_api_baza](source-snapshot/docs/old-doc/climbbetter_wymagania_api_baza.md) ------- | wcześniejsze wymagania danych i historii; nie utożsamiać z wdrożonym schematem ------------------------- |
| [frontend-product-design](source-snapshot/docs/old-doc/frontend-product-design.md) --------------------- | historyczne szkice ekranów, kontekst UX; nie kontrakt produktu ----------------------------------------- |
| [project_goals_02](source-snapshot/docs/old-doc/project_goals_02.md) ----------------------------------- | cele Training/Calendar/Areas, offline-first, snapshoty i historia; aktualny zakres opisuje product ----- |
| [spis_tresci](source-snapshot/docs/old-doc/spis_tresci.md) --------------------------------------------- | poprzedni indeks, którego ścieżki mogą być nieaktualne ------------------------------------------------- |

Zachowano także pusty `source-snapshot/docs/old-doc/.gitkeep`. Nie wykonywano ponownego odczytu arkusza zewnętrznego ani pełnego audytu archiwalnego .NET. Ich opisy w dawnych dokumentach pozostają historycznymi relacjami.

## Granice migracji

Iteracja 1 przeniosła aktywną wiedzę do tematycznych dokumentów i zachowała pełne źródła. Nie oznacza to wykonania każdej historycznej pozycji backlogu. DEC-019 ustala wersjonowanie `.ai` wraz z tym archiwum oraz pozostawienie oryginalnego `docs` poza Git do czasu usunięcia przez użytkownika. Iteracja 3 ponownie sprawdzi kompletność i stare lokalizacje. `.gitignore` nadal ignoruje oryginalne ścieżki, ale nie `.ai`.
