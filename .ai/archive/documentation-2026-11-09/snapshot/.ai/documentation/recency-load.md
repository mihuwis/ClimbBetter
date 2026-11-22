# Adjusted Load — aktualność poziomu odniesienia

Data: 2026-11-04. Status: backend v3, migracja V23 local i zgodność kontraktu web wdrożone; 176 testów backendu, build/lint/24 model web oraz realny browser CRUD v3 przeszły. Po restarcie użytkownika wykonano właściwy replay **16 sesji/133 wpisów** na8080, potwierdzając wszystkie wejścia, wyceny, punkty, intensywności i nowe obliczenia. Dashboard16 i 12 widoków piramid sprawdzone. Decyzje biznesowe: DEC-047; wzory mają jedno źródło w [modelu wyceny](../business/grading-and-scoring.md#aktualność-poziomu-odniesienia--model-v3).

## Zasady

Użytkownik zatwierdził zanik kwadratowy do zera w ciągu 2 lat (730 dni), poziom oparty na osiągnięciach bez wędki oraz zachowanie maksimum dla próbki 1–9 różnych wspinaczek. Przy małej próbce pozostaje skok, kiedy maksimum wypada z okna; próg 10 także może zmienić wynik skokowo. Wagi nie dotyczą wyceny drogi, punktów ani literalnej liczby osiągnięć w piramidzie. Raportowany Pyramid Support zachowuje dotychczasowe reguły; jego ewentualne ważenie czasem to osobny etap. Banister nie jest częścią wdrożenia.

## Przepływ obliczenia

1. Dla daty i początku zapisywanej sesji backend wyszukuje wcześniejsze, nieusunięte kwalifikujące przejścia tego użytkownika. Osobno Font i French; droga oraz obwód dzielą skalę French. Próby, wędka, przyszłe daty i późniejszy start tego samego dnia nie są dowodami poziomu.
2. W granicach horyzontu wybiera najnowsze kwalifikujące przejście każdego `climbId`, z jego historycznym snapshotem wyceny. Powtórzenie odświeża datę, ale nie zwiększa sample size. To celowo inny reprezentant niż pierwsze przejście używane do grupowania literalnej piramidy.
3. Dla 1–9 różnych wspinaczek stosuje maksimum bez osłabiania go wagami. Dla co najmniej 10 działa ważony poziom szczytowy opisany w modelu. Brak osiągnięć oznacza jawny fallback z bieżącej sesji.
4. Ułamkowy poziom odniesienia daje ułamkową różnicę do niezmienionej wyceny. Mnożnik jest interpolowany między sąsiednimi istniejącymi regułami, ograniczając wyłącznie odczyt kalibracji do przedziału −6…+5. Zaokrąglenie mnożnika do 4 miejsc HALF_UP następuje przed obliczeniem loadu, zgodnie z precyzją jego snapshotu w bazie.
5. Wynik i źródła zostają zamrożone w pojedynczym wpisie. Odczyt i identyczny retry nie przeliczają przeszłości. Edycja sesji używa v3, wykluczając jej stare wpisy z własnej historii. Dodanie wstecznej sesji nie aktualizuje automatycznie późniejszych zapisów. Znajomość i mnożniki stylu zachowują dotychczasowe reguły; zmiana wygładza tylko odniesienie poziomu.

## Kod i dane

`RecencyWeight` liczy wagę, `RecencyWeightedLevelEstimator` agreguje dowody, `ClimberLevelResolver` wybiera kwalifikującą historię i stosuje wyjątek małej próbki. `RelativeEffortInterpolator` obsługuje ułamkową różnicę. `TrainingSessionService` łączy je z istniejącym kalkulatorem intensywności/profili/cruxów.

`RecencyLoadSettings` ma domyślne `horizonDays=730`, `includeTopRope=false`, `weightSmallSample=false`. Wersja estymatora zapisana przy wyniku zawiera także dwa przełączniki kwalifikacji, aby przyszła zmiana konfiguracji nie zacierała użytych reguł.

V23 rozszerza dozwolone wersje o v3 i dodaje siedem nullable kolumn snapshotu. Dla ocenianego wpisu v3 są wymagane: efektywny indeks, efektywna różnica, źródło, wersja estymatora, horyzont, liczba różnych wspinaczek i użyta masa historii. Rozgrzewki i starsze v1/v2 mają te pola NULL. Stary całkowity `currentLevelIndex` jest dolnym sąsiadem efektywnego indeksu, zachowanym dla zgodności; rzeczywiste obliczenia v3 używają pól efektywnych.

API dodaje metadane do `calculation`, bez zmiany pól wejściowych POST/PUT. Typy web przyjmują v3 i nowe opcjonalne pola; nie dokładamy technicznych kontrolek do formularza. Szczegóły: [API](api.md), [baza](database.md).

## Uruchomienie i odtworzenie historii

Nowy backend po restarcie stosuje Flyway V23. Migracja dodaje strukturę i ograniczenia; nie przelicza starych wpisów. Użytkownik wykonał restart; OpenAPI8080 potwierdziło nowy kontrakt przed właściwym importem.

Kopia 16 aktywnych sesji/133 wpisów Mihu pozostaje w ignorowanym `.local-backups`. Właściwe przeliczenie wykonano przez chronologiczny replay komend API do v3, bez SQL INSERT starych wyników. Oryginalne snapshoty, katalog i inne konta zachowane. Dziennik importu i live-v3-import-verification.json są obok głównego replay-inputs.json; nie ponawiać importu z nowymi identyfikatorami. [Instrukcja i stan danych](training-history-export.md).

Dowody testów i stan lokalnej migracji: [weryfikacja backendu](backend-verification.md). Model jest heurystyką obciążenia, nie pomiarem zdolności fizjologicznych.
