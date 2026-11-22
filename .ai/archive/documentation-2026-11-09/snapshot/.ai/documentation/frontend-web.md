# Frontend web

Aktualizacja auth 2026-11-08: Google/Firebase, AuthProvider, chronione trasy, profil, wylogowanie i transport odporny na zmianę konta są wdrożone. Użytkownik potwierdził prawdziwe Google Sign-In, ukończenie profilu Mihu i swoją historię w Dashboardzie. Build/lint/30 model/37 dotychczasowych browser oraz4 testy auth i1 rzeczywisty UI/API przeszły. Osobne konta modelowe działają bez skrzynek. [Bieżące polecenia uruchomienia i stan](firebase-authentication-local.md); starsze komendy z `CB_LOCAL_USER_ID` poniżej opisują tryb sprzed Firebase.

Data aktualizacji: 2026-11-04. Status: frontend modelu v2, Statistics, powtarzanie sesji/wpisów i walidacja Completed wdrożone na polecenie użytkownika. Użytkownik wybrał **angielski interfejs**. Ostatnia weryfikacja: build/lint, 24 testy modelu i 31 przeglądarkowych przeszły; backend z nową walidacją: 136 testów. Wcześniejszy odczyt 12 kombinacji Statistics przez prawdziwy backend V22 również przeszedł. Pozostają ręczna ocena nowych usprawnień i wspólne dodanie sesji v2. [Zakres Statistics i dowody](statistics-frontend.md). Wyniki realnego CRUD z 2026-10-17 poniżej dotyczą wcześniejszej wersji.

## Repeat climb i Completed — 2026-11-04

Na polecenie użytkownika w wierszach wspinaczek formularza dodano **Repeat climb**, obok Edit/Remove. Kopia trafia bezpośrednio po źródle, otrzymuje nowe clientEntryId, a wszystkie entryOrder zostają uporządkowane. Zachowuje climbId albo wspólne clientClimbId nowej prywatnej wspinaczki, wynik, styl, wycenę, profil, ruchy, asekurację i notatki. Każdą kopię można niezależnie edytować lub usunąć. Rozgrzewka nie ma tego przycisku.

W odróżnieniu od wcześniejszego Repeat session, użytkownik poprosił tu o dokładną kopię wartości: OS/Flash pozostają OS/Flash. Potwierdzenie wyjątku historii nie jest kopiowane, a dotychczasowa walidacja stylu przez backend nadal obowiązuje. Oryginał pozostaje bez zmian; snapshoty obliczeń/audyt nie są kopiowane. Kopia jest utrwalana w bieżącym szkicu, a pending retry zachowuje dokładny payload i blokuje dalsze klonowanie.

Użytkownik doprecyzował ścisłą równość: **Completed (`ASCENT`) wymaga executedMoves == totalMoves**. Dla 8 ruchów jedynie 8/8 jest poprawne; 2/8 i 10/8 są odrzucane. Próby nadal dopuszczają mniej i więcej ruchów. Wybranie Completed lub zmiana długości przy Completed automatycznie dopasowują wykonane ruchy do pełnej długości. Ręcznie niespójne dane mają komunikat przy polu i blokują dodanie/edycję wpisu. Walidacja przed zapisem całej sesji obejmuje też odtworzone szkice i kopie dawnej historii; wskazuje numer błędnego wpisu.

Ta sama reguła działa w backendzie przy nowym POST/PUT, przez istniejące 400 INVALID_TRAINING_SESSION. Odczyt historii i identyczne retry wcześniej przyjętego żądania nie uruchamiają nowej walidacji ani przeliczenia. Brak nowych migracji; aktywacja zmiany serwera wymaga restartu dotychczasowego backendu użytkownika.

Weryfikacja końcowa: **build/lint, 24/24 model i 31/31 Edge**, exit 0, 14,0 s. Nowe 5 przypadków helpera kopiowania, 3 walidacji/zapisu/retry i 5 browser obejmują tożsamość wpisów/wspinaczek, kolejność, niezależną edycję, prywatne wspinaczki, odświeżenie, dokładny retry, 2/8 i 10/8 jako Completed oraz naprawę starego szkicu. Pełny backend **136/136**, PostgreSQL Testcontainers; [szczegóły](backend-verification.md#completed--ścisła-liczba-ruchów-2026-11-04). Nie tworzono/edytowano sesji w lokalnej bazie i nie restartowano działającego backendu użytkownika.

## Ręczna data i Repeat session — 2026-11-04

Na prośbę użytkownika wdrożono dwa usprawnienia formularza:

- **New session:** zmiana `Start` podpowiada `End` godzinę później w strefie sesji, z poprawną zmianą daty przez północ. Koniec można następnie poprawić ręcznie. Kolejna zmiana startu ponawia podpowiedź. Niepełna/błędna data albo nieistniejący czas DST nie przepisują końca. Edycja zapisanej sesji zachowuje dotychczasowy koniec, a timer live zachowuje rzeczywisty czas.
- **Repeat session:** przycisk obok Edit session prowadzi do `/sessions/:id/repeat`. Najpierw wybiera się datę/czas, z miejscem źródłowej sesji już ustawionym i tylko do odczytu. **Continue to entries** pokazuje wszystkie skopiowane wpisy, włącznie z rozgrzewką i próbami, do normalnej edycji/dodawania/usuwania. **Change date** wraca do kroku daty bez utraty wpisów.
- Kopia zachowuje nazwę, strefę, miejsce, stabilne `climbId`, kolejność, wyceny/profile, ruchy, asekurację i notatki. Daty rozpoczynają nowy domyślny przedział godzinny, zamiast kopiować daty starego treningu. OS/Flash mają sugestię RP z widocznym wyjaśnieniem, bo wspinaczki są już znane. Użytkownik nadal może poprawić styl.
- Powstają nowe `clientSessionId` i `clientEntryId`; historia, potwierdzenia wyjątków OS/Flash, wersja korekty i stare obliczenia nie są kopiowane. Zapis korzysta ze zwykłego POST i ponownych obliczeń backendu; źródłowa sesja pozostaje bez zmian.
- Szkic powtórki jest oddzielony po użytkowniku i źródłowym ID: `draftKey(userId, 'manual', sourceSessionId)`. Nie nadpisuje zwykłego manual/live/edit ani powtórki innej sesji. Opcjonalne metadane kroku mieszczą się w dotychczasowym schemaVersion 1. Odświeżenie przywraca datę, etap, wpisy i dokładny pendingRequest; retry nie generuje nowych ID.

Nowe źródła: `RepeatSessionPage`, `session-repeat.ts` i opcjonalne metadane `SessionDraft`; API i baza bez zmian. Testy: 5 nowych przypadków modelu (daty/DST, nowe ID, zachowanie danych i brak starego audytu) oraz 4 browser (obie poprawki, edycja kopii, odrębne szkice, odświeżenie i identyczny retry). Pełny zestaw: **16/16 model, 26/26 Edge**, exit 0, 13,8 s. Build/lint poprawne. Testy przeglądarkowe używały kontrolowanych odpowiedzi API; w tym przyroście nie tworzono sesji w lokalnej bazie.

## Statistics i piramida osiągnięć — wdrożone 2026-11-04

Backend i baza są gotowe od 2026-11-03, local V22, 120 testów backendu. Frontend `/statistics` jest wdrożony: filtry w URL, rzeczywiste warstwy i luki, stronicowane listy i powrót z sesji, maksimum oraz informacyjna estymacja. Źródło kontraktu: [achievement-pyramid-backend](achievement-pyramid-backend.md). [Raport frontendu](statistics-frontend.md) podaje zakres, testy oraz kierunek kolejnych zestawów miesięcznych i sezonowych. Estymacja nie zmienia poziomu do loadu.

Pełny plan backendu i frontendu z 2026-11-03: [achievement-pyramid-implementation-plan](achievement-pyramid-implementation-plan.md). Pierwszy proponowany przyrost P01–P13 obejmuje raport, estymację informacyjną, API, Statistics i testy; podłączenie poziomu do loadu jest opisane jako osobny etap do ustalenia. Wybrany algorytm to dotychczasowy ważony Pyramid Support, wariant pełnych warstw odrzucono.

Wybory: `Boulders / Routes & circuits / Combined — points`, `All time / Last 2 years` oraz `RP (includes OS & Flash) / OS only` we wszystkich trzech widokach. Jedna wspinaczka to jeden element po filtrach; wędka nigdy nie jest osiągnięciem i nie trafia do żadnej piramidy. Wybrany snapshot to pierwsze kwalifikujące przejście w okresie. Wiążące reguły i przykłady: [achievement-pyramid](../business/achievement-pyramid.md), DEC-045. Pozostałe OPEN-20 dotyczy integracji poziomu z loadem. Rzeczywisty test Statistics odczytuje historię; wspólne dodanie/edycja sesji v2 przez UI pozostają krokiem odbioru.

## Plan dostosowania do modelu v2 — 2026-11-01

Status: **wdrożony i sprawdzony automatycznie** po zatwierdzeniu planu przez użytkownika. Backend i baza v2 są gotowe; [kontrakt API](api.md#aktualny-kontrakt-modelu-v2--2026-11-01) i [model obliczeń](../business/grading-and-scoring.md#zatwierdzona-zmiana-v2--edl-zastępujemy-przez-profileintensityindex) pozostają źródłem reguł. Odbiór przez rzeczywiste UI/API jest kolejnym krokiem.

### Wdrożony zakres

1. **Typy danych i usunięcie EDL.** `session.types.ts` i `catalog.types.ts` zawierają `profileIntensityIndex`, `crux` i wersję wyniku w podsumowaniach. Usunięto `baseEdl` i `edlCount` z typów i fixture'ów oraz EDL z UI. Historyczne pola backendu są pomijane.
2. **Details — wynik i charakter wpisu.** Oceniany wpis pokazuje nazwę profilu, Adjusted Load i średnią intensywność ruchów. W rozwijanym `Calculation details` znajdują się `Intensity index` ze snapshotu, Classic Load, wersja modelu i dotychczasowe mnożniki.
3. **Details — crux.** Komponent `CruxSplit` pokazuje `Estimated difficulty split`: dwa wiersze z liczbą ruchów, orientacyjną etykietą Font i intensywnością otrzymanymi z API. Próba 40/50 pokazuje 6 trudniejszych i 34 łatwiejsze ruchy. Opis wyraźnie oznacza szacunek; etykiety nie trafiają do osiągnięć. Dla profilu bez cruxa panelu nie ma, rozgrzewka nie pokazuje indeksu ani intensywności.
4. **Formularze i szkice.** Istniejące pola pozostają wystarczające. Formularz pokazuje opis wybranego profilu z API i krótkie wyjaśnienie cruxa; nie oferuje cruxów dla balda. Kod profilu jest sprawdzany względem aktywnej listy przy dodaniu/edycji wpisu oraz ponownie dla całej sesji przed nowym żądaniem. Niedostępny katalog można ponownie załadować. Retry pomija nową walidację profili.
5. **Dashboard, podsumowanie i kalendarz.** Układ i wyniki z backendu są zachowane. Typy uwzględniają wersję; podsumowanie ma dyskretną informację dla v1 lub wyniku mieszanego. React nie przelicza intensywności, loadu ani cruxa.
6. **Weryfikacja.** Fixture'y, testy modelu i przeglądarki obejmują v2 oraz zachowanie starszych danych. Zapis/odczyt/edycja przez rzeczywisty backend V20 pozostają do sprawdzenia przy wspólnym dodaniu sesji w UI. Opcjonalny test `live-api.spec.ts` ma już asercje v2, ale dziś nie był uruchamiany.

### Zachowanie przy wycofanym profilu

`SHORT_CONTINUOUS_ROUTE` nie jest już dostępny w nowych wyborach. Może jednak pozostać w otwartym szkicu, danych edytowanej sesji, katalogu lub powtarzanym wpisie. Formularz zachowuje kod w wyłączonej opcji `Unavailable profile: …` i wymaga jawnego ponownego wyboru. Nazwa, ruchy, notatki i identyfikatory pozostają zachowane. Gotowy wpis jest również oznaczony na liście sesji i blokuje nowy zapis do korekty.

Klucze szkiców i `schemaVersion: 1` są zachowane — wersja formatu szkicu jest niezależna od modelu obliczeń. Szkic zawiera polecenie, a nie snapshot kalkulatora. `pendingRequest` pozostaje dokładnym zapisanym żądaniem do ponowienia, także przy zmianie lub niedostępności katalogu; korekta jest możliwa po jednoznacznym odrzuceniu żądania i odblokowaniu edycji.

### Granica implementacji

React prezentuje zapisany wynik i podział otrzymany z API. Wzory, zaokrąglenie 15% i kalibracja wycen pozostają wyłącznie w backendzie. Nie dodano podglądu obciążenia przed zapisem ani nowych wymaganych danych wejściowych.

## Weryfikacja v2 — 2026-11-01

- `npm run build` i `npm run lint`: sukces. ESLint pomija generowane raporty Playwright.
- `npm test`: **6/6**; dodatkowo potwierdzono zachowanie starego profilu i niedokończonego wpisu w szkicu oraz wysyłanie wyłącznie danych wejściowych podczas edycji.
- `npm run test:e2e -- --workers=2`: **13/13, kod zakończenia 0**, headless Edge, 9,7 s. Kontrolowane odpowiedzi API obejmują bald 6B (9,25/37), średnią ciągową 7a (6,3/126), długą 120-ruchową (4,5/540), obwód z indeksem 7, rozgrzewkę, crux dla przejścia i próby 40/50, edycję do 41 ruchów z nowym podziałem z API, odczyt v1 oraz dotychczasowe CRUD/timer/kalendarz.
- Przypadki profili: naprawa gotowego wpisu ze starym profilem, odzyskanie niedokończonego formularza, powtarzanie wpisu, wybór z katalogu i niezgodny crux dla balda. Retry przy niedostępnym katalogu wysłało identyczny `pendingRequest`.
- Obejrzano zrzut mobilny Details przy 390 px; test potwierdził brak poziomego przewijania. Zrzuty w ignorowanym `frontend-web/test-results/v2-details-desktop.png` i `v2-details-mobile.png`.
- Bez dzisiejszego testu zapisu przez rzeczywisty backend i bez tworzenia sesji w lokalnej bazie. Ten odbiór wykonamy w następnym kroku z użytkownikiem.

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
    statistics/        raporty okresowe: piramida, filtry URL, estymacja i listy
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
6. **Statistics `/statistics`**: szczegółowe raporty z dłuższych okresów; pierwszym jest piramida osiągnięć z trzema dyscyplinami, RP/OS, all-time/2 lata, listami warstw, maksimum i informacyjną estymacją. [Szczegóły](statistics-frontend.md).

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
