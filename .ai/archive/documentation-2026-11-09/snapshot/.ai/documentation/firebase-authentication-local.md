# Firebase Authentication — lokalne uruchomienie i wynik implementacji

Data: 2026-11-08. Status: **lokalne logowanie Google i profil Mihu wdrożone oraz odebrane przez użytkownika**. Działają testy automatyczne i osobne konta modelowe. Użytkownik zlecił implementację po [planie A00–A08](firebase-authentication-local-plan.md), wskazał MiniJirę jako referencję i utworzył projekt Firebase `climbbetter-dev`. Nie wdrażano produkcji. Zakres profilu to nazwa i jej potwierdzenie (DEC-051); zlecenie implementacji zapisuje DEC-052 w [rejestrze](decisions.md).

## Co działa w kodzie

- Google Sign-In przez Firebase JavaScript SDK, profil podstawowy, wylogowanie i przywracanie sesji SDK po odświeżeniu strony.
- `POST /api/v1/identity/bootstrap`, `GET /api/v1/identity/me` i `PUT /api/v1/identity/me`. Bootstrap jest idempotentny również przy równoległych żądaniach, a GET nie tworzy kont.
- Firebase Admin w Javie: podpis/ważność/projekt tokenu oraz kontrola wyłączenia konta i unieważnienia sesji. Token jest sprawdzany przed zapytaniem biznesowym; niedostępność usługi jest odróżniona od nieprawidłowych poświadczeń.
- Spring Security dla API, stateless Bearer, spójne błędy JSON, CORS dla lokalnych portów 5173–5175 i poprawne zachowanie statusu błędu podczas servlet ERROR dispatch.
- V24 dodaje `identity.user_identities` i `identity.users.profile_completed_at`; zachowuje istniejące UUID oraz relacje historii/katalogu/audytu.
- Chronione trasy i wspólny profil w React. Transport anuluje żądania przy zmianie konta, sprawdza generację sesji i UID, ponawia 401 najwyżej raz dla tego samego konta i zachowuje treść zapisu. Szkice pozostają przypisane do wewnętrznego UUID.
- Lokalny Auth Emulator i dwa niezależne konta modelowe, bez prawdziwych e-maili. Mają osobny projekt emulowany, bazę i adres frontendu.

Projekt Firebase jest zarządzany obecnym kontem Google użytkownika. To nie nadaje Mihu roli ADMIN w CB. Konto usługi backendu jest osobną techniczną tożsamością, a nie kolejną skrzynką Gmail.

## Konfiguracja prawdziwego Mihu

Aktualizacja uruchomienia 2026-11-08: użytkownik wskazał klucz konta usługi poza repozytorium. Potwierdzono zgodność z `climbbetter-dev` i uprawnienia odczytu Auth; API działa na127.0.0.1:8080, health UP, brak tokenu daje401. V24 została zastosowana do właściwej bazy. Użytkownik wykonał prawdziwe Google Sign-In i zobaczył Connect your existing journal; jedyny nowy UID z dostawcą Google powiązano kontrolowanie z UUID Mihu002. Nie utworzono drugiego konta CB. Następnie użytkownik potwierdził nazwę Mihu i widoczność swojej historii w Dashboardzie; serwerowy stan profilu jest ukończony.

Publiczna konfiguracja aplikacji Web jest już zapisana w ignorowanym `frontend-web/.env.local`. Projekt to `climbbetter-dev`; na podstawie publicznych API potwierdzono włączenie Google i obecność `localhost` w Authorized domains. Rzeczywistych wartości API key ani poświadczeń nie kopiujemy do dokumentacji.

Do uruchomienia backendu potrzebny jest plik JSON konta usługi tego projektu, zapisany poza repozytorium. Jego zawartość nie trafia do czatu. Launcher sprawdza zgodność projektu z metadanych pliku przed uruchomieniem aplikacji.

Z katalogu głównego repozytorium, w oddzielnym terminalu:

```powershell
./infra/start-local-api.ps1 -CredentialsFile 'C:/path/outside/repository/firebase-dev-admin.json'
```

Frontend w drugim terminalu:

```powershell
Set-Location frontend-web
npm.cmd run dev
```

Odbiór przez `http://localhost:5173`. Pierwszy login Google w obecnej konfiguracji `VITE_AUTH_ACCOUNT_MODE=existing` wykonuje odczyt konta, a nie automatyczny bootstrap nowego UUID. Użytkownik bez mapowania widzi ekran połączenia istniejącego dziennika. Pozwala to powiązać Mihu z `00000000-0000-0000-0000-000000000002` bez tworzenia drugiego pustego konta.

Po pierwszym logowaniu operator weryfikuje właściwy UID w Authentication/Users projektu CB. Narzędzie jest domyślnie tylko inspekcją:

```powershell
python infra/tools/link_firebase_identity.py --project-id climbbetter-dev --subject '<verified-firebase-uid>'
```

Po potwierdzeniu właściwego konta parametr `--apply` zapisuje mapowanie. Identyczny zapis jest idempotentny; konflikt z innym UUID/UID kończy się błędem zamiast przepięcia konta. Narzędzie nie przenosi ani nie usuwa treningów. Następnie w aplikacji używamy Try again, potwierdzamy nazwę Mihu i sprawdzamy historię.

`VITE_AUTH_ACCOUNT_MODE=register` umożliwia automatyczne zakładanie nowych kont CB po Google Sign-In. Nie włączamy go podczas pierwszego powiązania Mihu. Zmiana nazwy profilu nie tworzy drugiego konta.

## Konta modelowe bez dodatkowych skrzynek

Przygotowano:

- `Test_dobry_wspinacz_1`: dwa syntetyczne treningi z 2026-11-01 i 2026-11-08, łącznie osiem wpisów; rozgrzewki oraz RP na boulderach 6B, 6C i 7A.
- `Test_poczatkujacy_1`: osobne, puste konto do testów izolacji i przyszłych przykładów.

To konta Auth Emulatora projektu `demo-climbbetter`, nie rzeczywiste konta Google i nie metoda logowania dopuszczona w prawdziwym Firebase. Nazwy i sztuczne adresy `example.test` nie wymagają skrzynek. UI ma przyciski wyboru tych kont tylko w lokalnym trybie emulatora, a po wejściu pokazuje oznaczenie Test data. Sam emulator wydaje niepodpisane tokeny; backend akceptuje je wyłącznie w jawnym trybie `emulator`, z lokalnym hostem i projektem `demo-`. [Zasady emulatora](https://firebase.google.com/docs/emulator-suite/connect_auth).

Osobna baza to `climbbetter_models`; właściwa historia Mihu pozostaje w `climbbetter`. Dla trwałości UID emulator importuje/eksportuje swój stan w ignorowanym `infra/firebase/.firebase/auth-state` — należy zachować ten folder razem z bazą modeli. Usunięcie stanu emulatora może wymagać ponownego powiązania kont modelowych; sama baza nie przechowuje sesji logowania.

Uruchomienie z repozytorium, w trzech terminalach:

```powershell
# 1. Auth Emulator; po instalacji zależności wystarczy npm.cmd start.
npm.cmd --prefix infra/firebase install
npm.cmd --prefix infra/firebase start
```

```powershell
# 2. API i osobna, wcześniej utworzona baza climbbetter_models.
./infra/start-local-api.ps1 -AuthMode emulator -Port 18081
```

```powershell
# 3. Frontend modeli, niezależny od .env.local prawdziwego Mihu.
./infra/start-model-web.ps1
```

Adres modeli: `http://127.0.0.1:5175`. Do odtworzenia syntetycznych treningów i sprawdzenia API służy `python infra/tools/verify_local_auth.py`. Dane mają stałe client IDs; ponowienie nie tworzy duplikatów. Narzędzie wymaga działającego emulatora i API w trybie emulatora. Nie jest importerem osobistej historii Mihu.

Tryb `stub` pozostawiono wyłącznie do jawnych testów biznesowych i lokalnego debugowania. Nie jest testem Firebase ani sposobem przełączania prawdziwych użytkowników. Uruchomienie stub/emulator bez profilu local/test kończy się błędem; błędny token nigdy nie włącza użytkownika zastępczego.

## Kopia i dane Mihu

Przed zmianami wykonano świeży eksport do ignorowanego `.local-backups/mihu-002/2026-11-08-before-firebase-auth/` i dokładny SQL restore w izolowanej bazie. Wynik PASS: 52 sesje/415 wpisów/46 revisions, w tym 17 aktywnych sesji/137 aktywnych wpisów. Kopie z poprzednich prac zachowano. Do wdrożenia auth nie wykonano resetu ani replay osobistej historii.

Mapowanie UID zachowało UUID Mihu. Dane zmieniane przez potwierdzenie profilu to nazwa i `profile_completed_at`. V24 działa na właściwej bazie oraz wcześniej sprawdzonych bazach testów i modeli.

Kontrola przed powiązaniem potwierdziła identyczne pełne rekordy historii, katalogu, użytkowników i audytu względem świeżej kopii. Następnie migracja V23→V24 i powiązanie Google również zachowały historię oraz wszystkie dotychczasowe pola użytkowników. Zmiana hasha całych wierszy identity wynikała z dodania nullable kolumny; odrębny hash poprzednich trzech pól wszystkich użytkowników pozostał identyczny. Powiązanie nie zmieniło historii ani właścicieli katalogu.

Dowody w `.local-backups/firebase-auth-implementation/`: `mihu-before-link-verification.json`, `real-firebase-migration-verification.json` i `mihu-after-google-link-verification.json`. Na etapie połączenia potwierdzono17 aktywnych sesji; stan ukończenia profilu oczekiwał kliknięcia przez użytkownika.

Końcowy `mihu-real-google-final-verification.json` potwierdza ukończony profil Mihu, mapowanie Google do pierwotnego UUID002,17 aktywnych sesji/137 wpisów oraz identyczne pełne rekordy historii, katalogu i wcześniejsze pola wszystkich użytkowników. Cała historia nadal zawiera52 sesje/415 wpisów/46 revisions. Zmiana dotyczy nowej tabeli mapowań i nowego znacznika ukończenia profilu; nie wykonano resetu, importu ani ponownego przeliczenia treningów.

Archive/replay otrzymały obsługę Bearer z `CB_FIREBASE_ID_TOKEN_FILE` lub `CB_FIREBASE_ID_TOKEN`. Token nie trafia do URL ani dziennika importu. Odtworzenie po auth nadal wymaga świeżego eksportu, zgodności `/identity/me` z UUID kopii i sprawdzonego restore; nie uruchomiono go w tym wdrożeniu.

## Dowody weryfikacji

- Backend: pełny zestaw 220 testów, bez błędów i pominięć; obejmuje nową granicę auth oraz wcześniejsze obliczenia, CRUD i raporty.
- Frontend: build i lint oraz 30 testów modelu przeszły; 37 dotychczasowych scenariuszy przeglądarkowych przeszło po dodaniu ochrony tras.
- Cztery dodatkowe testy przeglądarkowe z rzeczywistym SDK Firebase i Auth Emulatorem sprawdziły onboarding/reload/logout, szkice dwóch kont, spóźnioną odpowiedź poprzedniego konta oraz pojedynczy retry 401/obsługę 503.
- Osobny test prawdziwego UI → Firebase SDK/Emulator → Java Admin SDK → PostgreSQL przeszedł: dwa treningi konta modelowego są widoczne dla właściciela, drugie konto ich nie widzi, a wejście w cudzą sesję daje Session not found.
- `verify_local_auth.py`: osiem kontroli PASS, w tym rzeczywista weryfikacja tokenu Admin SDK, idempotencja, odrzucenie obcego projektu i konta wyłączonego w emulatorze.
- `npm audit --omit=dev`: zero zgłoszeń dla zależności aplikacji. Firebase JS przypięto do 12.18.0 i zgodnych z Node.js 22 składników app/compat; gRPC do poprawionej 1.14.7. React Router zaktualizowano w istniejącej głównej wersji do 7.18.5.

Raport API bez tokenów znajduje się w `.local-backups/firebase-auth-implementation/emulator-api-verification.json`. Wyniki Surefire są w `backend/target/surefire-reports`; zrzuty UI i ślady Playwright są ignorowanymi artefaktami lokalnymi. Testy emulatora nie zastępują ręcznego Google Sign-In Mihu i sprawdzenia jego dotychczasowej historii.

## Zamknięcie lokalnego etapu

Poświadczenia backendu, właściwe API, prawdziwe Google Sign-In, migracja, mapowanie UID, potwierdzenie profilu i kontrola niezmienności danych są wykonane. Użytkownik potwierdził widoczność Mihu i dotychczasowej historii w UI. Lokalny etap auth jest zakończony. Rozwój ADMIN i produkcja pozostają osobnymi etapami.
