# Komponenty systemu

Data: 2026-10-03. Diagramy ręcznie opracowane z kodu i specyfikacji. Pierwszy opisuje obecność komponentów; drugi jest planem docelowym.

## Stan odczytany z repozytorium

```mermaid
flowchart LR
    Web[React: prototyp ekranow] --> WM[Mocki web]
    Mobile[Flutter: prototyp UI] --> MM[Mocki i stan widgetu]
    Java[Java: bootstrap Spring Boot] --> Clock[Clock UTC]
    Java --> Config[Profile i konfiguracja JPA]
    Tests[Kod testow Java] --> TC[Konfiguracja Testcontainers PostgreSQL]
    Legacy[Archiwalny backend .NET]
```

Brak strzałki frontend → Java oznacza brak potwierdzonej integracji treningowej. Konfiguracja testów nie dowodzi ich aktualnego powodzenia.

## Architektura docelowa

```mermaid
flowchart LR
    Browser[React + lokalny szkic] --> API[Java REST API]
    Phone[Flutter + lokalna baza i outbox] --> API
    subgraph Monolith[Modularny monolit Java - plan]
        API --> Identity[identity]
        API --> Catalog[catalog]
        API --> Training[training]
        API --> Reporting[reporting]
        Training --> Calculator[Kalkulator i snapshoty]
        Training --> Catalog
        Training --> Identity
    end
    Catalog --> DB[(PostgreSQL)]
    Training --> DB
    Reporting --> DB
```

Moduły korzystają ze wspólnej bazy, ale zachowują własność danych i publiczne granice. Dokładne porty odczytu między modułami dopracujemy w implementacji. Pełny outbox mobile jest późniejszym przyrostem.

Źródło: [architecture](../architecture.md).
