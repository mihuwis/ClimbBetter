# Wyceny i model obciążenia

Status: **v2 wdrożone w backendzie i lokalnej bazie**, 2026-11-01 (DEC-043–044). Nowe sesje i edycje używają v2. Historyczne wzory v1 zachowano poniżej jako dokumentację wcześniejszej kalibracji; kod kalkulatora EDL został usunięty.

Źródła: ustalenia użytkownika w tej rozmowie, odczyt kodu i testy. Weryfikacja: pełny zestaw 74 testów bez failures/errors/skipped, BUILD SUCCESS; lokalny Flyway zastosował migracje do V20. Szczegóły i granice dowodu: [weryfikacja backendu](../documentation/backend-verification.md). Frontend pozostaje kolejnym etapem.

## Zatwierdzona zmiana v2 — EDL zastępujemy przez profileIntensityIndex

### Waga profilu i nowe wzory

`profileIntensityIndex` jest wagą istniejącego `EffortProfile`, odrębną od `gradeIndex`. Zastępuje EDL w obliczeniach. Zestaw startowy do dalszej kalibracji:

| Profil               | Kod                    | `profileIntensityIndex` | Znaczenie                        |
| -------------------- | ---------------------- | ----------------------- | -------------------------------- |
| Bald                 | `BOULDER`              | 10                      | intensywność bazowa              |
| Krótka droga baldowa | `SHORT_BOULDERY_ROUTE` | 10                      | intensywność bazowa              |
| Średnia ciągowa      | `MEDIUM_CONTINUOUS`    | 7                       | intensywność bazowa              |
| Długa ciągowa        | `LONG_CONTINUOUS`      | 5                       | intensywność bazowa              |
| Obwód                | `CIRCUIT`              | 7                       | intensywność bazowa              |
| Średnia cruxowa      | `MEDIUM_CRUX`          | 7                       | baza ciągowa dla podziału ruchów |
| Długa cruxowa        | `LONG_CRUX`            | 5                       | baza ciągowa dla podziału ruchów |

`SHORT_CONTINUOUS_ROUTE` został wycofany z nowych wyborów zgodnie z odpowiedzią użytkownika; historyczne odwołania pozostają czytelne. Aktualne opisy siedmiu profili z tabeli użytkownika w sekcji 6 są odzwierciedlone w słowniku przez V20. Przedziały długości są orientacyjne i nie wybierają automatycznie profilu.

Dla profili bez podziału cruxowego:

```text
moveIntensity = (gradePoints / 100) × profileIntensityIndex

adjustedLoad =
    executedMoves × moveIntensity
    × styleMultiplier × relativeEffortMultiplier
```

Dzielenie przez `100` ustawia jedynie wygodną skalę liczb. Zachowujemy obecną punktację wycen jako przybliżenie, a profil określa intensywność względem innych profili. Wagi 10, 7 i 5 są kalibracją startową, nie pomiarem fizjologicznym.

Sama długość nie występuje już w mianowniku intensywności. `totalMoves` opisuje pełną długość wspinaczki, a `executedMoves` wykonaną pracę, także powtórzenia w próbach. Doprecyzowanie DEC-046 z 2026-11-04: Completed wymaga dokładnie pełnej liczby ruchów, `executedMoves == totalMoves`; ATTEMPT nadal może mieć mniej lub więcej. `classicLoad`, reguły stylu, względnej trudności, rozgrzewki i średniej ważonej pozostają zgodne z dotychczasowymi ustaleniami.

### Profile cruxowe — domyślny podział ruchów (DEC-044)

Dla `MEDIUM_CRUX` i `LONG_CRUX` stosujemy jeden schemat bez dodatkowych danych wymaganych od użytkownika i bez modelowania odpoczynków:

- trudne ruchy: 15% **wykonanych** ruchów, zaokrąglone w górę;
- łatwiejsze ruchy: pozostała liczba wykonanych ruchów;
- trudna część: jeden pełny stopień powyżej bazowej trudności ruchów odpowiadającej drogi ciągowej;
- łatwiejsza część: jeden pełny stopień poniżej tej bazy.

Pełny stopień to np. `6B → 6C`, pół stopnia `6B → 6B+`. W tym fragmencie skali Font pełny stopień to dwa kroki słownika. Przy próbie stosujemy ten sam podział, niezależnie od rzeczywistego miejsca odpadnięcia.

Dla dodatniego `executedMoves`:

```text
hardMoveCount = ceil(0.15 × executedMoves)
easyMoveCount = executedMoves - hardMoveCount

moveWork =
    hardMoveCount × hardMoveIntensity
    + easyMoveCount × easyMoveIntensity

moveIntensity = moveWork / executedMoves
adjustedLoad = moveWork × styleMultiplier × relativeEffortMultiplier
```

| Wpis                        | `totalMoves` | `executedMoves` | Trudne ruchy | Łatwiejsze ruchy |
| --------------------------- | ------------ | --------------- | ------------ | ---------------- |
| Pełne przejście             | 30           | 30              | 5            | 25               |
| Pełne przejście             | 50           | 50              | 8            | 42               |
| Próba na drodze 50-ruchowej | 50           | 40              | 6            | 34               |
| Próba po korekcie           | 50           | 41              | 7            | 34               |

Średnią zapisujemy ze skalą 10 i HALF_UP. Load obliczamy bezpośrednio z sumy pracy obu części, przed zaokrągleniem średniej, i zapisujemy do 4 miejsc. Niższy średni load drogi cruxowej od ciągowej tej samej wyceny jest zaakceptowany; nie wyrównujemy go dodatkowym mnożnikiem.

### Kalibracja trudności cruxa

Użytkownik zatwierdził proporcje punktów obecnej skali Font. Dla odpowiadającego profilu ciągowego:

```text
baseIntensity = routeGradePoints / 100 × profileIntensityIndex
hardMoveIntensity = baseIntensity × hardFontPoints / baseFontPoints
easyMoveIntensity = baseIntensity × easyFontPoints / baseFontPoints
```

Wybór orientacyjnej etykiety bazowej jest wersjonowanym przybliżeniem implementacyjnym: wybieramy najwyższą aktywną wycenę Font z punktami nie większymi od `routeGradePoints + 2.5`. Dodane 2,5 odpowiada połowie referencyjnego odstępu punktowego i uwzględnia środki zakresów z kalibracji DEC-029. Przy braku niższej pozycji używamy minimum słownika. Trudną i łatwiejszą część wyznaczamy przesunięciem o dwie pozycje aktywnego słownika, z ograniczeniem do jego granic.

Dla 7a: bazą jest Font 6B (92,5 pkt), częścią trudną 6C (97,5 pkt), łatwiejszą 6A (85 pkt). Przy bazowym indeksie 7 intensywność ciągowa wynosi 6,3; intensywności części to odpowiednio 6,6405405405 i 5,7891891892. Etykiety opisują szablon, a nie zmierzoną trudność każdego ruchu ani osobne osiągnięcia boulderowe.

Korekta względem wspinacza nadal korzysta z **całej wyceny drogi i poziomu linowego**. Baldy zachowują własny poziom odniesienia. Parametry i wyniki obu części są zapisywane w snapshotach, więc odczyt historii nie odtwarza ich z późniejszej konfiguracji.

### Przykład do porównania podczas implementacji

Pełne wykonanie podanej liczby ruchów, `styleMultiplier = 1`, `relativeEffortMultiplier = 1`; Load oznacza `adjustedLoad`:

| Wspinaczka         | Ruchy | Waga profilu | Intensywność | Load |
| ------------------ | ----- | ------------ | ------------ | ---- |
| Bald 6B            | 4     | 10           | 9,25         | 37   |
| Średnia ciągowa 7a | 20    | 7            | 6,30         | 126  |
| Długa ciągowa 7a   | 120   | 5            | 4,50         | 540  |

Bald 6B zachowuje `gradePoints = 92,5`, droga 7a — `90`. Rzeczywisty wpis może mieć inne mnożniki, np. RP przy słabej znajomości ma mnożnik 1,3.

### Zapis, edycja, historia i API

- Nowe sesje i edycje zapisują `calculationModelVersion = v2`; kalkulator EDL i jego test usunięto. Identyczne ponowienie wcześniej zapisanego żądania zwraca istniejący wynik bez przeliczenia.
- V1 pozostaje czytelne ze starych snapshotów. Po odpowiedzi użytkownika, że historia nie wymaga zachowania starego algorytmu, edycja istniejącej sesji przelicza ją według v2 i zapisuje audyt przed/po. Sama migracja nie przelicza ani nie usuwa sesji.
- Nowy snapshot zachowuje `profileIntensityIndex`, a dla cruxów także liczniki obu części, obie intensywności i etykiety bazowej/trudnej/łatwiejszej części.
- Szczegóły API udostępniają te dane jako `calculation.profileIntensityIndex` i `calculation.crux`. Pola EDL w odpowiedzi są zachowane wyłącznie do odczytu dawnej historii; w nowych wpisach są puste. Klient nie wylicza ani nie przesyła parametrów kalkulatora jako źródła prawdy.
- Szczegóły sesji, Dashboard i kalendarz zwracają wersję wyniku: `v1`, `v2` albo `mixed`. Zachowane sumy są odczytywane bez przeliczania; wartości różnych kalibracji wymagają rozróżnienia przy przyszłych agregatach.

### Stan wdrożenia i następny etap

Wdrożono V16–V20: słownik indeksów, obwód 7, snapshoty i ograniczenia v2, wycofanie krótkiej ciągowej oraz aktualne opisy profili. Lokalna baza jest na V20. Przed i po migracjach zachowano 19 sesji i 145 wpisów oraz identyczne sumy i odcisk wyników. Dane nie zostały usunięte.

Poprawiono również fallback poziomu: pusty zbiór historii nie może przez SQL `GREATEST(NULL, 0)` zwrócić najniższej wyceny. W takim przypadku poziom jest wyznaczany z bieżącej sesji zgodnie z dotychczasową regułą.

Backend i baza są gotowe do dostosowania frontendu. Nie wdrożono jeszcze prezentacji nowego indeksu, podziału cruxowego i wersji w UI. [API](../documentation/api.md), [baza](../documentation/database.md), [raport testów](../documentation/backend-verification.md).

## Specyfikacja v1 — punkt odniesienia dla dotychczasowych obliczeń

Poniższe sekcje zachowują dotychczasowy model i jego przykłady. W v2 reguły EDL z sekcji 6, 9 i 14 zastępuje powyższa decyzja; pozostałe ustalenia obowiązują w zakresie niezmienionym przez DEC-043.

## 1. Cel dokumentu

Dokument definiuje, jak ClimbBetter ocenia pojedynczą próbę lub przejście względem:

- obiektywnej wyceny wspinaczki;
- długości i profilu trudności;
- liczby rzeczywiście wykonanych ruchów;
- stylu i rezultatu próby;
- aktualnego poziomu konkretnego wspinacza.

Definiuje również, które wartości sumujemy, które uśredniamy i jakie dane zachowujemy historycznie. Nie opisuje implementacji serwisów ani endpointów.

## 2. Status i pierwszeństwo reguł

1. Tabele i wzory w tym dokumencie zastępują starsze wartości znajdujące się w `.ai/archive/source-snapshot/docs/old-doc`.
2. Arkusz jest prototypem modelu, a nie technicznym zachowaniem, które trzeba kopiować bezkrytycznie. Przykładowo brak wartości w `VLOOKUP` nie może po cichu oznaczać mnożnika `0`.
3. Zachowane decyzje mają identyfikatory `DEC-*`. Nierozstrzygnięte zachowania kontraktu mają obecnie identyfikatory `OPEN-*` w [rejestrze decyzji](../documentation/decisions.md); statusy nie oznaczają implementacji.
4. Zmiana reguły w przyszłości tworzy nową `calculationModelVersion`; nie przepisuje cicho zapisanej historii.

## 3. Pojęcia, których nie wolno mieszać

| Pojęcie          | Pytanie, na które odpowiada                                   | Przykład                             |
| ---------------- | ------------------------------------------------------------- | ------------------------------------ |
| `ClimbType`      | z czym fizycznie mamy do czynienia? ------------------------- | bald, droga z liną, obwód ---------- |
| `EffortProfile`  | jaki jest charakter i spodziewana długość trudności? -------- | bald, średnia cruxowa, długa ciągowa |
| `Grade`          | jak obiektywnie wyceniona jest wspinaczka? ------------------ | `6B+`                                |
| styl wynikowy -- | jaki był rezultat, tryb i poziom znajomości? ---------------- | OS/Flash, RP normalny, Attempt stały |
| `MoveIntensity`  | jaka intensywność ruchów wynika z wyceny, profilu i długości? | `15.0`                               |
| `RelativeEffort` | jak trudna jest wycena względem poziomu tego użytkownika? --- | `diff = +2`, mnożnik `1.80` -------- |
| `ClassicLoad`    | ile obiektywnych punktów daje wykonana część w danym stylu?   | wynik wpisu i suma sesji ----------- |
| `AdjustedLoad`   | jakie obciążenie daje próba po korekcie o poziom wspinacza?   | wynik wpisu i suma sesji ----------- |

`MoveIntensity` i `RelativeEffort` nie są tym samym wskaźnikiem. Pierwszy opisuje konstrukcję wysiłku, drugi relację wysiłku do osoby.

## 4. Dane wejściowe pojedynczego wpisu

### 4.1. Dane faktyczne od użytkownika

Dla ocenianej próby albo przejścia:

- rodzaj wspinaczki;
- profil EDL;
- wycena;
- rezultat `ASCENT` albo `ATTEMPT`;
- deklarowany tryb `OS`, `FLASH` albo `RP`;
- całkowita liczba ruchów problemu (`totalMoves`);
- liczba faktycznie wykonanych ruchów (`executedMoves`);
- opcjonalnie droga katalogowa, nazwa robocza i notatka.

Dla rozgrzewki wymagane jest tylko dodatnie `executedMoves`. Wspinaczka, profil EDL, wycena, `totalMoves`, `ascentMode`, `protectionMode`, poziom użytkownika i mnożniki nie należą do tego wariantu wpisu.

### 4.2. Dane pobierane z modelu

- `gradePoints` i `gradeIndex` z `Grade`;
- `baseEdl` z `EffortProfile`;
- `familiarityBand` wyprowadzony z historii tej wspinaczki z dwóch lat;
- `styleMultiplier` z reguły odpowiadającej rezultatowi, trybowi i znajomości;
- `currentLevelIndex` z poziomu użytkownika właściwego dla daty i dyscypliny;
- `relativeEffortMultiplier` z bandu dla różnicy indeksów.

### 4.3. Dane, których klient nie może podawać jako wyniku

Frontend nie jest źródłem prawdy dla `gradePoints`, indeksów, mnożników, `edlCount`, `moveIntensity`, `classicLoad` ani `adjustedLoad`. Może je wyświetlić po obliczeniu przez backend.

## 5. Wycena i punkty

Każda pozycja skali ma etykietę, punkty i porządkowy indeks. `ROUTE_FRENCH` i `BOULDER_FONT` są oddzielnymi skalami oraz oddzielnymi rekordami `Grade`. `gradeIndex` opisuje kolejność wyłącznie wewnątrz jednej skali i nie służy do porównania balda z drogą. `gradePoints` są wspólną, wersjonowaną kalibracją używaną przez obliczenia loadu.

Skala drogi i obwodu:

| `ROUTE_FRENCH` | `gradePoints` | `gradeIndex` |
| -------------- | ------------: | -----------: |
| 1              |          10.0 |            0 |
| 2              |          20.0 |            1 |
| 3              |          25.0 |            2 |
| 4a             |          30.0 |            3 |
| 4b             |          35.0 |            4 |
| 4c             |          40.0 |            5 |
| 5a             |          45.0 |            6 |
| 5b             |          50.0 |            7 |
| 5c             |          55.0 |            8 |
| 6a             |          60.0 |            9 |
| 6a+            |          65.0 |           10 |
| 6b             |          70.0 |           11 |
| 6b+            |          75.0 |           12 |
| 6c             |          80.0 |           13 |
| 6c+            |          85.0 |           14 |
| 7a             |          90.0 |           15 |
| 7a+            |          95.0 |           16 |
| 7b             |         100.0 |           17 |
| 7b+            |         105.0 |           18 |
| 7c             |         110.0 |           19 |
| 7c+            |         115.0 |           20 |
| 8a             |         120.0 |           21 |
| 8a+            |         125.0 |           22 |
| 8b             |         130.0 |           23 |
| 8b+            |         135.0 |           24 |
| 8c             |         140.0 |           25 |
| 8c+            |         145.0 |           26 |
| 9a             |         150.0 |           27 |
| 9a+            |         155.0 |           28 |
| 9b             |         160.0 |           29 |
| 9b+            |         165.0 |           30 |
| 9c             |         170.0 |           31 |

Skala balda i kalibracja v1 względem przybliżonej trudności drogi:

| `BOULDER_FONT` | Orientacyjny odpowiednik `ROUTE_FRENCH` | `gradePoints` | `gradeIndex` |
| -------------- | --------------------------------------- | ------------: | -----------: |
| 2              | poniżej 4a ----------------------------- |          25.0 |            0 |
| 3              | poniżej 5a ----------------------------- |          40.0 |            1 |
| 4-             | 5c -------------------------------------- |          55.0 |            2 |
| 4              | 6a/6a+ ---------------------------------- |          62.5 |            3 |
| 4+             | 6a+/6b ---------------------------------- |          67.5 |            4 |
| 5              | 6b/6b+ ---------------------------------- |          72.5 |            5 |
| 5+             | 6b+/6c ---------------------------------- |          77.5 |            6 |
| 6A             | 6c+ ------------------------------------- |          85.0 |            7 |
| 6A+            | 7a -------------------------------------- |          90.0 |            8 |
| 6B             | 7a/7a+ ---------------------------------- |          92.5 |            9 |
| 6B+            | 7a+ ------------------------------------- |          95.0 |           10 |
| 6C             | 7a+/7b ---------------------------------- |          97.5 |           11 |
| 6C+            | 7b -------------------------------------- |         100.0 |           12 |
| 7A             | 7b+ ------------------------------------- |         105.0 |           13 |
| 7A+            | 7b+/7c ---------------------------------- |         107.5 |           14 |
| 7B             | 7c/7c+ ---------------------------------- |         112.5 |           15 |
| 7B+            | 7c+/8a ---------------------------------- |         117.5 |           16 |
| 7C             | 8a/8a+ ---------------------------------- |         122.5 |           17 |
| 7C+            | 8b -------------------------------------- |         130.0 |           18 |
| 8A             | 8b+ ------------------------------------- |         135.0 |           19 |
| 8A+            | 8c -------------------------------------- |         140.0 |           20 |
| 8B             | 8c+ ------------------------------------- |         145.0 |           21 |
| 8B+            | 9a -------------------------------------- |         150.0 |           22 |
| 8C             | 9a+ ------------------------------------- |         155.0 |           23 |
| 8C+            | 9b -------------------------------------- |         160.0 |           24 |
| 9A             | 9b+ ------------------------------------- |         165.0 |           25 |
| 9A+            | 9c -------------------------------------- |         170.0 |           26 |

Dla zakresu odpowiedników `gradePoints` przyjmuje środek punktów dwóch wycen drogi. Dla opisów „poniżej” kalibracja v1 używa najwyższego stopnia bez przekroczenia wskazanej granicy. Jest to wewnętrzna, przybliżona kalibracja modelu, a nie obietnica dokładnej konwersji sportowej. Źródło porównawcze może zmieniać się między opracowaniami, dlatego snapshot zachowuje `gradePoints` i `gradeCalibrationVersion = 1`.

Zakres startowy obejmuje `ROUTE_FRENCH` od `1` do `9c` oraz `BOULDER_FONT` od `2` do `9A+`. Obwód korzysta ze skali oraz poziomu drogi. Kod nie zakłada, że obecne granice pozostaną niezmienne.

### 5.1. Skale kanoniczne i systemy prezentacji

Obliczenia korzystają z dwóch skal kanonicznych: `BOULDER_FONT` dla baldów oraz `ROUTE_FRENCH` dla dróg i obwodów. Dodatkowe systemy używane przez wspinaczy, np. V-scale dla baldów albo skala Kurtyki dla dróg, są wersjonowanymi mapowaniami wejścia i prezentacji do kanonicznego rekordu `Grade`. Nie tworzą nowego porządku punktowego ani osobnego poziomu użytkownika.

Mapowanie zachowuje system i etykietę podane przez użytkownika, a do obliczeń dostarcza kanoniczne `gradeId`, `gradeIndex` i `gradePoints`. Wiele etykiet może wskazywać ten sam kanoniczny stopień. Przybliżone albo zmienione mapowania wymagają jawnej wersji; nie przepisują istniejących snapshotów.

## 6. Profile EDL i wpływ długości

Ustalenie użytkownika z 2026-10-17 (DEC-041): długość katalogowa `lengthMeters` jest osobnym faktem. Jeśli użytkownik nie poda własnej sugestii ruchów, przyjmujemy `ceil(lengthMeters × 2)`, np. 15 m → 30 ruchów. Jawne `suggestedMoveCount` ma pierwszeństwo. To edytowalna sugestia pełnej długości, nie `executedMoves`, nie EDL i nie automatyczny wybór profilu wysiłku. [Implementacja i import skalny](../documentation/outdoor-catalog.md).



| Profil wyświetlany     | Kod roboczy            | Kiedy stosować                                                                                |
| ---------------------- | ---------------------- | --------------------------------------------------------------------------------------------- |
| Bald ----------------- | `BOULDER`              | klasyczny bald, zwykle 4–10 ruchów, moze byc krótszy lub nieznacznei dłuższy ---------        |
| Krótka droga / baldowa | `SHORT_BOULDERY_ROUTE` | 4–12 ruchów, w praktyce bald z liną --------                                                  |
| Średnia ciągowa ------ | `MEDIUM_CONTINUOUS`    | 15–30 ruchów bez wyraźnego cruxa -----------                                                  |
| Średnia cruxowa ------ | `MEDIUM_CRUX`          | Droga ok 12 do 20metrów. wyraźny crux lub dwa miejsca trudne, odpoczynki i łatwiejsze sekcje  |
| Długa ciągowa -------- | `LONG_CONTINUOUS`      | długa wspinaczka, zwykle ponad 35–40 ruchów, moze miec nawet ponad 100r                       |
| Długa cruxowa -------- | `LONG_CRUX`            | długa wspinaczka ponad 25m  z cruxem i odpoczynkami i dużymi sekcjami ławtwiejszych ruchów--- |
| Obwód ---------------- | `CIRCUIT`              | treningowy obwód na panelu -----------------                                                  |

Rozgrzewka nie jest profilem EDL. Korzysta z osobnego `WarmUpEntry`, zapisuje wyłącznie dodatnie `executedMoves` i nie uruchamia obliczenia `edlCount`.

Dla wpisu ocenianego:

```text
edlCount = baseEdl + 0.2 × totalMoves
```

Wpływ długości jest więc dwuczęściowy:

- profil określa bazową liczbę trudnych ruchów;
- `20%` całkowitej długości zwiększa dzielnik intensywności.

Im większy `edlCount` przy tej samej wycenie, tym mniejsza intensywność pojedynczego ruchu w modelu. Dla rozgrzewki `edlCount = 0`, a dalsze dzielenie nie jest wykonywane.

Kanoniczną nazwą jest `edlCount`. Starsze `LengthDivisor` nie wchodzi do nowego kontraktu.

## 7. Rezultat, tryb, znajomość i mnożnik

Wpis przechowuje osobno `resultType`, deklarowany `ascentMode` i automatyczny `familiarityBand`. Nie istnieje osobne `isCompleted`. Poniższa tabela opisuje wynikowe reguły obliczeniowe:

| Wariant wynikowy        | `styleMultiplier` | `resultType` | `ascentMode` / znajomość           |
| ----------------------- | ----------------: | ------------ | ---------------------------------- |
| OS                      |              1.50 | `ASCENT`     | `OS` + `FIRST_CONTACT` ----------- |
| Flash ----------------- |              1.50 | `ASCENT`     | `FLASH` + `FIRST_CONTACT` -------- |
| RP słaba znajomość ---- |              1.30 | `ASCENT`     | `RP` + `LOW`; także deklarowana znajomość sprzed aplikacji przy braku historii |
| RP normalny ----------- |              1.00 | `ASCENT`     | `RP` + `NORMAL` ------------------ |
| RP stały -------------- |              0.75 | `ASCENT`     | `RP` + `ESTABLISHED` ------------- |
| Attempt OS/Flash ------ |              1.50 | `ATTEMPT`    | `OS` lub `FLASH` + `FIRST_CONTACT` |
| Attempt słaba znajomość |              1.20 | `ATTEMPT`    | `RP` + `LOW`; także deklarowana znajomość sprzed aplikacji przy braku historii |
| Attempt normalny ------ |              0.90 | `ATTEMPT`    | `RP` + `NORMAL` ------------------ |
| Attempt stały --------- |              0.50 | `ATTEMPT`    | `RP` + `ESTABLISHED` ------------- |

Nieudana próba generuje obciążenie, jeżeli wykonano w niej ruchy. Nie jest jednak ukończonym przejściem ani osiągnięciem.

Rozgrzewka nie korzysta z `StyleRule` ani mnożnika stylu. Jest osobnym wariantem wpisu z zerowym `classicLoad` i `adjustedLoad`.

Znajomość jest liczona ze wszystkich wcześniejszych prób i przejść tej samej wspinaczki z ostatnich dwóch lat, bez rozgrzewek. Wcześniejszy wpis tej samej sesji już zwiększa licznik:

- `0` → `FIRST_CONTACT`;
- `1–10` → `LOW`;
- `11–20` → `NORMAL`;
- `>20` → `ESTABLISHED`.

Użytkownik nie wybiera wariantu słaba/normalna/stała podczas szybkiego logowania. Może go później skorygować w webie, co tworzy revision i ponownie liczy wpis. OS i Flash mają ten sam mnożnik, ale pozostają osobnymi osiągnięciami. Ukończone RP w drugiej albo trzeciej próbie w całej znanej historii wspinaczki daje `fastRp = true`; w odróżnieniu od znajomości ten licznik nie jest ograniczony do dwóch lat. Jest to statystyka/achievement i nie zmienia loadu.

`RP` przy zerowej historii aplikacji oznacza deklarację wcześniejszej znajomości i dla doboru reguły korzysta z `LOW`, mimo zerowego zapisanego licznika. OS/Flash sprzeczne z istniejącą historią wywołuje ostrzeżenie widoczne dla użytkownika. Zapis jest dozwolony dopiero po jawnym potwierdzeniu, zachowuje mnożnik `1.50`, rzeczywistą znajomość oraz konflikt i audyt override.

## 8. Poziom wspinacza i trudność względna

Wspinacz ma dwa niezależne poziomy odniesienia:

- poziom balda;
- poziom drogi z liną/obwodu.

Wybór opiera się na `ClimbType`, nie na nazwie profilu:

- `BOULDER` korzysta z poziomu balda;
- `ROUTE` i `CIRCUIT` korzystają z poziomu drogi.

Przed pierwszym użyciem użytkownik może podać najwyższy poziom pokonany w ostatnich dwóch latach osobno dla balda i drogi. Pole jest opcjonalne. Gdy poziomu brakuje, aplikacja wyznacza go po zakończeniu sesji. Dla 1–9 różnych przejść przyjmuje maksimum z `confidence = LOW`. Gdy nie ma żadnego przejścia, używa tymczasowo stopnia poniżej najłatwiejszej próbowanej wyceny. Próby są tylko fallbackiem i nie podnoszą dojrzałej estymacji.

Od 10 różnych przejść model szuka najwyższego stopnia popartego piramidą:

```text
support(G) = min(uniqueClimbsAtOrAbove(G), 2) × 1.00
           + min(uniqueClimbsAt(G - 1), 4)   × 0.50
           + min(uniqueClimbsAt(G - 2), 8)   × 0.25
```

Wybieramy najwyższe `G`, nie wyższe od peak grade, z `support(G) >= 3.00`. Każda katalogowa wspinaczka liczy się raz, a `G - 1` i `G - 2` są kolejnymi indeksami skali. Dzięki capom duża liczba łatwych dróg nie dominuje wyniku.

`sampleSize` jest liczbą różnych ukończonych wspinaczek bez rozgrzewek w dwuletnim oknie. Confidence v1 wynosi `UNASSESSED` dla poziomu deklarowanego bez automatycznej oceny, `LOW` dla fallbacku lub próbki poniżej 10, `MEDIUM` dla Pyramid Support `3.00–<4.50` albo brakującej warstwy oraz `HIGH` dla wyniku co najmniej `4.50` z dowodem we wszystkich trzech warstwach. Confidence nie zmienia loadu; objaśnia wiarygodność poziomu.

Do ogólnego poziomu popartego piramidą wchodzą kwalifikujące się ukończone wspinaczki, bez względu na OS, Flash, Fast RP albo RP. DEC-045 z 2026-11-01 doprecyzowuje: wędka (`TOP_ROPE`) jest wyłącznie treningiem i nigdy osiągnięciem przejścia drogi. Tryb kwalifikującego przejścia nie zmienia wartości dowodu. Powtarzana wspinaczka liczy się raz, po wyborze okresu; jej przejście sprzed pięciu lat i powtórzenie teraz daje po jednym elemencie w całej historii i ostatnich dwóch latach. Dla osobnych widoków piramidy RP kwalifikuje RP/OS/Flash, OS wyłącznie OS. [Pełne zasady i status wdrożenia](achievement-pyramid.md).

Stan przed v3 (2026-11-03): ważony Pyramid Support działa informacyjnie w Statistics, a resolver loadu brał najwyższe przejście, także na wędkę. Poniższe reguły v3 zastępują ten resolver; sam raport Pyramid Support zachowuje swoją dotychczasową metodę. [Kontrakt raportu](../documentation/achievement-pyramid-backend.md).

### Aktualność poziomu odniesienia — model v3

DEC-047, 2026-11-04: **dawne 7a pozostaje 7a**. Wygaszamy wagę dowodu poziomu odniesienia, a nie wycenę, punkty, intensywność ruchów ani zapisany historyczny load. Dla daty sesji stosujemy:

```text
ageDays = liczba dni od przejścia do daty zapisywanej sesji
weight = max(0, 1 - ageDays / 730)^2
```

Wagi wynoszą 1 dla wieku 0 dni, 0,25 dla 365 dni i 0 dla co najmniej 730 dni. Przyszłe przejścia są wykluczone. Wiek liczymy względem daty treningu, nie dnia odczytu ekranu. Każde `climbId` wnosi jeden dowód: najnowsze wcześniejsze kwalifikujące przejście, z jego niezmienioną historyczną wyceną. OS, Flash i RP kwalifikują się tak samo; TOP_ROPE oraz ATTEMPT nie potwierdzają poziomu. Wędka nadal wnosi swój load.

Przy 1–9 różnych wspinaczkach zachowujemy maksimum bez ważenia (potwierdzony wyjątek małej próbki). Przy co najmniej 10 stosujemy implementacyjny estymator `recency-weighted-peak-v1`: sortujemy dowody malejąco po indeksie wyceny i zbieramy najwyższą jedną jednostkę ich masy. Każdy dowód wnosi najwyżej własną wagę; ostatni może zostać częściowo przycięty. Jeśli suma wag nie osiąga 1, brakującą masę uzupełnia tymczasowy poziom z bieżącej sesji.

```text
remaining = 1
effectiveLevel = 0
dla kolejnych dowodów od najwyższej wyceny:
    contribution = min(weight, remaining)
    effectiveLevel += contribution × gradeIndex
    remaining -= contribution
effectiveLevel += remaining × currentSessionFallback
```

Przykład: dawne 7b (indeks 17) ma wagę 0,25, a świeże 7a (indeks 15) wystarcza do uzupełnienia 0,75. Efektywny poziom to `0,25 × 17 + 0,75 × 15 = 15,5`. Przy dalszym starzeniu starego dowodu zbliża się do 15. Wyceny obu przejść są zachowane. Ta metoda łagodzi dotychczasowe maksimum; nie jest ważonym Pyramid Support z raportu Statistics.

Fallback przy pustej lub niewystarczającej masie historii to najwyższe kwalifikujące przejście bieżącej sesji; jeśli go nie ma — indeks o jeden niższy od najłatwiejszego wejścia treningowego (ATTEMPT lub wędka), z minimum 0. To prowizoryczne założenie, nie dowód osiągnięcia. Powtórzenia jednego `climbId` nie zwiększają sample size. Poziomy Font i French pozostają niezależne. Sesja podczas własnej edycji nie stanowi dowodu dla siebie.

`effectiveRelativeGradeDifference = gradeIndex - effectiveCurrentLevelIndex`. Dla różnicy ułamkowej interpolujemy liniowo sąsiednie mnożniki z tabeli poniżej; lookup pozostaje ograniczony do −6…+5. Przykład: różnica −0,5 daje mnożnik 0,90. Mnożnik zaokrąglamy do 4 miejsc HALF_UP przed użyciem w Adjusted Load, efektywny indeks i wagi do 10 miejsc. Intensywność oraz wzór Adjusted Load z v2 pozostają takie same.

Zapis v3 zamraża użyty poziom, źródło, wersję estymatora, horyzont, sample size i masę historii. Odczyt oraz identyczny retry nie zmieniają wcześniejszych wyników; zwykły upływ czasu też ich nie przelicza. Maksimum dla próbki poniżej 10 i przejście przez próg 10 nadal mogą powodować skok — to zaakceptowany wyjątek, nie gwarancja pełnej ciągłości modelu. Literalna piramida i jej Pyramid Support nie są w tym etapie postarzane. [Implementacja i granice](../documentation/recency-load.md).

Profil stylów przechowuje co najmniej najwyższe OS, Flash, Fast RP i RP Max oraz liczbę prób najlepszego RP. Różnica indeksów `RP Max - OS` opisuje **zysk z projektowania**, a nie absolutną jakość RP. Przykładowo `7a RP` i `6c+ OS` daje tylko jeden indeks zysku: OS jest mocne względem maksimum, natomiast projektowanie może być obszarem do poprawy. Taki wniosek wymaga sample size i confidence, ponieważ pojedynczy sezon albo brak trudnych projektów może zaniżyć różnicę.

Różnica względna:

```text
relativeGradeDiff = gradeIndex - currentLevelIndex
```

- wartość ujemna: wspinaczka łatwiejsza od bieżącego poziomu;
- `0`: poziom równy;
- wartość dodatnia: wspinaczka trudniejsza.

| `relativeGradeDiff` | `relativeEffortMultiplier`                |
| ------------------: | ----------------------------------------: |
|                  -6 |                                      0.20 |
|                  -5 |                                      0.25 |
|                  -4 |                                      0.35 |
|                  -3 |                                      0.45 |
|                  -2 |                                      0.55 |
|                  -1 |                                      0.80 |
|                   0 |                                      1.00 |
|                   1 |                                      1.30 |
|                   2 |                                      1.80 |
|                   3 |                                      2.60 |
|                   4 |                                      3.50 |
|                   5 |                                      5.00 |

Dla `relativeGradeDiff <= -6` stosujemy dolny floor `0.20`. Dla `relativeGradeDiff >= 5` stosujemy górny cap `5.00`. Dokładne granice `-6` i `+5` są jeszcze w skalibrowanym zakresie; `outsideCalibratedRange = true` dopiero dla wartości `< -6` albo `> 5`. Snapshot zachowuje rzeczywistą różnicę. Brak pasującej reguły nie może po cichu dać mnożnika `0`.

## 9. Kolejność obliczeń wpisu

Kolejność jest częścią specyfikacji.

### Krok 1 — walidacja wyniku i ruchów

- `totalMoves > 0` dla wpisu ocenianego;
- `executedMoves >= 0`;
- dla `ASCENT`: `executedMoves == totalMoves` (DEC-046);
- dla `ATTEMPT`: `executedMoves` może być mniejsze lub większe od `totalMoves`, gdy wpis obejmuje odpadnięcia i ponowne wykonanie części ruchów;
- zgodność stylu z rezultatem przejścia/próby;
- aktywne i właściwe pozycje słowników.

### Krok 2 — odczyt wartości wyceny i profilu

```text
gradePoints = Grade.points
gradeIndex  = Grade.gradeIndex
baseEdl     = EffortProfile.baseEdl
```

### Krok 3 — wyznaczenie EDL

```text
edlCount = baseEdl + 0.2 × totalMoves
```

Dla rozgrzewki `edlCount` nie występuje; ten krok jest pomijany.

### Krok 4 — wyznaczenie intensywności ruchu

```text
moveIntensity = gradePoints / edlCount
```

Dla rozgrzewki `moveIntensity` nie występuje. Nie wykonujemy dzielenia przez zero.

### Krok 5 — wybór poziomu odniesienia

Pobieramy `currentLevelIndex` właściwy dla użytkownika, daty sesji i rodzaju wspinaczki.

```text
relativeGradeDiff = gradeIndex - currentLevelIndex
```

### Krok 6 — wybór mnożników

```text
styleMultiplier          = StyleRule(resultType, ascentMode, familiarityBand)
relativeEffortMultiplier = band(relativeGradeDiff).multiplier
```

### Krok 7 — klasyczne obciążenie wpisu

```text
classicLoad =
    gradePoints
    × styleMultiplier
    × (executedMoves / totalMoves)
```

Dla rozgrzewki `classicLoad = 0`.

Stosunek `executedMoves / totalMoves` dla prób nie jest ograniczony do 1.0. `totalMoves` opisuje długość wspinaczki, a `executedMoves` faktycznie wykonaną pracę, łącznie z powtórzonymi ruchami po odpadnięciu; nadmiarowe ruchy zwiększają load. DEC-046 wymaga dla `ASCENT` dokładnie 1.0. Taka równość jest warunkiem koniecznym ukończenia, ale nie dowodzi czystego przejścia: `ATTEMPT` może również wykonać pełną długość.

### Krok 8 — skorygowane obciążenie wpisu

```text
adjustedLoad =
    executedMoves
    × moveIntensity
    × styleMultiplier
    × relativeEffortMultiplier
```

Dla rozgrzewki `adjustedLoad = 0`.

### Krok 9 — snapshot

Wpis zachowuje wszystkie wartości wejściowe pobrane ze słowników oraz wyniki pośrednie i końcowe wraz z `calculationModelVersion`.

## 10. Co sumujemy, a co uśredniamy

### 10.1. Pojedyncza sesja

| Wskaźnik               | Agregacja                                                        | Rozgrzewka |
| ---------------------- | ---------------------------------------------------------------- | ---------- |
| `totalMoves`           | suma `executedMoves` wszystkich wpisów ------------------------- | wliczana   |
| `classicLoad`          | suma `classicLoad` wpisów -------------------------------------- | wnosi `0`  |
| `adjustedLoad`         | suma `adjustedLoad` wpisów ------------------------------------- | wnosi `0`  |
| `averageMoveIntensity` | średnia `moveIntensity` ważona `executedMoves` ocenianych wpisów | pomijana   |
| `entryCount`           | liczba wpisów -------------------------------------------------- | wliczana   |
| `completedClimbsCount` | liczba wpisów typu `ASCENT` ------------------------------------ | pomijana   |
| `duration`             | różnica start–koniec albo jawne minuty ------------------------- | niezależne |

Kanoniczna średnia sesji jest ważona liczbą wykonanych ruchów:

```text
averageMoveIntensity =
    Σ(moveIntensity × executedMoves)
    / Σ(executedMoves ocenianych wpisów)
```

Jeżeli żaden oceniany wpis nie ma wykonanego ruchu, wartość jest `null`, a nie `0`.

### 10.2. Dzień, tydzień, miesiąc i cykl

Bezpieczne agregaty addytywne:

- liczba sesji;
- liczba wpisów;
- `totalMoves`;
- `classicLoad`;
- `adjustedLoad`;
- liczba ukończonych przejść;
- czas trwania.

Wartości, których nie wolno średnio wyliczać przez „średnią ze średnich” bez wag:

- `averageMoveIntensity`;
- średnia wycena;
- skuteczność przejść;
- load na minutę.

Dla zakresu czasu trzeba je policzyć ponownie z właściwego mianownika. Przykładowo średnia ważona ruchami:

```text
weightedMoveIntensity =
    Σ(moveIntensity × executedMoves)
    / Σ(executedMoves ocenianych wpisów)
```

Miesięczne „Suma punktów” nie jest osobną trzecią metryką. `adjustedLoad` jest główną metryką użytkową, a mniej ważny `classicLoad` pozostaje wartością porównawczą i diagnostyczną. Obie wartości zapisujemy i zwracamy.

## 11. Przykłady kontrolne

### 11.1. Obwód 6a+, pełne RP stałe

Dane:

- profil `Obwód`, `baseEdl = 20`;
- `totalMoves = 40`, `executedMoves = 40`;
- wycena drogi/obwodu `6a+`, `gradePoints = 65`, `gradeIndex = 10`;
- `currentLevelIndex = 12`, więc różnica `-2` i multiplier `0.55`;
- styl `RP stały`, multiplier `0.75`.

Wynik:

```text
edlCount     = 20 + 0.2 × 40 = 28
moveIntensity = 65 / 28 = 2.321428571...
classicLoad   = 65 × 0.75 × 40/40 = 48.75
adjustedLoad  = 40 × 2.321428571... × 0.75 × 0.55
              = 38.30357143...
```

### 11.2. Nieudana próba balda 6B+

Dane:

- profil `Bald`, `baseEdl = 4`;
- `totalMoves = 5`, `executedMoves = 1`;
- `gradePoints = 75`, `gradeIndex = 12`;
- `currentLevelIndex = 12`, relative multiplier `1.00`;
- styl `Attempt normalny`, multiplier `0.90`.

Wynik:

```text
edlCount      = 4 + 0.2 × 5 = 5
moveIntensity = 75 / 5 = 15
classicLoad   = 75 × 0.90 × 1/5 = 13.5
adjustedLoad  = 1 × 15 × 0.90 × 1 = 13.5
```

Nieudana próba ma load, ale nie zwiększa `completedClimbsCount`.

### 11.3. Rozgrzewka

Dla `executedMoves = 30`:

```text
executedMoves contribution = 30
classicLoad             = 0
adjustedLoad            = 0
averageMoveIntensity    = wpis pominięty
```

### 11.4. Pyramid Support i profil stylów — sezon linowy

Zakładamy 11 różnych ukończonych dróg:

- `1 × 7a RP` w dziesiątej próbie;
- `0 × 6c+`;
- `3 × 6c`, w tym jedna droga RP w drugiej próbie;
- `1 × 6b+ OS` i `1 × 6b+ RP`;
- `5 × 6b`.

Dla kandydata `G = 7a`:

```text
support(7a) = 1 × 1.00 + 0 × 0.50 + 3 × 0.25 = 1.75
```

`7a` nie osiąga progu `3.00`. Dla `G = 6c+`:

```text
support(6c+) = 1 × 1.00 + 3 × 0.50 + 2 × 0.25 = 3.00
```

Ogólny poziom odniesienia wynosi zatem `6c+`, mimo braku dokładnie takiego przejścia: jest to estymacja pomiędzy potwierdzonym maksimum `7a` a objętością na `6c` i `6b+`, a nie twierdzenie, że użytkownik przeszedł drogę `6c+`. Pięć dróg `6b` nie wspiera kandydata `6c+`, bo leży trzy indeksy niżej, ale liczy się do sample size i przy ocenie niższych kandydatów.

Profil stylów zachowuje równocześnie:

```text
generalSupportedGrade = 6c+
peakGrade             = 7a
peakRpGrade           = 7a   (10. próba, nie Fast RP)
peakFastRpGrade       = 6c   (2. próba)
peakOsGrade           = 6b+
maxRpVsOsGap          = 3 indeksy
fastRpVsOsGap         = 1 indeks
maxRpVsFastRpGap      = 2 indeksy
```

Wszystkie przejścia budują ogólny poziom, ale profil pokazuje, w jaki sposób ten poziom został osiągnięty.

## 12. Precyzja i zaokrąglenia

- Obliczenia używają dziesiętnego typu o kontrolowanej precyzji (`BigDecimal` w Javie, `numeric` w PostgreSQL).
- Nie używamy `double` jako źródła utrwalanych wyników.
- Kroki pośrednie mają skalę 10 i używają `RoundingMode.HALF_UP` tam, gdzie dzielenie wymaga zaokrąglenia.
- Snapshot zapisuje wartości do 4 miejsc po przecinku.
- Suma powstaje z zapisanych wartości dziesiętnych, nigdy z tekstu sformatowanego przez UI.
- Frontend zwykle pokazuje 2 miejsca, bez zmiany wartości źródłowej.

## 13. Integralność i przypadki błędne

Obliczenie zostaje odrzucone, gdy:

- brakuje wymaganej wyceny, profilu albo stylu;
- wpis oceniany ma `totalMoves <= 0`;
- ruchy wykonane są ujemne lub większe od całkowitych;
- styl nie zgadza się z rezultatem;
- nie istnieje właściwy poziom użytkownika i nie zatwierdzono fallbacku;
- żaden aktywny relative effort band nie pasuje albo pasuje więcej niż jeden;
- słownik jest nieaktywny dla nowego wpisu;
- wycena i poziom należą do nieporównywalnych skal.

Nie wolno po cichu zastępować braków zerem, domyślnym poziomem ani pierwszym rekordem słownika.

## 14. Historia modelu

Każdy wpis zapisuje co najmniej:

- punkty i indeks wyceny;
- poziom odniesienia;
- `baseEdl` i `edlCount`;
- mnożnik oraz rezultat stylu;
- różnicę i mnożnik względny;
- `moveIntensity`, `classicLoad`, `adjustedLoad`;
- wersję modelu.

Zmiana aktywnej konfiguracji wpływa na nowe obliczenia. Historyczny wpis zmienia się wyłącznie w wyniku jawnej korekty/reewaluacji z audytem.

## 15. Propozycje dalszych statystyk

Poniższe pomysły nie są zatwierdzonym elementem modelu v1. Warto je oceniać dopiero na prawdziwych danych i zawsze pokazywać użytkownikowi, z jakich faktów wynikają.

### 15.1. Rozkład intensywności

- liczba ruchów i load w strefach `relativeGradeDiff`;
- udział ruchów łatwych, na poziomie i ponad poziomem;
- rozkład wycen osobno dla balda, dróg i obwodów;
- porównanie profili ciągowych i cruxowych.

Pozwala ocenić nie tylko „ile”, lecz także jaki charakter miał tydzień lub cykl.

### 15.2. Porównanie średniej ważonej i zwykłej

Średnia ważona `executedMoves` jest metryką kanoniczną. Na danych walidacyjnych można równolegle policzyć zwykłą średnią wpisów, aby sprawdzić, jak mocno wybór wag zmienia interpretację sesji. Zwykła średnia nie jest jednak zapisywana jako drugi obowiązkowy wynik v1.

### 15.3. Gęstość obciążenia

```text
loadDensity = adjustedLoad / durationMinutes
```

Może rozróżnić długi spokojny trening od krótkiej, bardzo intensywnej sesji. Wymaga wiarygodnego czasu trwania i nie powinien być liczony dla sesji z domyślnym czasem bez oznaczenia jakości danych.

### 15.4. Skuteczność prób i praca nad projektem

- stosunek przejść do prób;
- liczba prób do pierwszego przejścia;
- profil poziomu: najwyższe OS, Fast RP i maksymalne RP osobno dla balda i drogi;
- liczba oraz historia Flash jako osiągnięcia, mimo wspólnego mnożnika loadu z OS;
- load wykonany przed sukcesem;
- czas od pierwszej próby do przejścia;
- trend liczby wykonanych ruchów na kolejnych próbach tej samej wspinaczki.

Te metryki wymagają stabilnego `climbId`, poprawnego `attemptBlockId` i spójnej historii.

### 15.5. Progresja poziomu

- najwyższa potwierdzona wycena i poziom poparty piramidą jako dwie osobne wartości;
- kompletność piramidy względem zatwierdzonych warstw i limitów;
- progres osobno dla balda i liny;
- poziom z oznaczeniem źródła i pewności.

Poziom poparty piramidą może zasilać `UserGradeReference`. Peak grade i kompletność piramidy pozostają statystykami objaśniającymi, a automatyczna zmiana poziomu musi być transparentna i odwracalna.

### 15.6. Obciążenie krótkie i długie

Można testować wykładniczo ważone obciążenie krótkoterminowe i długoterminowe, monotonię tygodnia oraz strain. Są to wskaźniki pomocnicze, nie diagnoza regeneracji ani ryzyka kontuzji. Wymagają wystarczająco długiej i kompletnej historii.

### 15.7. Regularność i ciągłość

- dni/sesje wspinaczkowe w tygodniu i miesiącu;
- odstępy pomiędzy sesjami;
- rolling 7/28 days dla ruchów i loadu;
- kompletność logowania sesji.

Streak powinien być motywacyjny, ale nie powinien zachęcać do treningu bez regeneracji.

### 15.8. Kalibracja EDL

Po zebraniu danych można porównać profile EDL z rzeczywistymi rozkładami długości, skuteczności i loadu. Jeśli profil regularnie daje nielogiczne wyniki, korekta powinna powstać jako nowa wersja modelu i zostać sprawdzona na historycznym zbiorze testowym.

### 15.9. Jakość i pewność danych

Każda zaawansowana statystyka może otrzymać wskaźnik kompletności:

- czy czas był zmierzony czy domyślny;
- czy wspinaczka ma stabilne `climbId`;
- czy długość była podana czy zasugerowana;
- czy poziom użytkownika był aktualny;
- czy wpis pochodzi z importu.

To pozwoli odróżnić precyzyjną analizę od orientacyjnego trendu.

### 15.10. Porównanie modeli

Warto zachować możliwość policzenia, jak zmiana EDL, mnożników lub poziomu wpłynęłaby na dane, bez automatycznego nadpisywania historii. Raport porównawczy `v1 vs v2` jest bezpieczniejszy niż masowa migracja wyników wykonywana w ciemno.

## 16. Kryteria zatwierdzenia wersji 1

Model wyceny jest gotowy do implementacji, gdy:

- decyzje `DEC-006` i `DEC-008` są częścią tej specyfikacji;
- co najmniej 5–10 realnych przypadków jest ręcznie policzonych i zatwierdzonych;
- przypadki obejmują przejście, próbę częściową, rozgrzewkę, bald, drogę/obwód i oba krańce relative effort;
- ustalone są precyzja, rounding i nazwy pól;
- każda agregacja zakresu czasu ma jednoznaczny licznik i mianownik;
- wiadomo, jak model zachowuje się bez poziomu użytkownika i poza zakresem relative effort.
