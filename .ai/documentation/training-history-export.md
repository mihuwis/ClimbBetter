# Kopia i odtworzenie historii

Aktualizacja: 2026-11-09. Operacje v3 i Firebase zakończone; opis nie jest zleceniem ponowienia usuwania/importu.

## Dwa rodzaje odtworzenia

- **SQL restore** przywraca fakty, stare wyniki i audyt. Nie uruchamia nowego kalkulatora.
- **API replay** wysyła aktywne sesje chronologicznie jako komendy do aktualnego kalkulatora. Zachowuje wejścia i tożsamość Climb, a generuje nowe snapshoty i nowe ID zapisane raz w eksporcie. Nie wskrzesza wcześniej usuniętych sesji.

Wdrożenie auth nie wymagało replay. Najnowsza opisana kopia z 2026-11-08: `.local-backups/mihu-002/2026-11-08-before-firebase-auth/`, pełne 52 sesje/415 wpisów/46 revisions, aktywne 17/137. Dokładny restore przeszedł. Eksport historii nie jest pełnym backupem środowiska: mapowania `user_identities`, konta Firebase i poświadczenia wymagają osobnego zabezpieczenia.

## Narzędzia `infra/tools`

| Narzędzie                           | Działanie                                                                                                   |
| ----------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| `export_training_history.py`        | spójny eksport jednego UUID: SQL, surowe dane, replay, preflight i SHA-256; bez usuwania i nadpisania kopii |
| `verify_training_history_backup.py` | dokładny restore zależności tej historii w osobnej bazie `cb_history_verify_…`, domyślnie sprzątanej        |
| `archive_training_history.py`       | domyślnie sprawdza kopię/wersje/wejścia; `--apply` wykonuje DELETE po ID z eksportu i zapisuje receipts     |
| `replay_training_history.py`        | domyślnie walidacja offline; `--apply` wysyła POST przy zgodnej tożsamości i usuniętych oryginałach         |

Przed zmianą danych: świeży eksport, zweryfikowany restore, zgodność `/identity/me` z UUID kopii i preflight kontraktu. Archive/replay używają Bearer przez `api_auth.py`: `CB_FIREBASE_ID_TOKEN_FILE` lub `CB_FIREBASE_ID_TOKEN`; token nie trafia do URL/dziennika.

Replay wiąże dziennik z SHA wejścia i adresem API, utrwala pending request, client IDs i receipts, blokuje równoległy import. Zachowujemy ten dziennik przy retry; nie nadajemy nowych ID. Nie potwierdza nowych konfliktów OS/Flash. Niejednoznaczne DELETE/404 po utracie odpowiedzi wymaga sprawdzenia pending receipt przed wznowieniem. Importów testowych nie mieszamy z dziennikiem właściwej bazy.

## Zakończona operacja v3, 2026-11-04

Kopia `.local-backups/mihu-002/2026-11-04-before-recency-model/` zabezpieczyła 35 sesji/278 wpisów/30 revisions, w tym 16 aktywnych sesji/133 wpisy. Po sprawdzonym restore i testowym replay wykonano soft delete 16 aktywnych sesji, a następnie właściwy chronologiczny import do v3. Oryginały i 46 revisions po archiwizacji zachowano, bez zmiany katalogu i innych kont. Wejścia, wyceny, punkty i intensywności zgadzały się; nowe loady były odtwarzalne ze snapshotów. Dashboard i 12 wariantów piramidy sprawdzone.

Główny dziennik: `replay-inputs.replay-state.json`; dowód: `live-v3-import-verification.json` obok wejść. Późniejsze liczniki z wdrożenia Firebase opisują nowszy stan, nie sprzeczny wynik tej operacji. Szczegółowa chronologia: [archiwalny raport](../archive/documentation-2026-11-09/snapshot/.ai/documentation/training-history-export.md).
