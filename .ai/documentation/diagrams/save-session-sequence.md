# Sekwencja zapisu sesji

Aktualizacja: 2026-11-09. Wdrożony POST; pełna ochrona kolejności różnych równoległych sesji pozostaje w OPEN-12.

```mermaid
sequenceDiagram
    participant W as React / szkic
    participant I as Security + identity
    participant F as Firebase Admin
    participant T as TrainingSessionService
    participant C as Kalkulatory v3
    participant D as PostgreSQL
    W->>I: POST sesji + client IDs + Bearer
    I->>F: Zweryfikuj token i stan konta
    F-->>I: Poprawna tożsamość albo błąd
    I->>D: Rozpoznaj UUID i ukończenie profilu
    I->>T: Komenda dla bieżącego UUID
    T->>D: Sprawdź clientSessionId i fingerprint
    alt Identyczny zapis istnieje
        D-->>T: Zapisany wynik
    else Nowa komenda
        Note over T,D: Jedna transakcja, błąd oznacza rollback
        T->>D: Rozwiąż katalog, historię i poziom v3
        loop Wpisy według entryOrder
            T->>C: Fakty, wcześniejsze kontakty i reguły
            C-->>T: Wyniki i snapshot
        end
        T->>D: Zapisz katalog, sesję, wpisy i summary
        D-->>T: Commit
    end
    T-->>W: 201, sessionId i Location albo błąd
    Note over W: Szkic znika dopiero po potwierdzeniu sukcesu
```

Niepoprawna tożsamość/profil przerywają przepływ przed operacją biznesową. Inna treść pod istniejącym client ID daje konflikt. PUT dodatkowo sprawdza wersję i zapisuje rewizję przed/po; GET nie liczy ponownie. [Kontrakt](../api.md).
