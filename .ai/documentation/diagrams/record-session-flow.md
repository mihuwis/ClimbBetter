# Flow: Record session

Data: 2026-10-03. Projekt przepływu z ustaleń 2026-09-22 i DEC-021; nie opis działającego UI. Utrwalenie szkicu jest wymagane. Backendowy preview oraz polityka wielu kart i liczby szkiców pozostają w OPEN-04.

```mermaid
flowchart TD
    Dashboard[Dashboard] --> Add[+ Add session]
    Add --> Intent{Intencja}
    Intent --> Record[Record session]
    Intent -.-> Finished[Add finished session - pozniej]
    Intent -.-> Single[Add single climb - do ustalenia]
    Record --> Start[Start: lokalny poczatek czasu]
    Start --> Draft[Robocza sesja we frontendzie]
    Draft --> Entry[Dodaj probe albo przejscie]
    Entry --> Draft
    Draft --> Persist[Odzyskiwalny zapis lokalny]
    Draft -.-> Preview[Backend preview bez zapisu - propozycja]
    Draft --> Stop[Stop: zapamietaj koniec czasu]
    Stop --> Review[Sprawdz i popraw dane]
    Review --> Finish[Finish session: wyslij caly trening]
    Finish --> Validate{Walidacja i zapis backendu}
    Validate -->|blad lub brak odpowiedzi| Keep[Zachowaj szkic; popraw albo ponow]
    Keep --> Review
    Validate -->|potwierdzony sukces| Saved[Trwala sesja + snapshoty]
    Saved --> Feed[Karta Dashboardu]
    Feed --> Details[Details: wspinaczki i ich proby]
```

Start nie tworzy sesji w backendzie. Brak pauzy. Stop nie zapisuje treningu na serwerze; czas sprawdzania danych po Stop nie wydłuża sesji. Retry zachowuje stabilne ID. Przy utracie odpowiedzi backend może już mieć zapis — ponowienie ma go rozpoznać.

Źródło: [training](../../business/training.md).
