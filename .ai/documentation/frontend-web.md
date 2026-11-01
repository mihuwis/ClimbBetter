# Frontend web

Data: 2026-10-17. Status: zaimplementowane punkty 1–5, potwierdzone automatycznie; oczekują na ocenę użytkownika. Użytkownik upoważnił asystenta do zmian i wybrał **angielski interfejs**. Źródła: kod React, testy przeglądarkowe, build/lint oraz uruchomienie na lokalnych danych Mihu.

## Struktura

`app`, `pages` i `features` pozostają osobno, ponieważ mają różne zadania:

```text
src/
  app/                 App, routing i wspólny layout
  pages/               ekrany i składanie funkcji dla konkretnej trasy
  features/
    dashboard/         karty aktywności i personal bests
    training/          model sesji, API zapisu, formularze, szkic, kalendarz
    catalog/           typy miejsc, sektorów, wspinaczek i słowników
  shared/
    api/               HTTP, błędy, odczyt z anulowaniem żądań
    lib/               formatowanie liczb i dat
    ui/                stany ładowania i błędów
```

Model sesji należy do `training`. Typy karty Dashboardu nie definiują sesji. Usunięto nieużywane mocki, stare wspólne typy domenowe i zastąpione komponenty prototypu. Wspólny formularz obsługuje manual/live/edit; uruchomienie i czas są odrębnymi zachowaniami.

## Wdrożone ekrany

1. **Dashboard `/`**: prawdziwe sesje z API, Adjusted Load, ruchy, czas i najwyższe ukończone wyceny oddzielnie dla balda i drogi/obwodu. Personal bests prowadzą do sesji. Lista obejmuje maksymalnie 20 ostatnich sesji; jej sumy są tak opisane. Brak fikcyjnych wykresów, celów i wyników.
2. **Details `/sessions/:id`**: podsumowanie backendu, grupy po `climbId`, chronologia po `entryOrder`, osobna rozgrzewka, obie wartości loadu i szczegóły obliczeń. **Edit `/sessions/:id/edit`**: zmiana danych sesji i wpisów, dodanie nowej/znanej wspinaczki, usunięcie wpisu lub wszystkich wpisów wspinaczki, zmiana kolejności. Zapis całego agregatu przez PUT wymaga powodu korekty i wersji. Usunięcie całej sesji wymaga potwierdzenia i wersji.
3. **Calendar `/calendar`**: dowolny miesiąc, dni z aktywnością i sesje wybranego dnia. Oddzielny GET z zakresem dat nie dziedziczy limitu 20 Dashboardu.
4. **New session `/sessions/new`**: miejsce z katalogu albo nowe prywatne miejsce, nazwa, start/koniec i strefa IANA. Rozgrzewka, próba lub przejście; istniejąca, nowa prywatna lub powtarzana nowa wspinaczka ze szkicu. Sektory i wyszukiwanie, słowniki wycen/profili, ruchy, OS/Flash/RP oraz LEAD/TOP_ROPE dla dróg. Dla katalogu skalnego wyświetla oryginalną wycenę, a do obliczeń wysyła kanoniczną.
5. **Start now `/sessions/record`**: jawny start i stop, rzeczywiste instants, timer oparty na czasie startu, dopisywanie wpisów i końcowy POST. Odświeżenie nie zeruje licznika. Rozpoczęcie nie zapisuje sesji na serwerze; zapis następuje po zakończeniu.

Areas również odczytuje rzeczywisty katalog: miejsca, sektory, nazwy, wyceny, metry, ruchy i widoczność. Samodzielne dodawanie Climb przed treningiem nadal jest dostępne przez API/Swagger; osobny formularz katalogu nie należał do tych pięciu punktów.

## Szkic i błędy

`localStorage` przechowuje osobny szkic manual, live i edycji konkretnej sesji **dla UUID aktualnego użytkownika**. Identyfikatory klienta pozostają stabilne. Przetrwają dane sesji, dodane wpisy, wpis właśnie wypełniany oraz powód korekty. Anulowanie wpisu i odrzucenie szkicu są jawne. To szkic lokalny tej przeglądarki, nie synchronizacja między urządzeniami.

Przed wysłaniem utrwalane jest dokładne żądanie. Przy braku odpowiedzi lub 5xx formularz jest zamrożony do ponowienia; POST korzysta z backendowej idempotencji. Błąd walidacji umożliwia korektę. Konflikt historii OS/Flash pozwala poprawić styl albo jawnie potwierdzić deklarację. Stara wersja edycji nie nadpisuje nowszej. PUT po utracie odpowiedzi może zwrócić konflikt wersji, jeżeli pierwszy zapis doszedł do skutku; trzeba wtedy porównać najnowszą sesję, a nie ponawiać korekty z nową wersją automatycznie.

Zdarzenie zmiany szkicu w innej karcie blokuje dalszą edycję do przeładowania. Nie jest to atomowa blokada równoczesnych zapisów wielu kart. Uszkodzony szkic wymaga jawnego odrzucenia; niedostępny/pełny storage wyświetla ostrzeżenie. Brak backendowego preview loadu oraz pełnej pracy offline; wyniki są obliczane przez serwer po zapisie. `sessionDate` pozostaje lokalną datą, a konwersja godzin używa strefy sesji i odrzuca nieistniejące godziny DST.

## Uzupełnienia API

- `GET /api/v1/identity/me`: UUID i nazwa aktualnego użytkownika, potrzebne do podpisu UI i oddzielenia szkiców. W profilu local jest to wybór tożsamości procesu, nadal bez logowania.
- `GET /api/v1/training/sessions?from=YYYY-MM-DD&to=YYYY-MM-DD`: wszystkie widoczne sesje z zakresu dat, bez usuniętych i cudzych. Zakres musi być uporządkowany, różnica granic najwyżej 366 dni. Nowa migracja nie była potrzebna.

## Uruchomienie

Najpierw uruchom Docker Desktop i upewnij się, że `docker version` pokazuje Client i Server. Potrzebny jest lokalny PostgreSQL Compose. **Zrestartuj wcześniej uruchomiony backend**, aby załadował nowe endpointy. W Bash, w `backend`:

```bash
CB_LOCAL_USER_ID=00000000-0000-0000-0000-000000000002 ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

W drugim terminalu, w `frontend-web`:

```bash
npm ci
npm run dev
```

Adres: **http://localhost:5173/**. Vite przekazuje `/api` do `127.0.0.1:8080`. `strictPort` zatrzymuje uruchomienie przy zajętym 5173 zamiast przejść na port odrzucany przez lokalny CORS. Opcjonalne `CB_API_PROXY_TARGET` ustawia cel proxy, a `VITE_API_URL` bezpośredni adres backendu; domyślnie pozostaw je nieustawione.

## Weryfikacja 2026-10-17

- `npm run build` i `npm run lint`: sukces.
- `npm test`: **4/4** — daty/strefy/DST, izolacja szkiców, zachowanie żądania i grupowanie tożsamości wspinaczek.
- `npm run test:e2e`: **6/6**, headless Edge. Obejmuje Dashboard/Details/Calendar, edycję agregatu, usunięcie, odzyskanie szkicu, identyczne retry po braku odpowiedzi, timer po odświeżeniu oraz próbę i przejście nowej wspinaczki ze wspólnym clientClimbId. Odpowiedzi HTTP są kontrolowane przez test.
- Dodatkowo **1/1 test przez prawdziwy backend Mihu i trwały PostgreSQL**: create → read → update → calendar → delete. Potwierdzono zapis ruchów i wersji przez GET, 404 po usunięciu oraz brak poziomego przewijania Dashboardu przy 390 px. Obejrzano zrzuty desktop/mobile. Sesja `[SMOKE]` została soft-deleted, bez modyfikowania sesji DEMO.
- Backend: **52 testy, 0 failures/errors/skipped**, PostgreSQL Testcontainers. Dodatkowy test kalendarza obejmuje 22 widoczne sesje i wykluczenie cudzej/usuniętej, granice dat oraz błędny zakres.

Testy UI wymagają zainstalowanego Edge (`channel: msedge`). Domyślnie używają Vite 5174 i kontrolowanych odpowiedzi. Opcjonalny test prawdziwej lokalnej bazy wymaga konta Mihu i katalogu Bronx; tworzy jedną sesję i usuwa ją przez API, pozostawiając audyt. W Bash, przy uruchomionym backendzie 8080 i wolnym porcie 5173:

```bash
CB_REAL_API=1 CB_WEB_TEST_PORT=5173 npm run test:e2e
```

Źródła testów: [browser](../../frontend-web/tests/browser/sessions.spec.ts), [real API](../../frontend-web/tests/browser/live-api.spec.ts), [draft](../../frontend-web/tests/session-draft.test.mjs). Dalsza ocena: uwagi użytkownika do układu Dashboardu, wygody formularzy i rejestrowania treningu. Media, preview, pełny offline i produkcyjne logowanie pozostają poza przyrostem.
