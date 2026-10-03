# Wyceny i model obciążenia

Status: przeniesiona specyfikacja v1 z 2026-08-15; konsolidacja 2026-10-03, iteracja 1 z 3.

Poniżej zachowano pełne tabele, wzory, przykłady i propozycje statystyk z `CB_climbing_effort_valuation.md`. Źródłowy arkusz nie był ponownie odczytywany w tej iteracji. Przykłady w dokumentacji nie zastępują zatwierdzenia 5–10 realnych sesji ani testów kalkulatora.

Wykryte luki kontraktu są w [rejestrze decyzji](../documentation/decisions.md): `OPEN-07` (brak poziomu i preview), `OPEN-09` (rozgrzewka), `OPEN-10` (niepełne kombinacje reguł stylu), `OPEN-11` (granice estymacji i bandów). Uzgodnionych wzorów nie zmieniono. Historyczne określenie „iteracja 4 z 4” dotyczy dawnej specyfikacji, nie obecnego porządkowania `.ai`.

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

- rodzaj wspinaczki;
- profil EDL;
- wycena;
- rezultat `ASCENT`, `ATTEMPT` albo `WARMUP`;
- deklarowany tryb `OS`, `FLASH` albo `RP`;
- całkowita liczba ruchów problemu (`totalMoves`);
- liczba faktycznie wykonanych ruchów (`executedMoves`);
- jednoznaczny rezultat (`ASCENT`, `ATTEMPT` albo `WARMUP`);
- opcjonalnie droga katalogowa, nazwa robocza i notatka.

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

Każda pozycja skali ma etykietę, punkty i porządkowy indeks. `BOULDER_FONT` i `ROUTE_FRENCH` są oddzielnymi skalami oraz oddzielnymi rekordami `Grade`, ale odpowiadające sobie stopnie mają te same punkty i indeksy. Mała/wielka litera komunikuje użytkownikowi rodzaj skali, nie zmienia wartości punktowej.

| Droga/obwód (`ROUTE_FRENCH`) | Bald (`BOULDER_FONT`) | `gradePoints` | `gradeIndex` |
| ---------------------------- | --------------------- | ------------: | -----------: |
| 1                            | 1                     |            10 |            0 |
| 2                            | 2                     |            20 |            1 |
| 2+                           | 2+                    |            25 |            2 |
| 3                            | 3                     |            30 |            3 |
| 3+                           | 3+                    |            35 |            4 |
| 4                            | 4                     |            40 |            5 |
| 4+                           | 4+                    |            45 |            6 |
| 5                            | 5                     |            50 |            7 |
| 5+                           | 5+                    |            55 |            8 |
| 6a                           | 6A                    |            60 |            9 |
| 6a+                          | 6A+                   |            65 |           10 |
| 6b                           | 6B                    |            70 |           11 |
| 6b+                          | 6B+                   |            75 |           12 |
| 6c                           | 6C                    |            80 |           13 |
| 6c+                          | 6C+                   |            85 |           14 |
| 7a                           | 7A                    |            90 |           15 |
| 7a+                          | 7A+                   |            95 |           16 |
| 7b                           | 7B                    |           100 |           17 |
| 7b+                          | 7B+                   |           105 |           18 |
| 7c                           | 7C                    |           110 |           19 |
| 7c+                          | 7C+                   |           115 |           20 |
| 8a                           | 8A                    |           120 |           21 |
| 8a+                          | 8A+                   |           125 |           22 |
| 8b                           | 8B                    |           130 |           23 |
| 8b+                          | 8B+                   |           135 |           24 |
| 8c                           | 8C                    |           140 |           25 |
| 8c+                          | 8C+                   |           145 |           26 |
| 9a                           | 9A                    |           150 |           27 |
| 9a+                          | 9A+                   |           155 |           28 |
| 9b                           | 9B                    |           160 |           29 |
| 9b+                          | 9B+                   |           165 |           30 |
| 9c                           | 9C                    |           170 |           31 |

Zakres startowy aplikacji obejmuje `1–9c/9C`. Obwód korzysta ze skali oraz poziomu drogi. Kod nie zakłada, że `9c/9C` pozostanie granicą na zawsze.

## 6. Profile EDL i wpływ długości

`EDL` oznacza expected difficulty length — spodziewaną długość trudnej części. Profil nie zastępuje całkowitej liczby ruchów; dostarcza jedynie wartość bazową.

| Profil wyświetlany     | Kod roboczy              | Kiedy stosować                               | `baseEdl` |
| ---------------------- | ------------------------ | -------------------------------------------- | --------: |
| Bald ----------------- | `BOULDER`                | klasyczny bald, zwykle 4–10 ruchów --------- |         4 |
| Krótka droga / baldowa | `SHORT_BOULDERY_ROUTE`   | 4–12 ruchów, w praktyce bald z liną -------- |         8 |
| Krótka droga ciągowa   | `SHORT_CONTINUOUS_ROUTE` | 8–15 ruchów ciągowych ---------------------- |        10 |
| Średnia ciągowa ------ | `MEDIUM_CONTINUOUS`      | 15–40 ruchów bez wyraźnego cruxa ----------- |        28 |
| Średnia cruxowa ------ | `MEDIUM_CRUX`            | wyraźny crux, odpoczynki i łatwiejsze sekcje |        15 |
| Długa ciągowa -------- | `LONG_CONTINUOUS`        | długa wspinaczka, zwykle ponad 35–40 ruchów  |        40 |
| Długa cruxowa -------- | `LONG_CRUX`              | długa wspinaczka z cruxem i odpoczynkami --- |        22 |
| Obwód ---------------- | `CIRCUIT`                | treningowy obwód na panelu ----------------- |        20 |
| Rozgrzewka ----------- | `WARMUP`                 | ruchy rozgrzewkowe bez punktów ------------- |         0 |

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
| RP słaba znajomość ---- |              1.30 | `ASCENT`     | `RP` + `LOW` --------------------- |
| RP normalny ----------- |              1.00 | `ASCENT`     | `RP` + `NORMAL` ------------------ |
| RP stały -------------- |              0.75 | `ASCENT`     | `RP` + `ESTABLISHED` ------------- |
| Rozgrzewka ------------ |              0.00 | `WARMUP`     | bez trybu i znajomości ----------- |
| Attempt OS/Flash ------ |              1.50 | `ATTEMPT`    | `OS` lub `FLASH` + `FIRST_CONTACT` |
| Attempt słaba znajomość |              1.20 | `ATTEMPT`    | `RP` + `LOW` --------------------- |
| Attempt normalny ------ |              0.90 | `ATTEMPT`    | `RP` + `NORMAL` ------------------ |
| Attempt stały --------- |              0.50 | `ATTEMPT`    | `RP` + `ESTABLISHED` ------------- |

Nieudana próba generuje obciążenie, jeżeli wykonano w niej ruchy. Nie jest jednak ukończonym przejściem ani osiągnięciem.

Znajomość jest liczona ze wszystkich wcześniejszych prób i przejść tej samej wspinaczki z ostatnich dwóch lat, bez rozgrzewek. Wcześniejszy wpis tej samej sesji już zwiększa licznik:

- `0` → `FIRST_CONTACT`;
- `1–10` → `LOW`;
- `11–20` → `NORMAL`;
- `>20` → `ESTABLISHED`.

Użytkownik nie wybiera wariantu słaba/normalna/stała podczas szybkiego logowania. Może go później skorygować w webie, co tworzy revision i ponownie liczy wpis. OS i Flash mają ten sam mnożnik, ale pozostają osobnymi osiągnięciami. Ukończone RP w drugiej albo trzeciej próbie w całej znanej historii wspinaczki daje `fastRp = true`; w odróżnieniu od znajomości ten licznik nie jest ograniczony do dwóch lat. Jest to statystyka/achievement i nie zmienia loadu.

OS/Flash sprzeczne z istniejącą historią wywołuje ostrzeżenie. Zapis jest dozwolony dopiero po jawnym potwierdzeniu, a backend utrwala konflikt i audyt override. Dzięki temu nie tracimy deklaracji przy niepełnej albo błędnie połączonej historii.

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

Do ogólnego poziomu wchodzą wszystkie ukończone wspinaczki poza rozgrzewkami, bez względu na OS, Flash, Fast RP albo RP. Tryb przejścia nie zmienia wartości dowodu w piramidzie; jest analizowany osobno w profilu stylów. Powtarzane przejście tej samej katalogowej wspinaczki nie jest kolejnym elementem piramidy, chociaż nadal wpływa na load i historię znajomości.

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
|                   5 | 5.00, obecnie oznaczone jako „poza skalą” |

Dla `relativeGradeDiff <= -6` stosujemy dolny floor `0.20`. Dla `relativeGradeDiff >= 5` stosujemy górny cap `5.00`. Snapshot zachowuje rzeczywistą różnicę oraz `outsideCalibratedRange = true`, gdy wartość leży poza skalibrowanym zakresem. Brak pasującej reguły nie może po cichu dać mnożnika `0`.

## 9. Kolejność obliczeń wpisu

Kolejność jest częścią specyfikacji.

### Krok 1 — walidacja wyniku i ruchów

- `totalMoves > 0` dla wpisu ocenianego;
- `executedMoves >= 0`;
- `executedMoves <= totalMoves`;
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

Dla rozgrzewki `edlCount = 0`.

### Krok 4 — wyznaczenie intensywności ruchu

```text
moveIntensity = gradePoints / edlCount
```

Dla rozgrzewki `moveIntensity` jest nieobecne logicznie; w kontrakcie może być `null` albo `0` po decyzji o reprezentacji. Nie wykonujemy dzielenia przez zero.

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

Ułamek ukończenia służy wyłącznie proporcjonalnemu naliczeniu punktów. Nie zastępuje zapisywania liczby wykonanych ruchów.

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
totalMoves contribution = 30
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
