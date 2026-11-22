# Training summary — miesiąc i rok

Data aktualizacji: 2026-11-07. Status: backend i frontend wdrożone na polecenie użytkownika. Pełny backend 210/210, web build/lint, 30 testów modelu, 37 przeglądarkowych i osobny odczytowy test UI z rzeczywistym API przeszły. Niezależnie sprawdzono 18 raportów na 17 sesjach/137 wpisach; historia, katalog, audyt i V23 bez zmian. Proces użytkownika na 8080 wymaga restartu do nowego kodu raportu. Inspiracja: docs/Podsumowanie_treningow.png z Polar Flow; potwierdzone reguły DEC-048.

## Miejsce i zakres

Osobny raport **Training summary** w Statistics, obok istniejącego Achievement pyramid. Miesiąc i rok kalendarzowy, wybór daty i strzałki poprzedni/następny okres. Dyscypliny: All, Boulders, Routes & circuits. Rok pokazuje miesiące, miesiąc — dni. Kliknięcie miesiąca otwiera jego raport, kliknięcie dnia — sesje. Dane tabelaryczne zapewniają te same akcje z klawiatury. Dotychczasowe URL Statistics nadal otwierają piramidę; nowy raport ma report=summary. Filtry, wybrane linie/słupki i dzień są zapisane w URL, a link Back to statistics odtwarza widok po otwarciu źródłowej sesji.

Zakres wybieramy według zapisanego lokalnego sessionDate, od początku okresu włącznie do początku następnego wyłącznie. Sesja przechodząca przez północ należy w całości do swojej daty treningowej. Nieusunięte dane bieżącego użytkownika, bez ograniczenia do ostatnich 20 sesji Dashboardu. Raport czyta snapshoty loadu i wycen; nie przelicza historii według dzisiejszego poziomu.

## Potwierdzone decyzje DEC-048

Po wyborze dyscypliny pokazujemy **pełny czas pasujących sesji**, z opisem „czas sesji zawierających tę dyscyplinę”. Sesja mieszana należy do obu filtrów, ale do All tylko raz. Liczba sesji i czas z filtrów nie są addytywne. Nie szacujemy podziału czasu proporcją ruchów; w danych jest tylko czas całej sesji, obejmujący odpoczynki.

Koło stylów liczy **różne wspinaczki: jeden climbId raz**, według najlepszego udanego stylu w wybranym okresie: **OS > Flash > RP**. Najpierw filtrujemy użytkownika, aktywne sesje, okres i dyscyplinę, potem kwalifikujące osiągnięcia, na końcu grupujemy po climbId i wybieramy styl.

Kwalifikują się wyłącznie ASCENT, a dla ROUTE dodatkowo LEAD. ATTEMPT, WARMUP i TOP_ROPE nie tworzą elementu koła. Próby i wędka pozostają częścią ruchów i loadu treningowego.

Przykłady dla jednej wspinaczki:

- OS, następnie RP w okresie → OS 1, RP 0.
- OS poza okresem, RP w okresie → RP 1.
- Nieudana próba OS, potem udane RP w okresie → RP 1.
- Ukończone OS na wędkę bez innego osiągnięcia → brak elementu.

Sektory OS, Flash i RP są rozłączne; ich suma to liczba różnych kwalifikujących wspinaczek. RP koła oznacza wyłącznie kategorię RP. Istniejąca piramida RP nadal obejmuje OS/Flash/RP i zachowuje własne reguły snapshotów. Nie zmieniamy zapisanych deklaracji stylu ani liczenia pracy treningowej.

Unikalność ustalamy dla całego wybranego okresu. Rocznego koła nie tworzymy przez dodanie kół miesięcznych: ta sama wspinaczka może pojawić się w kilku miesiącach, ale w roku tylko raz.

## Wdrożone metryki i wykresy

Kafle: liczba sesji, łączny i średni czas sesji, ruchy, suma i średnia sesyjna Adjusted Load oraz Classic Load, aktywne dni. Dodatkowo liczba różnych ukończonych wspinaczek z definicji koła stylów.

Słupki: jeden wybór spośród liczby sesji, czasu i wykonanych ruchów. Linie: maksymalnie trzy serie wybrane z sumy loadu, maksymalnego loadu sesji i średniego loadu na sesję — każda osobno Classic lub Adjusted. Domyślnie ruchy jako słupki i suma Adjusted Load jako linia. Jedna oś dla jednostki słupków, druga dla loadu; intensywność o innej jednostce zostaje na późniejszy osobny wykres. Wybranie trzech linii blokuje pozostałe, odznaczenie ponownie je udostępnia. Zmiana serii nie pobiera ponownie raportu.

Load sesji to suma odpowiednich wpisów, a nie największy pojedynczy wpis. Po filtrze dyscypliny ruchy i load obejmują wyłącznie pasujące wpisy; liczba sesji i pełny czas obejmują sesje z tymi wpisami. Średnia = suma loadu / liczba pasujących sesji, bez średniej średnich dziennych lub miesięcznych. Rozgrzewki wchodzą do ruchów All, mają zero loadu i nie mają dyscypliny. Pusty koszyk ma sumy/liczby 0, a maksimum i średnią NULL. Backend zwraca wszystkie dni/miesiące, także puste.

Wyróżnienia: sesje o najwyższym Classic i Adjusted Load, najtrudniejszy bald i droga/obwód, najwyższe udane OS/Flash/RP osobno dla obu skal. Linki do sesji źródłowych, bez wędki w osiągnięciach. Ranking według zapisanej wyceny, nie punktacji przeliczonej dzisiaj.

Pierścień dyscyplin: udział ocenianych ruchów baldów oraz dróg/obwodów; rozgrzewka pokazana osobno poza mianownikiem. Pierścień stylów: potwierdzona reguła DEC-048. Kolory, etykiety, wartości i podpowiedzi spójne z CB. Na telefonie ustawienia wykresu mogą być zwijane.

Biblioteka: Recharts 3.10.1 i react-is 19.2.6 zgodny z React 19.2.6. Wykres komponowany i dwa pierścienie mają spójny styl CB; raport ładowany osobnym lazy chunk, bez dodawania biblioteki do początkowego bundle Dashboardu/piramidy. Build: główny JS 299 KB, raport 437 KB (około 123 KB gzip). [Przykład słupków i linii](https://recharts.github.io/en-US/examples/LineBarAreaComposedChart/), [pierścień](https://recharts.github.io/en-US/examples/PieChartWithPaddingAngle/).

## Implementacja

1. Backend reporting: PeriodSummaryRequest, PeriodTrainingSummary, PeriodSummaryQuery, repository, serwis i controller. GET /api/v1/statistics/summary?period=MONTH&year=2026&month=11&discipline=ALL; rok bez parametru month. Year 1…9998, month 1…12 wymagany tylko dla MONTH, domyślne period=MONTH i discipline=ALL. Niepoprawny filtr daje 400 INVALID_PERIOD_SUMMARY_REQUEST.
2. Kontrakt zwraca filters, windowStart/windowEnd (włącznie), summary, uniqueClimbCount, warmupMoves, buckets z lekką listą sesji, disciplineBreakdown, styleBreakdown i highlights z linkami. Osiągnięcia wykorzystują istniejącą kwalifikację reporting.achievement_ascents. Dwa odczyty w REPEATABLE_READ, bez nowych zapisów snapshotów.
3. Agregacja wpisów najpierw do jednego rekordu na sesję, potem do dni/miesięcy. JOIN wpisów nie może zwielokrotniać czasu sesji. Koło stylów deduplikujemy niezależnie dla całego okresu.
4. Obecny schemat wystarcza; nie potrzeba migracji danych ani nowych pól formularza. Dodatkowy indeks tylko po ocenie zapytania. Nie zapisujemy czasu per dyscyplina i nie zmieniamy kalkulatora loadu.
5. Frontend: wybór raportu w StatisticsPage, osobny komponent Training summary oraz typy i filtry URL, Recharts, stany loading/error/empty, responsywność i odświeżenie po powrocie z edycji.
6. Testy: mieszana sesja liczona raz, pełny czas bez mnożenia przez wpisy, częściowy load po filtrze, kilka sesji jednego dnia, luty przestępny i granice roku, brak treningów, rozgrzewka/wędka/próby, cztery przykłady stylów, deduplikacja w roku, snapshoty, usunięcia i izolacja kont. UI: okres/dyscyplina, limit trzech linii, telefon i linki do sesji.

## Weryfikacja i uruchomienie

Pełny Maven offline przez izolowany backend/target/v2-verification/pom.xml: **210/210**, bez failures/errors/skipped, BUILD SUCCESS, 35,181 s. Nowe 34 przypadki: 17 HTTP/controller i 17 integracyjnych, w tym sesje mieszane, częściowe loady, średnie/max, osiągnięcia, deduplikacja roku, luty przestępny, granice roku, ponad 20 sesji, niezmienność snapshotów i izolacja kont. Log: period-summary-full-test.log. Baza nie wymagała migracji.

Web build i lint przeszły, **30/30** testów modelu i **37/37** kontrolowanych Edge, 15,5 s. W tym 6 nowych modelowych i 6 przeglądarkowych. Sprawdzono filtry, limit linii, brak zbędnego pobierania po zmianie wykresu, drilldown/powrót, błędy/retry, puste okresy i telefon 390 px. Obejrzano zrzuty desktop i mobile. Pierwszy przebieg wymagał dopasowania asercji do istniejącego linku z prefiksem strzałki i finalnej etykiety pustego okresu; checkboxy czekają na aktualizację stanu po nawigacji URL.

Na tymczasowym backendzie 18081, z połączeniami Hikari tylko do odczytu, niezależny weryfikator porównał **18 raportów** z surowymi zapisanymi danymi użytkownika 002: 17 aktywnych sesji/137 wpisów. Osobny opt-in Edge **1/1**, 4,9 s: 6 kombinacji miesiąc/rok/dyscyplina, wszystkie kafle zgodne z API, źródłowa sesja i powrót, telefon; bez mocków i zapisów. Hashe pełnej historii, revisions, użytkowników, katalogu i schemaVersion identyczne przed/po. Tymczasowy serwer zamknięto; procesu 8080 użytkownika nie zatrzymywano. Raport: backend/target/v2-verification/period-summary-local-verification.json.

Po restarcie backendu użytkownik wybiera Statistics → Training summary. Bez ponownego importu historii. Test realnego UI jest jawnie opt-in: CB_REAL_PERIOD_SUMMARY_API=1 npm run test:e2e; CB_API_PROXY_TARGET może wskazać osobny serwer testowy. Domyślny zestaw browser nie dotyka lokalnej historii.
