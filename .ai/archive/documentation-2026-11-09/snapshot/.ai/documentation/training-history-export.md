# Eksport i odtworzenie historii treningowej

Data: 2026-11-04. Status: eksport, weryfikacja, odwracalne usunięcie i **właściwy replay zakończone**. Po restarcie backendu do v3 odtworzono 16 sesji/133 wpisy przez8080, z ponownym obliczeniem wyników i zachowaniem wycen, punktów, intensywności oraz wszystkich wejść. [Nowy model i weryfikacja](recency-load.md).

## Dwa rodzaje odtworzenia

Aktualizacja Firebase Auth, 2026-11-08: wdrożenie zachowuje UUID i zakłada historię bez replay. Nowa kopia `.local-backups/mihu-002/2026-11-08-before-firebase-auth/` obejmuje52 sesje/415 wpisów/46 revisions, aktywnych17/137; dokładny SQL restore przeszedł. Archive/replay wysyłają teraz Bearer pobrany przez `api_auth.py` z `CB_FIREBASE_ID_TOKEN_FILE` lub `CB_FIREBASE_ID_TOKEN`; nie zapisują tokenu w dzienniku. SQL restore dotyczy historii, nie odtwarzania kont Firebase. Przy pełnej odbudowie środowiska mapowanie UID należy zachować lub odtworzyć osobno. Kopia16/133 z2026-11-04 pozostaje historyczna. [Wdrożenie auth i dalszy odbiór Mihu](firebase-authentication-local.md).

Aktualizacja po restarcie: użytkownik002 ma ponownie **16 aktywnych sesji/133 wpisy**, wszystkie v3. GET każdego wpisu potwierdził daty/czasy, nazwy, style, ruchy, climbId, wyceny, punkty i intensywności zgodne z kopią oraz odtwarzalny load z nowych snapshotów. Źródła poziomu: 17 CURRENT_SESSION_FALLBACK, 47 LOW_SAMPLE_MAX, 53 RECENCY_WEIGHTED_PEAK. Pozostałe 16 wpisów to zerowe rozgrzewki. Hashe wcześniejszych 35 sesji/278 wpisów/46 revisions, katalogu, wycen i użytkowników nie zmieniły się. Nie wskrzeszono 19 wcześniej usuniętych sesji.

Dashboard zwraca wszystkie 16 sesji. Sprawdzono 12 widoków piramidy: RP ma 23 baldy i 4 drogi/obwody (27 łącznie), OS ma 10 baldów i zero dróg/obwodów. Oba okresy dają te same liczby dla tego zestawu. Dowody: live-v3-before-import.json, live-v3-import-verification.json i główny **replay-inputs.replay-state.json** w prywatnym folderze kopii. Zachować dziennik oraz client IDs; wcześniejsze akapity o oczekiwaniu na restart/import opisują poprzednie etapy.

Użytkownik poprosił o zabezpieczenie swoich przejść przed zmianą Adjusted Load, usunięcie ich i późniejsze przeliczenie. Sama kopia INSERT przywracałaby również stare wyniki, dlatego przygotowano:

- wierną kopię SQL oraz surowy snapshot, obejmujące wszystkie sesje i revisions;
- wejściowe komendy aktywnych sesji do zwykłego POST przez nowy kalkulator, w kolejności chronologicznej.

Kopia prywatna jest w `.local-backups/mihu-002/2026-11-04-before-recency-model/`, z instrukcją README. Folder jest ignorowany przez Git i znajduje się poza target. W repo znajdują się jedynie narzędzia i opis operacji.

## Wykonana operacja

Konto `00000000-0000-0000-0000-000000000002` miało 35 sesji, 278 wpisów i 30 revisions; aktywne było 16 sesji/133 wpisy z okresu 2026-06-19–2026-11-03. Wszystkie aktywne wpisy miały v2, dostępne wyceny/profile/katalog i poprawną liczbę ruchów Completed. Nie było potwierdzonych wyjątków historii ani rozbieżności nazw.

Zweryfikowano pełne odtworzenie SQL w osobnej pustej bazie: skróty wszystkich rekordów 35/278/30 były identyczne. Kopiowano tylko historię tego użytkownika, bezpośrednie zależności katalogowe i wspólne słowniki; schema-only nie zawiera danych innych kont. Próba dodatkowej kopii całej bazy została odrzucona przez automatyczną kontrolę zakresu i zastąpiona powyższą węższą weryfikacją. Pełny dump innych danych nie został wykonany.

Na tej samej izolowanej bazie uruchomiono tymczasowy backend 127.0.0.1:18081. Po soft delete źródłowych sesji importer utworzył 16 nowych sesji/133 wpisy. GET potwierdził wszystkie wejścia, wyceny, style, ruchy, daty i tożsamość wspinaczek oraz obliczenia wygenerowane przez backend. To sprawdza ścieżkę importu na obecnym v2, nie trafność przyszłego modelu. Tymczasowy serwer zatrzymano i bazę usunięto.

Następnie na właściwym backendzie 8080, po sprawdzeniu tożsamości, wszystkich wersji i zgodności wejść z kopią, usunięto 16 sesji przez istniejące DELETE z expectedVersion. Jest to **soft delete**: nadal istnieją rekordy oznaczone jako usunięte, ale nie uczestniczą w aktywnej historii ani poziomie odniesienia. Zachowano katalog, wszystkie 278 wpisów i ich wyniki oraz wcześniejszy audyt. Dodano 16 revisions usunięcia. Dashboard jest pusty, piramidy wszystkich trzech dyscyplin mają zero elementów; aktywnych sesji jest zero. Skróty użytkowników, katalogu i historii pozostałych użytkowników nie zmieniły się.

## Narzędzia

- `infra/tools/export_training_history.py`: eksport jednego UUID w spójnym odczycie; odmawia nadpisania istniejącej kopii. Zapisuje SQL, surowe dane, komendy replay, preflight i SHA-256. Nie usuwa danych.
- `infra/tools/verify_training_history_backup.py`: odtworzenie kopii w bazie o losowej nazwie cb_history_verify_…; tylko zależności tej historii i schemat bez innych danych. Domyślnie usuwa bazę po sprawdzeniu; opcjonalne pozostawienie służy sprawdzeniu API.
- `infra/tools/archive_training_history.py`: domyślnie sprawdza backup i aktualne wersje/wejścia sesji; --apply wykonuje user-scoped DELETE przez API, zapisując receipts. Dotyczy wyłącznie aktywnych ID zapisanych w eksporcie. Po utracie odpowiedzi DELETE może zatrzymać wznowienie na 404; wtedy potrzebne jest sprawdzenie pending receipt.
- `infra/tools/replay_training_history.py`: domyślnie waliduje offline. Dopiero --apply wykonuje POST przy pasującej tożsamości i usuniętych oryginałach. Zapisane nowe clientSessionId/clientEntryId pozostają stałe przy retry. Dziennik jest związany z SHA pliku i adresem API, przechowuje pending request i receipts, ma blokadę równoległego importu. Nie potwierdza nowych konfliktów OS/Flash; może odtworzyć tylko uprzednio zapisane zgody.

Właściwy replay używa `replay-inputs.json` z głównego folderu kopii, nie z api-replay-check. Obejmuje tylko 16 uprzednio aktywnych sesji; nie wskrzesza 19 wcześniej usuniętych. areaId/climbId i wyceny pozostają te same, a backend ponownie wyznacza wyniki i kontekst poziomu. Soft-deleted clientSessionId są zarezerwowane, więc importer używa nowych identyfikatorów zapisanych raz w eksporcie. Nowe dane nie powstają w katalogu.

Przy kolejnych zmianach kontraktu przed replay należy ponowić walidację zgodności wejść. Zwykły SQL restore jest wariantem awaryjnym i zastępuje historię tego użytkownika wraz ze starymi wynikami; nie jest sposobem uruchomienia nowego kalkulatora.

Aktualizacja po wdrożeniu v3: osobny v3-replay-check i v3-replay-verification.json przechowują wynik testu 16/133, bez nadpisywania oryginalnej kopii ani dziennika właściwego importu. Potwierdzono niezmienione wejścia, wyceny, gradePoints i moveIntensity oraz nowe snapshoty poziomu/loadu. Local V23 dodała strukturę, zachowując wszystkie stare wyniki i active002=0. Testowego dziennika z portu 18081 nie używać do właściwego importu.
