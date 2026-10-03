# UML: sekwencja końcowego zapisu

Data: 2026-10-03. Projekt logiczny; endpointy, nazwy klas i dokładny mechanizm współbieżności nie zostały jeszcze wdrożone ani zamrożone. OPEN-12 określi, jak chronimy historię podczas transakcji.

```mermaid
sequenceDiagram
    actor User as Uzytkownik
    participant Web as Web / lokalny szkic
    participant API as Java API
    participant App as Przypadek uzycia zapisu
    participant Calc as Kalkulator domenowy
    participant DB as PostgreSQL
    User->>Web: Finish session
    Web->>API: Ukonczona sesja + client ID + uporzadkowane wpisy
    API->>App: Fakty i tozsamosc uzytkownika
    App->>DB: Sprawdz idempotencje i odczytaj historie
    alt Identyczne zadanie juz zapisane
        DB-->>App: Istniejaca sesja i wynik
        App-->>API: Poprzedni wynik bez ponownego doliczenia prob
    else Nowa sesja
        Note over App,DB: Jedna transakcja; polityka ochrony historii do ustalenia
        App->>DB: Rozwiaz katalog, client ID i poziom odniesienia
        loop Kazdy wpis wedlug entryOrder
            App->>Calc: Fakty + wczesniejsza historia + wczesniejsze wpisy szkicu
            Calc-->>App: Wynik, snapshot lub blad
        end
        alt Dane poprawne
            App->>DB: Utrwal katalog, sesje, wpisy, snapshoty i summary
            DB-->>App: Commit
            App-->>API: Zapisana sesja i mapowanie ID
        else Konflikt lub bledny wpis
            App->>DB: Rollback
            App-->>API: Blad wymagajacy korekty lub potwierdzenia
        end
    end
    API-->>Web: Potwierdzenie albo blad
    Note over Web: Szkic pozostaje do potwierdzenia sukcesu
    User->>Web: Otworz Dashboard / Details
    Web->>API: Odczytaj liste / szczegoly
    API->>DB: Odczyt zapisanych wartosci
    DB-->>API: Summary, uporzadkowane wpisy i snapshoty
    API-->>Web: Read model
```

Inna treść pod ID ukończonej sesji nie jest nowym utworzeniem — plan zakłada konflikt i osobny proces edycji. Równoległe różne sesje wymagają dodatkowej ochrony poza unique client ID. Preview, jeśli zostanie przyjęty, nie zapisuje sesji, prób ani draftów katalogu.

Źródło: [api](../api.md), [backend](../backend.md).
