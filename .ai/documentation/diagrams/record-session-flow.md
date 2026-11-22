# Record session

Aktualizacja: 2026-11-09. Wdrożony przepływ web, źródło: formularz sesji i lokalny szkic.

```mermaid
flowchart TD
    A[Google Sign-In i ukończony profil] --> B[Wybierz jedno Area]
    B --> C[Start: początek utrwalony lokalnie]
    C --> D[Dodaj rozgrzewkę, próbę lub przejście]
    D --> E{Koniec treningu?}
    E -->|nie| D
    E -->|tak| F[Stop: koniec utrwalony lokalnie]
    F --> G[Przegląd i korekty]
    G --> H[Finish: atomowy POST]
    H --> I{Potwierdzony sukces?}
    I -->|tak| J[Usuń szkic i pokaż zapisaną sesję]
    I -->|błąd lub brak odpowiedzi| K[Zachowaj szkic i dokładny request]
    K --> G
```

Bez pauzy i serwerowego Start. Ręczna sesja pomija pomiar Start/Stop, podając czasy. [Zasady i powtarzanie](../../business/training.md).
