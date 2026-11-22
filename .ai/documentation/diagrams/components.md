# Komponenty

Aktualizacja: 2026-11-09. Stan z kodu; produkcja i mobile offline pozostają planami.

```mermaid
flowchart LR
    U[Użytkownik] --> W[React: szkic i widoki]
    W -->|Google Sign-In| F[Firebase Authentication]
    W -->|HTTPS / Bearer ID token| I[identity: Security i UID → UUID]
    I --> T[training: CRUD i kalkulatory]
    I --> C[catalog: miejsca i wspinaczki]
    I --> R[reporting: Dashboard i Statistics]
    I -->|weryfikacja Admin SDK| F
    I --> DB[(PostgreSQL V24)]
    T --> DB
    C --> DB
    R --> DB
    M[Flutter: prototyp na mockach]
```

Local ma osobny Auth Emulator/bazę modeli. Obecny Compose uruchamia tylko PostgreSQL. [Opis modułów](../architecture.md), [plan hostingu i CI/CD](../Environments_plan.md).
