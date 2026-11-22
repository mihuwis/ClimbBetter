# Achievement pyramid — implementation plan

Data aktualizacji: 2026-11-04. Status: backend wdrożony, 120 testów przeszło, local na V22; frontend P10–P12 również wdrożony i sprawdzony. Realny odczyt UI/API wszystkich 12 kombinacji potwierdzony. Pełniejszy odbiór P13 na zestawie danych z zapisem sesji pozostaje. [Kontrakt backendu](achievement-pyramid-backend.md), [raport frontendu](statistics-frontend.md). Reguły wiążące: [achievement-pyramid](../business/achievement-pyramid.md), DEC-045 i ważony Pyramid Support z [OPEN-20](decisions.md#open-20--zakres-estymacji-poziomu-przy-wdrożeniu-piramidy).

## Zakres proponowanego pierwszego wdrożenia

Statistics ma prezentować piramidę rzeczywistych osiągnięć, listy wspinaczek w warstwach, maksimum oraz estymację poziomu dla pojedynczych skal. Wybory to trzy widoki dyscypliny × dwa okresy × dwa style, łącznie 12 kombinacji:

- `BOULDER`, `ROUTE_AND_CIRCUIT`, `COMBINED`;
- `ALL_TIME`, `LAST_TWO_YEARS`;
- `RP` (RP + OS + Flash), `OS` (tylko OS).

Wędka nigdy nie jest osiągnięciem. Jedna wspinaczka liczy się raz po wybraniu okresu i stylu; wybieramy pierwszy kwalifikujący snapshot. Wspólny widok grupuje dokładnie równe gradePoints, bez zaokrąglania lub nowych przeliczeń.

Wdrożony backend i frontend pokazują estymację jako wynik raportu. Włączenie jej do poziomu używanego przy obliczaniu loadu pozostaje osobnym etapem OPEN-20. Część odczytowa P13 została wykonana przez rzeczywiste UI, HTTP i bazę, bez tworzenia sesji; pełny odbiór na nowym zestawie danych oraz ręczna ocena użytkownika pozostają.

## Kolejność prac — pierwszy przyrost

### P01 — kontrakt i przykłady akceptacyjne

- [x] Utrwalić parametry filtrów, kształt warstw, danych listy wspinaczek i estymacji. Backend zwraca faktyczne granice okna i zastosowane filtry.
- [x] Oprzeć okres na lokalnych `sessionDate`, z jawną datą odniesienia `asOf`. Propozycja: frontend przesyła bieżącą lokalną datę jako `YYYY-MM-DD`, backend używa jej jako granicy raportu. Dla `LAST_TWO_YEARS` zakres od `asOf.minusYears(2)` do `asOf`, obie granice włącznie; `ALL_TIME` również kończy się na `asOf`. Domyślny raport nie obejmuje przyszłych sesji.
- [x] Ustalić deterministyczny porządek pierwszego kwalifikującego przejścia: data sesji, początek sesji z istniejącym fallbackiem do createdAt, stabilne ID sesji, entryOrder, ID wpisu.
- [x] Przygotować dwa zatwierdzone przykłady estymacji: 6c+ dla pierwszego zestawu oraz 7b ze wsparciem 3,25 dla drugiego (7b+ = 2,25).

Rezultat: kontrakt do implementacji i dane z jednoznacznie oczekiwanymi wynikami.

### P02 — wspólna kwalifikacja osiągnięcia

- [x] Wprowadzić jedną regułę domenową: `ASCENT` oraz dla `ROUTE` koniecznie `LEAD`; dla baldów i obwodów asekuracja nie występuje. Wspólne źródło SQL i testy mają zapewniać tę samą kwalifikację w odczytach.
- [x] Zastosować regułę do piramidy, personal bests, maksymalnej wyceny sesji i Fast RP w nowych zapisach/edycjach.
- [x] Zachować TOP_ROPE w ruchach, loadzie, intensywności i historii kontaktów. Wykluczenie z osiągnięć nie zmienia treningu w próbę i nie usuwa znajomości drogi.
- [x] Nie rozszerzać przy okazji istniejącej kategorii personal best `rp` o OS/Flash; szeroka semantyka dotyczy nowego widoku piramidy RP.

Miejsca do zmiany: `TrainingSessionService`, `DashboardSessionRepository` i nowy raport. Polityka kwalifikacji oraz estymator powinny mieć API umożliwiające współdzielenie między `training` i `reporting` bez dostępu do cudzych pakietów `internal` i bez cyklicznych zależności.

### P03 — spójny odczyt istniejącej historii

- [x] Skontrolować utrwalone `max_route_grade` i `fast_rp`, w których dotychczas mogła pojawić się wędka.
- [x] Zalecany sposób: wyprowadzać aktualną projekcję osiągnięć z kwalifikujących wpisów i ich snapshotów, również dla dawnych sesji. Historyczny TOP_ROPE nie może być zwracany jako maksimum ani Fast RP.
- [x] Zastosować poprawny odczyt w Dashboardzie, Details i Calendar, nie tylko w nowym Statistics. Osobiste rekordy już mają dynamiczny odczyt i wymagają prawidłowego filtra.
- [x] Zachować zapisane loady, intensywności, dane wejściowe, revision JSON i fingerprinty żądań. Do naprawy osiągnięć nie uruchamiać zwykłej edycji/przeliczenia sesji.

Jeśli przy implementacji zamiast projekcji zostanie wybrana naprawa utrwalonych pól pochodnych, potrzebny będzie oddzielny, audytowany plan korekty wyłącznie osiągnięć. Domyślny plan zakłada projekcję odczytu.

### P04 — zapytanie pełnej historii i deduplikacja

- [x] Dodać źródło raportu w `reporting`: bieżący użytkownik, nieusunięte sesje, okres, kwalifikacja, dyscyplina i styl.
- [x] Dopiero po tych filtrach wybrać jeden najwcześniejszy wpis per `climbId` zgodnie z P01. Osobno nazwane wspinaczki o różnych ID pozostają różnymi osiągnięciami.
- [x] Odczytać gradeLabel, gradeIndex, gradePoints i gradeCalibrationVersion ze snapshotów. Archiwizacja wspinaczki lub profilu w katalogu nie usuwa dawnego osiągnięcia.
- [x] Grupować wybrane wpisy po stopniach odpowiedniej skali, a dla COMBINED po punktach. Nie pobierać historii z listy Dashboardu ograniczonej do 20 sesji.
- [x] Stosować identyczne filtry i deduplikację dla liczników warstw oraz listy wspinaczek otwieranej po kliknięciu.

### P05 — baza i wydajność

- [x] Wykorzystać istniejące wpisy, snapshoty i indeksy. Na początek nie tworzyć tabeli przechowującej osobną piramidę ani cache wymagającego synchronizacji po edycjach.
- [ ] Sprawdzić plan zapytania na reprezentatywnej większej historii. Dodać migrację widoku/indeksu wyłącznie wtedy, gdy wybrana implementacja lub pomiar tego wymagają; nie zmieniać zastosowanych migracji.
- [x] Dla dużej warstwy zastosować stronicowanie listy wspinaczek. Warstwy i podsumowanie mogą być zwracane w jednej małej odpowiedzi.

### P06 — ważony Pyramid Support

- [x] Wydzielić czysty estymator dla pojedynczej skali, niezależny od HTTP i SQL; w przyszłości możliwy do ponownego użycia przy poziomie odniesienia.
- [x] Użyć tej samej deduplikowanej próbki, co raport dla wybranego okresu i stylu. Stosować wagi 1/0,5/0,25, limity 2/4/8 i próg 3,00; szukać najwyższego kwalifikującego kandydata, nie największej wartości support.
- [x] Zachować zasady małej próbki 1–9, dotychczasowy fallback braku kandydata oraz confidence. Wynik zawiera metodę i pewność, aby fallback nie był przedstawiany jako pełne wsparcie piramidą.
- [x] Dla pustej próbki osiągnięć raport zwraca brak estymacji. Próby i wędka nie tworzą elementów piramidy; treningowy fallback do obliczeń loadu pozostaje osobną sprawą.
- [x] Zwracać wycenę oszacowaną, faktyczne maksimum, sampleSize, supportScore gdy ma zastosowanie, surowe liczności warstw, confidence, metodę i algorithmVersion.
- [x] Dla all-time wynik oznaczyć jako poziom poparty całą historią, dla ostatnich dwóch lat jako poziom poparty tym okresem. OS/RP również określają próbkę; wynik OS nie zastępuje ogólnego poziomu do loadu.

### P07 — wspólna piramida punktowa

- [x] Łączyć wyłącznie dokładnie równe gradePoints. 7a i 6A+ mają wspólną warstwę 90; 6B ma osobną warstwę 92,5.
- [x] Zachować informację o skali i etykiecie każdego osiągnięcia w liście szczegółów; warstwa pokazuje liczbę różnych wspinaczek, nie sumę punktów.
- [x] Uwzględnić gradeCalibrationVersion. Obecnie obowiązuje kalibracja 1; przy odmiennych wersjach rozdzielić nieporównywalne grupy lub zwrócić jawną informację wymagającą wyboru kalibracji, bez cichej normalizacji. Dokładny kontrakt tego rzadkiego przypadku dopracować w P01, przed kodowaniem.
- [x] W pierwszym wdrożeniu zwracać rozkład i maksimum punktowe bez wspólnego oszacowanego poziomu: reguł takiego estymatora jeszcze nie ustalono.

### P08 — API Statistics

- [x] Dodać kontroler i query w `reporting`. Propozycja: `GET /api/v1/statistics/pyramid` z parametrami `view`, `period`, `style`, `asOf`.
- [x] Odpowiedź: filtry i granice okresu, liczba różnych wspinaczek, uporządkowane warstwy z identyfikatorem/licznością/etykietą lub punktami, maksimum i estymacja tam, gdzie jest zdefiniowana.
- [x] Dodać stronicowany odczyt wspinaczek warstwy, np. `GET /api/v1/statistics/pyramid/climbs`, z tymi samymi filtrami i identyfikatorem warstwy z odpowiedzi podsumowania. Każda pozycja zawiera climbId, nazwę, miejsce, typ, snapshot wyceny, datę, styl i sessionId reprezentatywnego przejścia.
- [x] Walidować parametry i daty; użytkownika pobierać z istniejącego CurrentUserProvider. Zapytanie nie przyjmuje dowolnego userId od klienta i nie ujawnia cudzych/usuniętych danych.
- [x] Opisać kontrakt w OpenAPI i dokumentacji API. Spójny widok odczytu zapobiega rozbieżnym licznikom i listom w obrębie odpowiedzi; zmiana danych między żądaniami wymaga odświeżenia raportu.

### P09 — testy backendu

- [x] Jednostkowe: kwalifikacja osiągnięć; estimator, oba zatwierdzone przykłady, capy, progi, mała próbka, puste dane, fallback i confidence. Twarde warstwy odrzuconego wariantu nie są implementowane.
- [x] Integracyjne PostgreSQL: cztery przejścia jednej drogi = jeden element; powtórzenie po pięciu latach; OS dawno/RP teraz; Flash w RP i brak w OS; wędka nigdy, także przy OS/Flash/RP; drogi/obwody; takie same nazwy i różne ID.
- [x] Zmienne wyceny i pierwszy kwalifikujący snapshot, kolejność remisów, katalog archiwalny, granice okresu i rok przestępny, cudze/usunięte/przyszłe względem asOf sesje, więcej niż 20 sesji.
- [x] Pełne 12 kombinacji filtrów, dokładne równości punktów i 92,5 jako osobna warstwa, zgodność liczników ze stronicowaną listą.
- [x] Regresja historii: TOP_ROPE wyłączone z dawnych i nowych osiągnięć, lecz ruchy/load i historia kontaktów pozostają; odczyt raportu nie zmienia zapisanych wyników, audytu ani fingerprintów.
- [x] Zmiany po dodaniu, edycji i usunięciu sesji oraz weryfikacja granic modułów. Następnie pełny zestaw backendu.

### P10 — frontend: Statistics i integracja

- [x] Dodać `/statistics`, pozycję menu i nową funkcję `features/statistics` z typami DTO, pobieraniem raportu i komponentami piramidy. Model sesji pozostaje w `training`.
- [x] Dodać trzy wybory: dyscyplina, okres, styl. Domyślne: baldy, ostatnie dwa lata, RP obejmujące OS i Flash; wartości w URL umożliwiają odświeżenie i powrót do widoku.
- [x] Przekazywać asOf i pokazywać faktyczny zakres zwrócony przez API. React nie liczy unikalności, wsparcia ani punktacji.
- [x] Obsłużyć loading, błąd i ponowienie, brak osiągnięć, brak estymacji i zmianę filtrów podczas trwającego żądania. Wykorzystać istniejący useResource z anulowaniem nieaktualnych odpowiedzi.
- [x] Po powrocie z sesji ponownie odczytywać Statistics bez trwałego cache; dostępne jest też jawne Refresh report, obejmujące otwartą listę. Zapis sesji v2 w rzeczywistym odbiorze pozostaje w P13.

### P11 — frontend: wykres, lista i estymacja

- [x] Narysować rzeczywiste warstwy: trudniejsze u góry, szerokość proporcjonalna do liczności, etykieta i jawna liczba obok. Uwzględnić puste stopnie w wyświetlanym zakresie; nie dopasowywać danych do idealnego kształtu.
- [x] Kliknięcie i klawiatura otwierają stronicowaną listę wspinaczek warstwy, z linkiem do źródłowej sesji. Lista i licznik używają tej samej kwalifikacji backendu.
- [x] Dla baldów i dróg/obwodów pokazać osobno najwyższe przejście i oszacowany poziom, z krótkim wyjaśnieniem metody i pewności. Wynik odpowiada wybranemu okresowi i stylowi.
- [x] W COMBINED pokazać punkty i listę z oryginalnymi skalami. Nie sugerować wspólnego poziomu sportowego, którego reguł nie zdefiniowano.
- [x] Sprawdzić telefon, liczności wymagające paginacji, długie nazwy, obsługę klawiatury i czytelność bez rozróżniania kolorów. Wykres HTML/CSS bez nowej biblioteki.

### P12 — testy frontendu

- [x] Typy/build/lint oraz odpowiednie testy obsługi URL i prezentacji odpowiedzi.
- [x] Playwright: wszystkie filtry, prezentacja estymacji, pojedyncze wpisy z deduplikowanej odpowiedzi API, opis wykluczenia wędki, punkty i paginacja, link do sesji, błąd/retry, pusta historia, odświeżenie i powrót. Regułę wykluczenia/deduplikacji sprawdzają testy backendu, frontend jej nie powiela.
- [x] Dodać nowe pliki do Playwright testMatch: domyślnie sessions/statistics, osobno opt-in realnego CRUD lub odczytowego realnego Statistics.
- [x] Sprawdzić 390 px i brak poziomego przewijania; potwierdzić tekstową dostępność warstw i obsługę klawiaturą.

### P13 — odbiór przez rzeczywisty backend i bazę

- [x] Uruchomić Docker/PostgreSQL, aktualny backend i frontend. Baza pozostaje na V22, nowe migracje frontendu nie były potrzebne.
- [ ] Przygotować uzgodniony zestaw sesji testowych: powtórzenia, OS/Flash/RP, wędka, obwód, bald oraz przejścia starsze i nowsze niż dwa lata.
- [ ] Porównać SQL/API/UI dla wszystkich kombinacji, obu przykładów estymatora i list warstw. Dodać, edytować i usunąć sesję; potwierdzić aktualizację raportu i osiągnięć.
- [x] Potwierdzić, że sama aktualizacja statystyk nie zmieniła historycznych rekordów: skróty sesji, wpisów i audytu oraz loady są identyczne. Regresja retry przeszła w testach backendu oraz kontrolowanych testach UI; realnej edycji w tym odczytowym teście nie wykonywano.
- [x] Zapisać rzeczywiste wyniki testów, zaktualizować postęp i dokumentację, odróżniając wdrożenie od pozostałego pełnego odbioru P13.

## Osobny kolejny etap — poziom używany w loadzie

Te punkty są częścią pełnego planu rozwoju, ale wymagają decyzji OPEN-20 przed implementacją i nie blokują raportu osiągnięć:

1. Ustalić, czy nowy estymator od razu zastępuje maksimum w `ClimberLevelResolver`, czy najpierw pozostaje informacją w Statistics. Rekomendacja: najpierw raport, następnie osobne podłączenie.
2. Ustalić treningowy fallback dla historii obejmującej wyłącznie wędkę lub próby; wędka nadal nigdy nie staje się osiągnięciem. Określić również pierwszą sesję i wpływ wpisów bieżącej sesji.
3. Zdefiniować zapis/okres obowiązywania źródła poziomu, algorithmVersion i confidence, a jeśli potrzebny będzie trwały UserGradeReference — jego migrację, odczyt i audyt.
4. Podłączyć estymację z ostatnich dwóch lat i ogólnej próbki RP/OS/Flash do nowych obliczeń właściwej dyscypliny. Statystyka all-time lub OS nie przełącza automatycznie poziomu odniesienia do loadu.
5. Zachować historyczne snapshoty i retry; nie uruchamiać automatycznej kaskady przeliczania dawnych sesji. Przetestować tworzenie, edycję i fallback oraz dokładną datę odniesienia poziomu.
6. Osobno, dopiero po zdefiniowaniu reguł, rozważyć estimator wspólnego poziomu punktowego. Sam wykres punktowy jest już objęty P07/P11.

## Granica obecnego zadania

Backend i baza zostały wdrożone 2026-11-03, frontend P10–P12 na polecenie użytkownika 2026-11-04. Część odczytowa P13 przeszła przez rzeczywiste UI/API/bazę, bez zmiany historii. Pełniejszy odbiór z nowym zestawem sesji oraz ręczna ocena pozostają. Integracja poziomu z loadem pozostaje osobnym zadaniem. EXPLAIN wykonano na małej lokalnej historii; benchmark reprezentatywnej dużej historii z P05 nadal oczekuje. Statistics ma docelowo mieścić więcej zestawów, również miesięcznych i sezonowych; piramida zachowuje obecne dwa zakresy.
