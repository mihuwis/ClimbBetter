# Logowanie i profil użytkownika — frontend web

Data: 2026-10-24; aktualizacja 2026-11-08. Status: Google/Firebase auth i profil podstawowy wdrożone, sprawdzone automatycznie i odebrane przez użytkownika na prawdziwym koncie Mihu. Użytkownik potwierdził nazwę i widoczność dotychczasowej historii; zachowano UUID i wszystkie dane. [Wynik implementacji i uruchomienie](firebase-authentication-local.md) uzupełnia [plan lokalny](firebase-authentication-local-plan.md). Ankieta pozostaje na później (DEC-051, OPEN-26 zamknięte).

Źródło: ustalenia z użytkownikiem i polecenie ich zapisania z 2026-10-24. Obecna struktura aplikacji: [frontend web](frontend-web.md).

## Podział odpowiedzialności

Frontend React obsługuje logowanie Google przez Firebase Authentication, stan zalogowania oraz formularz profilu. Backend Java w module `identity` weryfikuje tożsamość i ustala wewnętrzny UUID użytkownika ClimbBetter.

Firebase Authentication przechowuje techniczne konto logowania: Firebase UID, metody logowania i podstawowe dane od dostawcy. Konto CB, profil i dane treningowe przechowujemy przez własne API w PostgreSQL. Późniejsza ankieta również będzie należeć do danych aplikacji. Firebase UID jest powiązany z UUID konta CB.

W tym etapie dodajemy logowanie Google. Docelowo kolejne metody logowania mogą zostać dołączone do tego samego konta.

## Foldery i pliki frontendu

Poniższe ścieżki są względne wobec `frontend-web/src/`:

```text
shared/
  lib/firebase.ts                    ← inicjalizacja Firebase Auth
  api/http.ts                        ← istniejąca komunikacja z naszym API

features/
  auth/
    services/auth.service.ts         ← logowanie Google i wylogowanie
    context/AuthProvider.tsx         ← stan zalogowania
    hooks/useAuth.ts                 ← dostęp do tego stanu
    components/RequireAuth.tsx       ← dostęp do ekranów po zalogowaniu

  profile/
    api/profile.ts                   ← pobieranie i zapis profilu przez API
    model/profile.types.ts           ← typy danych profilu

pages/
  LoginPage.tsx                      ← ekran logowania
  ProfileSetupPage.tsx               ← nazwa i potwierdzenie profilu
```

Wymienione pliki istnieją po implementacji 2026-11-08. Dodano też stan generacji sesji transportu, ekran oczekiwania na powiązanie dziennika oraz wybór lokalnych kont emulatora. `CurrentUser` należy teraz do modelu profilu, a strony używają wspólnego auth context zamiast osobno pobierać `/identity/me`.

## Zadania plików

- `shared/lib/firebase.ts` — wspólna inicjalizacja Firebase Authentication, z której korzystają obsługa logowania i transport HTTP.
- `shared/api/http.ts` — istniejący wspólny transport do API; rozszerzymy go o dołączanie aktualnego Firebase ID tokenu w nagłówku `Authorization: Bearer …`.
- `features/auth/services/auth.service.ts` — operacje logowania Google i wylogowania przez Firebase Authentication.
- `features/auth/context/AuthProvider.tsx` — udostępnienie stanu zalogowania, w tym oczekiwania na jego ustalenie, oraz koordynacja pobrania profilu z naszego API po zalogowaniu.
- `features/auth/hooks/useAuth.ts` — dostęp komponentów React do stanu i operacji logowania.
- `features/auth/components/RequireAuth.tsx` — kontrola dostępu do ekranów po stronie Reacta. Weryfikacja dostępu do danych odbywa się również w backendzie.
- `features/profile/api/profile.ts` — odczyt i zapis profilu przez własne API z użyciem wspólnego transportu HTTP.
- `features/profile/model/profile.types.ts` — typy danych profilu wymienianych z API.
- `pages/LoginPage.tsx` — ekran uruchamiający logowanie Google.
- `pages/ProfileSetupPage.tsx` — ekran nazwy i potwierdzenia podstawowego profilu po pierwszym logowaniu; ankieta wspinaczkowa pozostaje na później.

## Przepływ logowania i profilu

1. React loguje użytkownika przez Firebase.
2. Do żądań naszego API dołącza otrzymany token.
3. Backend w module `identity` sprawdza token i ustala UUID konta CB.
4. Profil pobieramy i zapisujemy przez nasze API w PostgreSQL.

Odpowiednik `userProfile.service.ts` z MiniJiry nadal będzie potrzebny, ale będzie wywoływał nasze endpointy zamiast `getDoc()` i `setDoc()` z Firestore. W ClimbBetter tę rolę pełni planowany `features/profile/api/profile.ts`.

## Pierwszy krok implementacji

Kolejność rozszerzona 2026-11-08: najpierw baseline i świeża kopia historii (A00), potem projekt Firebase dev (A01). Pierwszy krok po stronie Reacta nadal obejmuje `shared/lib/firebase.ts` oraz `features/auth/services/auth.service.ts` (A02). Techniczny login Google nie uruchamia jeszcze automatycznego tworzenia kont CB: najpierw wiążemy UID Mihu z dotychczasowym UUID, aby nie utworzyć drugiego pustego konta. [Pełna kolejność A00–A08](firebase-authentication-local-plan.md#9-kolejność-realizacji-i-warunki-ukończenia).

## Punkty odniesienia w obecnym kodzie

- [Wspólny transport HTTP](../../frontend-web/src/shared/api/http.ts).
- [Routing aplikacji React](../../frontend-web/src/app/router.tsx).
- [Kontrakt bieżącego użytkownika w backendzie](../../backend/src/main/java/pl/climbbetter/identity/CurrentUserProvider.java).
- [Obecny endpoint bieżącego użytkownika](../../backend/src/main/java/pl/climbbetter/identity/internal/CurrentUserController.java).
