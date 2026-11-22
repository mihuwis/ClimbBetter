# Firebase Authentication — lokalny dev

Aktualizacja: 2026-11-09. **Wdrożone i odebrane 2026-11-08**: prawdziwe Google Sign-In, profil Mihu i dotychczasowa historia. DEC-050–052. Produkcja i ADMIN pozostają osobnymi zadaniami. [Datowane dowody](backend-verification.md).

## Przepływ i granice

Firebase JS SDK loguje Google i utrzymuje sesję. API otrzymuje ID token, Firebase Admin sprawdza podpis/ważność/projekt oraz revocation/disabled, Spring Security tworzy bieżącą tożsamość. `identity.user_identities` wiąże issuer/UID z UUID CB. Nie łączymy kont po samym e-mailu. Konto usługi backendu nie jest skrzynką użytkownika ani rolą ADMIN.

`POST /identity/bootstrap` tworzy/pobiera konto idempotentnie; `GET /identity/me` tylko czyta, a `PUT /identity/me` potwierdza nazwę i stan profilu. Prefix `/api/v1`. Bez mapowania backend zwraca `409 ACCOUNT_NOT_PROVISIONED`, bez ukończonego profilu dla funkcji biznesowych `403 PROFILE_INCOMPLETE`. Brak/niepoprawny token → `401 AUTH_REQUIRED`/`INVALID_TOKEN`; awaria weryfikatora → `503 AUTH_UNAVAILABLE`, bez fallbacku na użytkownika local.

React chroni trasy, prowadzi przez profil i czyści aktywny kontekst przy zmianie konta. Transport anuluje żądania, odrzuca odpowiedzi starego UID/generacji i ponawia 401 najwyżej raz w tym samym koncie, z tą samą treścią zapisu. Szkice należą do wewnętrznego UUID, bez mieszania kont. Ankieta wspinaczkowa i role nie są częścią podstawowego profilu.

## Prawdziwe konto Mihu

Projekt dev: `climbbetter-dev`, Google włączone, localhost w Authorized domains. Publiczna konfiguracja web jest w ignorowanym `.env.local`, wzór w `.env.example`. `VITE_AUTH_ACCOUNT_MODE=existing` chroni przed przypadkowym utworzeniem drugiego pustego konta; `register` służy świadomemu dopuszczeniu nowych kont. JSON konta usługi jest poza repozytorium; launcher sprawdza zgodność projektu.

[Standardowe uruchomienie](development.md#standardowy-dev-z-googlefirebase). Powiązanie Google Mihu z UUID `00000000-0000-0000-0000-000000000002` już wykonano. Nie trzeba ponawiać mapowania ani importu historii.

Narzędzie operatora dla kontrolowanego powiązania:

```powershell
python infra/tools/link_firebase_identity.py --project-id climbbetter-dev --subject '<verified-firebase-uid>'
```

Domyślnie inspekcja; dopiero `--apply` zapisuje mapowanie. UID wymaga potwierdzenia w Authentication/Users właściwego projektu. Konflikt UID/UUID odrzucany, identyczne mapowanie idempotentne. Narzędzie nie przenosi treningów.

## Konta modelowe

Auth Emulator `demo-climbbetter`, osobna baza `climbbetter_models`, API 18081, web `http://127.0.0.1:5175`. Konta bez prawdziwych skrzynek: `Test_dobry_wspinacz_1` (2 syntetyczne sesje/8 wpisów) i puste `Test_poczatkujacy_1`. Wybór kont i oznaczenie Test data działają tylko lokalnie.

Z repozytorium, w trzech terminalach, po utworzeniu osobnej bazy modeli:

```powershell
# Terminal 1; instalacja zależności przy pierwszym uruchomieniu.
npm.cmd --prefix infra/firebase ci
npm.cmd --prefix infra/firebase start
```

```powershell
# Terminal 2
./infra/start-local-api.ps1 -AuthMode emulator -Port 18081
```

```powershell
# Terminal 3
./infra/start-model-web.ps1
```

`python infra/tools/verify_local_auth.py` odtwarza syntetyczne dane ze stałymi client IDs i sprawdza izolację API; wymaga emulatora i API. Nie jest importerem osobistej historii. Stan UID emulatora trwa w ignorowanym `infra/firebase/.firebase/auth-state`; zachowujemy go razem z bazą modeli.

Tryby `emulator` i `stub` są dozwolone tylko dla local/test. Emulator wymaga lokalnego hosta i projektu `demo-`; jego niepodpisane tokeny nie są prawdziwym logowaniem Google. Standardowe `firebase` odrzuca konfigurację emulatora.

## Zachowanie danych

Przed wdrożeniem wykonano eksport i wierny SQL restore w izolacji. Końcowa kontrola z 2026-11-08: zachowany UUID Mihu, ukończony profil, 17 aktywnych sesji/137 wpisów; pełna historia 52 sesje/415 wpisów/46 revisions. Historia, katalog, audyt i wcześniejsze pola innych użytkowników bez zmian. Nie wykonano resetu, replay ani przeliczenia treningów.

Kopia: `.local-backups/mihu-002/2026-11-08-before-firebase-auth/`. Dowód końcowy: `.local-backups/firebase-auth-implementation/mihu-real-google-final-verification.json`. To prywatne lokalne artefakty, nie pliki dystrybuowane z repozytorium. [Odtwarzanie historii i Bearer w narzędziach](training-history-export.md).
