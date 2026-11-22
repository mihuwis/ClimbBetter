# Weryfikacja aplikacji

Aktualizacja dokumentu: 2026-11-15. Poniżej wyniki bieżącej weryfikacji oraz wcześniejsze datowane raporty; [pełne relacje](../archive/documentation-2026-11-09/snapshot/.ai/documentation/backend-verification.md) i [raport auth](../archive/documentation-2026-11-09/snapshot/.ai/documentation/firebase-authentication-local.md).

## 2026-11-15 — czas sesji, DEC-053

`./mvnw.cmd verify`: 242 testy, bez błędów i pominięć. W tym 19 scenariuszy `TrainingSessionTimeConflictIntegrationTest`: częściowe i pełne nakładanie, identyczne okna, granice przedziałów, inne miejsca i użytkownicy, strefy i północ, zachowanie stanu po odrzuconej edycji, zwolnienie czasu przez usunięcie, blokada zapisu poza API, jednoczesne dodawanie/edycje i identyczne retry. Starsze dane testowe kalendarza, Dashboardu i raportów otrzymały rozłączne czasy.

Frontend: lint i build poprawne; `npm.cmd run test:e2e -- sessions.spec.ts` — 15/15 w Edge. Testy z kontrolowanym API potwierdzają komunikat `SESSION_TIME_CONFLICT`, zachowanie wpisów i identyfikatorów szkicu po odświeżeniu oraz skuteczny ponowny zapis po zmianie godzin. Wykorzystano istniejącą obsługę błędów formularza.

Lokalna baza: prywatny backup przed migracją w `.local-backups/session-time-20261115-111006/`; Flyway przeszedł z V24 do V25. Wymagane granice czasu i ograniczenie wykluczające kolizje są aktywne. Sumy kontrolne wszystkich rekordów sesji (53), wpisów (424), użytkowników, miejsc i wspinaczek pozostały identyczne; 18 aktywnych sesji nie ma kolizji. Logi w `backend/target/session-time-*-test.log`, `backend/target/session-time-local-migration.log` i `frontend-web/test-results/session-time-browser-test.log`.

## Wcześniejsze wyniki

| Data / przyrost             | Zapisany wynik i zakres                                                                                                                                                                                                                                                 |
| --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 2026-11-08 Firebase         | Backend 220/220, bez błędów/pominięć; web build/lint, 30 modelowych i 37 dotychczasowych browser. Dodatkowe 4 browser SDK/emulator oraz 1 pełnego UI/API/PostgreSQL. Weryfikator emulatora: 8 PASS. Prawdziwe Google, profil i historia potwierdzone przez użytkownika. |
| 2026-11-07 Training summary | Backend 210/210, web build/lint, 30 modelowych/37 browser. Niezależnie 18 raportów porównanych z surowymi danymi 17 sesji/137 wpisów; 1 odczytowy browser pokrył 6 kombinacji UI/API. Historia i schemaVersion bez zmian.                                               |
| 2026-11-04 v3               | Backend 176/176; web build/lint/24 modelowe oraz realny CRUD browser na osobnej bazie. V23 i replay 16 sesji/133 wpisów zweryfikowane, oryginały zachowane.                                                                                                             |
| 2026-11-04 formularz        | Completed: backend 136/136 i web 24 modelowe/31 browser; Repeat session/ręczny czas: 16 modelowych/26 browser w ówczesnym zestawie.                                                                                                                                     |
| 2026-11-03–04 piramida      | Backend 120/120; V21/V22, 12 kombinacji HTTP/listy i kontrola niezmienności historii. Frontend build/lint/11 modelowych/22 browser oraz osobny odczytowy test 12 kombinacji z realnym API.                                                                              |
| 2026-11-01 v2               | Backend 74/74; osobny podzbiór V20: 9/9. V16–V20 zachowały zapisane wyniki. Frontend build/lint/6 modelowych/13 browser.                                                                                                                                                |

Liczby opisują różne wersje zestawu i nie są sumą unikalnych testów. Kontrolowane browser testy nie zastępują prawdziwego API; emulator nie zastępuje Google Sign-In. Migracja, zgodność danych, wynik testu i ręczny odbiór UI to oddzielne dowody.

## Lokalizacja dowodów

- Surefire: `backend/target/surefire-reports` i historyczny izolowany build `backend/target/v2-verification/target/surefire-reports`. Izolacja wyjścia chroniła `.class` przed nadpisaniem przez IDE; źródła i zależności były te same.
- Raport okresowy: `backend/target/v2-verification/period-summary-local-verification.json` i `period-summary-full-test.log`.
- Auth: `.local-backups/firebase-auth-implementation/emulator-api-verification.json` oraz `mihu-real-google-final-verification.json`; wynik końcowy potwierdził zachowane UUID i dane Mihu.
- Replay: [historia odtworzenia i dziennik](training-history-export.md). Zrzuty/trace Playwright i prywatne raporty są ignorowane, mogą nie być obecne w świeżym checkout.

Przegląd dokumentacji z 2026-11-09 sprawdzał źródła, odsyłacze i zachowanie archiwum; nie obejmował ponownego uruchomienia aplikacji. [Polecenia weryfikacji](development.md), [pozostały odbiór](../project-management/priorities.md).
