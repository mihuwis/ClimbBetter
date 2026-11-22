# Statistics — frontend

Data aktualizacji: 2026-11-07. Status: piramida oraz raport miesiąca/roku Training summary wdrożone na polecenie użytkownika. Najnowsza weryfikacja: build/lint,30model/37browser i osobny odczytowy browser z prawdziwym raportem okresowym przeszły. Backend210testów,18raportów porównanych z historią17sesji/137wpisów bez zmian danych. Backend8080 wymaga restartu do nowego endpointu. [Raport okresowy](period-training-summary.md), [backend piramidy](achievement-pyramid-backend.md).

## Miejsce w aplikacji

Przyrost2026-11-07 z planu2026-11-04: Training summary dla miesiąca i roku, inspirowany Polar Flow. Pełny czas pasujących sesji mieszanych z opisem oraz koło udanych stylów unikalne po climbId, OS > Flash > RP, bez prób i wędki (DEC-048). Osobny raport i backend, Recharts ładowany dopiero po wyborze, filtry/ustawienia w URL,6kafli, wykres słupkowo-liniowy,2pierścienie i wyróżnienia z linkami do sesji. Stare URL piramidy zachowane. [Implementacja i testy](period-training-summary.md).

`/statistics` i pozycja **Statistics** w głównym menu tworzą miejsce na szczegółowe raporty z dłuższych okresów. Pierwszym zestawem jest **Achievement pyramid**. Strona składa raport jako osobny komponent `features/statistics`; typy i pobieranie danych nie należą do modelu sesji.

Użytkownik wskazał 2026-11-04 kierunek kolejnych zestawów: miesiąc, sezon, dwa lata i cała historia. Obecna piramida zachowuje uzgodnione zakresy ostatnich dwóch lat i all-time. Miesiąc i sezon nie są pozornie dostępnymi opcjami. Rozszerzenie okresów wymaga kontraktu backendu i określenia granic sezonu, np. jawnego przedziału dat; nie interpretujemy sezonu automatycznie jako roku kalendarzowego. Poszczególne przyszłe raporty mogą mieć własny wybór okresu.

## Wdrożone zachowania

- Trzy dyscypliny, dwa okresy i dwa style: łącznie 12 kombinacji. Domyślne: `BOULDER`, `LAST_TWO_YEARS`, `RP` obejmujące OS i Flash. OS wybiera wyłącznie OS.
- Parametry `view`, `period`, `style`, `asOf`, wybrana `bucket` i `offset` mieszczą się w URL. Zmiana filtra usuwa poprzednią warstwę i stronę. Niepoprawne filtry/daty przy otwarciu adresu mają obsługiwane wartości domyślne.
- Frontend wysyła lokalny dzień jako `asOf` i pokazuje faktyczne granice okna zwrócone przez API. Nie przelicza wycen, unikalności, poziomu ani loadu.
- Warstwy zachowują kolejność i zerowe stopnie z API. Wycentrowane słupki mają szerokość proporcjonalną do liczności, wspólne kolumny i jawne etykiety/liczby. Nie wymuszają idealnego kształtu piramidy.
- Kliknięcie lub klawiatura otwierają listę warstwy; 50 pozycji na stronę, `nextOffset` z API. Fokus przechodzi do nagłówka listy, zamknięcie oddaje go wybranej warstwie. Dostępne są oryginalne wyceny/skale, miejsce, data, styl i link do źródłowej sesji.
- Powrót z sesji działa przez historię przeglądarki oraz **Back to statistics**. Zachowuje filtry, warstwę i stronę, ponownie odczytując raport. Powrót nie używa trwałego cache dawnych wyników.
- Podsumowanie rozróżnia liczbę różnych wspinaczek, maksimum i informacyjną estymację. Mała próbka oraz fallback mają **Provisional level**, low confidence i wyjaśnienie zamiast oznaczenia pełnego wsparcia piramidą. Metodę/dowody można rozwinąć. Wynik zawsze dotyczy wybranego okresu i stylu, nie zmienia odniesienia dla loadu.
- COMBINED zachowuje dokładne punkty, np. 90 i 92.5 osobno. Lista pokazuje pierwotne skale. Kalibracje mają osobne grupy; przy kilku wersjach nie ma wspólnego maksimum. Brak wspólnego estymatora jest jawnie opisany.
- Loading, błędy raportu/listy i retry, puste osiągnięcia, zerowa warstwa, zmiana danych między raportem a listą. `useResource` anuluje poprzednie odczyty i ignoruje nieaktualne odpowiedzi. **Refresh report** pobiera także ponownie otwartą listę.

## Weryfikacja 2026-11-04

- `npm run build`, `npm run lint`: sukces. `npm test`: **11/11**, w tym 5 nowych przypadków URL, lokalnych dat, granic i punktów.
- `npm run test:e2e`: **22/22**, exit 0, 10,2 s, headless Edge. 13 dotychczasowych scenariuszy sesji oraz 9 Statistics: 12 kombinacji, luki i estymacja, dokładne punkty i mieszane kalibracje, 51 wspinaczek/paginacja, odświeżenie/powrót, błędy/retry, brak i mała próbka, fallback, zmiana filtra w trakcie żądania, telefon i klawiatura.
- Po dodaniu zrzutów oraz jawnego linku powrotnego w scenariuszu testowym trzy zmienione testy przeszły ponownie: **3/3**, exit 0. Obejrzano desktop i telefon 390 px; bez poziomego przewijania, widoczne liczby, wyceny, fokus oraz długie nazwy.
- Osobny opt-in `CB_REAL_STATISTICS_API=1 npm run test:e2e`: **1/1**, exit 0, 3,9 s. Bez mocków, konto Mihu i trwały PostgreSQL V22: UI/API zgodne we wszystkich 12 kombinacjach, niepuste listy i ich liczniki, źródłowa sesja i jawny powrót, telefon 390 px. Test tylko odczytuje istniejącą historię.
- Przed/po realnym teście: identyczne skróty pełnych rekordów sesji, wpisów i revisions; 19 sesji, 145 wpisów, Classic Load 12835.4207, Adjusted Load 24460.7012. Nie dodano/edytowano/usunięto sesji ani migracji w tym przyroście.

Kontrolowane testy UI pokrywają bogatsze przykłady i błędy; lokalna historia Mihu jest mała i nie dostarcza każdej kombinacji osiągnięć. Nowy test nie zastępuje pełnego odbioru P13 na specjalnym zestawie danych i ręcznej oceny użytkownika. Dotychczasowe testy backendu obu przykładów i CRUD pozostają dowodem z poprzedniego przyrostu; nie uruchamiano ponownie Maven przy zmianach wyłącznie frontendu.

## Uruchomienie i testy

Standardowe uruchomienie jest w [frontend-web](frontend-web.md#uruchomienie): backend local na 8080, użytkownik Mihu, frontend na 5173. W tej sesji uruchomiono oba procesy lokalnie, a backend korzysta z wcześniej zweryfikowanej, odizolowanej kompilacji tych samych źródeł.

Testy kontrolowane uruchamia `npm run test:e2e` na 5174. Odczyt prawdziwego API wymaga uruchomionego backendu i jawnej flagi. PowerShell, w `frontend-web`:

```powershell
$env:CB_REAL_STATISTICS_API='1'
npm.cmd run test:e2e
Remove-Item Env:CB_REAL_STATISTICS_API
```

Ta flaga wybiera wyłącznie `statistics-live-api.spec.ts`. Dotychczasowe `CB_REAL_API=1` nadal wybiera test CRUD, który tworzy/edytuje/usuwa sesję; nie jest potrzebny do odczytowej weryfikacji Statistics.
