# ClimbBetter — model oceny wspinaczki

## 1. Cel dokumentu

Dokument opisuje model domenowy i model obliczeń stosowany w ClimbBetter. Jego podstawą jest arkusz `CB_2026_Wsinaczka_MWis`, przede wszystkim zakładka `2026_08` oraz słowniki `Skale`, `RelEffort`, `Profil`, `Styl` i `LEGENDA`, odczytane 15 sierpnia 2026 roku.

Dokument ma być źródłem wymagań dla dalszego projektowania aplikacji, bazy danych, API i usług. Nie zawiera projektu technicznego, kodu ani konkretnego schematu bazy danych.

### Pierwszeństwo źródeł

1. Wartości słownikowe i wzory zapisane w aktualnym arkuszu są bieżącą wersją modelu.
2. Zachowane są wcześniejsze decyzje domenowe ClimbBetter, które nie są sprzeczne z arkuszem.
3. W miejscach nieokreślonych nie należy dopowiadać reguł. Są one wymienione w sekcji „Otwarte decyzje”.
4. Jeżeli starsze dokumenty projektu zawierają inne wartości EDL, punktów, mnożników albo nazw profili, niniejszy dokument i aktualny arkusz mają pierwszeństwo.

## 2. Główne założenia modelu

ClimbBetter nie ma być wyłącznie dziennikiem przejść. Model ma opisywać faktycznie wykonany wysiłek wspinaczkowy i umożliwiać porównywanie sesji o różnym charakterze.

Najważniejsze zasady:

- podstawową jednostką danych jest uporządkowany wpis w sesji, czyli przejście albo próba przejścia;
- liczba faktycznie wykonanych ruchów jest zapisywana bezpośrednio — nie jest zastępowana procentem ukończenia drogi lub balda;
- nieudana próba również generuje obciążenie, jeżeli wykonano w niej ruchy;
- udane przejście może być osiągnięciem, a nieudana próba nie jest osiągnięciem, ale pozostaje pełnoprawnym elementem treningu;
- wycena wspinaczki jest zamieniana na punkty oraz porządkowy indeks trudności;
- charakter wspinaczki jest opisany profilem EDL, który wpływa na przewidywaną długość trudności;
- styl określa zarówno rezultat próby, jak i poziom znajomości problemu, a następnie dostarcza mnożnik stylu;
- `MoveInt` opisuje intensywność ruchów wynikającą z wyceny i profilu wspinaczki;
- `RelativeEffort` opisuje trudność względem aktualnego poziomu konkretnego wspinacza;
- `MoveInt` i `RelativeEffort` są osobnymi pojęciami i nie wolno ich scalać;
- model wylicza dwa równoległe wyniki: klasyczne punkty obciążenia oraz obciążenie skorygowane względem poziomu wspinacza;
- rozgrzewka liczy się do całkowitej liczby ruchów, ale nie generuje punktów;
- sesja ma datę, godzinę rozpoczęcia i zakończenia, lokalizację, kontekst cyklu treningowego, notatkę oraz uporządkowaną listę wpisów;
- zmiany słowników w przyszłości nie powinny zmieniać historycznych wyników już zapisanych sesji.

## 3. Rozdzielenie kluczowych pojęć

W arkuszu kolumna A jest podpisana `Climb typ`, ale jej wartości pełnią przede wszystkim rolę **profilu EDL**. W aplikacji nie należy traktować następujących pojęć jako jednego pola:

### 3.1. Rodzaj wspinaczki

Rodzaj aktywności opisuje, z czym fizycznie mamy do czynienia, np.:

- bald;
- droga z liną;
- obwód.

Wcześniejsze ustalenia projektu przewidywały typy `Boulder`, `Route` i `Circuit`. Aktualny arkusz nie zawiera osobnego pola dla tej klasyfikacji — część informacji jest zaszyta w profilu EDL i lokalizacji.

### 3.2. Profil EDL

Profil opisuje charakter i przewidywaną długość trudnej części wspinaczki. Profil wyznacza `EDL Base`. Przykłady: `Bald`, `Srednia cruxowa`, `Długa Ciągowa`, `Obwód`.

Profil jest właściwością konkretnego wpisu. Nie powinien być zawsze automatycznie wyprowadzany wyłącznie z rodzaju wspinaczki, ponieważ dwie drogi o podobnej długości mogą mieć inny charakter.

### 3.3. Wynik i styl próby

Styl opisuje rezultat oraz znajomość problemu. Przykładowo:

- `OS/Flash` oznacza udane przejście bez znajomości albo po znajomości wyłącznie z opisu;
- `RP normalny` oznacza udane przejście znanego problemu;
- `Attempt normalny` oznacza nieudaną próbę problemu na normalnym poziomie znajomości;
- `Rozgrzewka` oznacza ruchy, które nie generują punktów.

W modelu trwałym wartość stylu i informacja o ukończeniu powinny pozostać jednoznaczne. Nie należy polegać wyłącznie na analizowaniu tekstowej nazwy stylu przy ustalaniu, czy wpis był ukończonym przejściem.

## 4. Wspinacz i aktualny poziom

Obliczenia są wykonywane dla konkretnego wspinacza/użytkownika.

Wspinacz ma co najmniej dwa niezależne poziomy odniesienia:

- aktualny poziom balda;
- aktualny poziom drogi/obwodu.

W arkuszu `2026_08` oba poziomy wynoszą `6B+`, co odpowiada `GradeIndex = 12`.

Aktualny poziom jest potrzebny wyłącznie do obliczenia względnej trudności wpisu. Nie zmienia obiektywnej liczby punktów przypisanej wycenie.

Dla wpisu o profilu `Bald` stosowany jest poziom balda. Dla pozostałych profili aktualny arkusz stosuje poziom drogi/obwodu.

Poziom wspinacza zmienia się w czasie. Historyczny wpis musi zachować poziom lub co najmniej `CurrentLevelIndex`, który został użyty podczas obliczania wyniku.

## 5. Sesja treningowa

Sesja treningowa reprezentuje jeden trening albo jeden wyjazd wspinaczkowy.

### 5.1. Dane sesji

Sesja zawiera:

- wspinacza;
- datę;
- godzinę rozpoczęcia;
- godzinę zakończenia;
- lokalizację;
- makrocykl;
- mezocykl;
- mikrocykl;
- jednostkę treningową;
- notatkę do całej sesji;
- uporządkowaną listę wpisów;
- użyty poziom balda i poziom drogi/obwodu albo ich historyczne indeksy;
- podsumowania wyliczone z wpisów.

Godziny rozpoczęcia i zakończenia służą do ustalenia czasu trwania sesji. W bieżącym modelu czas sesji nie bierze udziału w obliczaniu `MoveInt`, `ClassicLoad` ani `AdjustedLoad`.

### 5.2. Przykładowe sesje z arkusza

| Data | Start | Koniec | Czas | Lokalizacja | Jednostka |
| --- | ---: | ---: | ---: | --- | --- |
| 01.08.2026 | 15:30 | 16:08 | 38 min | Garaż, Bolechowice | Obwody |
| 05.08.2026 | 21:30 | 22:28 | 58 min | Zimny Dół, Jura Południowa | Baldy — dowolne |

### 5.3. Periodyzacja

Arkusz opisuje cztery poziomy kontekstu treningowego:

1. **Makrocykl** — szeroki okres, np. `2026 sezon`.
2. **Mezocykl** — część makrocyklu, np. `M2-Baza`.
3. **Mikrocykl** — krótszy cel lub akcent, np. `Performance` albo `Wytrzymałość siłowa, Obwody długie`.
4. **Jednostka treningowa** — bezpośredni rodzaj sesji, np. `Obwody` albo `Baldy — dowolne`.

Model nie rozstrzyga jeszcze, czy są to współdzielone słowniki, obiekty planu treningowego, czy tylko etykiety przypisane do sesji.

## 6. Lokalizacja wspinaczkowa

Każda sesja odbywa się w jednej lokalizacji. Arkusz pokazuje dwa warianty opisu lokalizacji.

### 6.1. Lokalizacja sztuczna lub prywatna

Przykład:

- `Location type`: `Bouldering Gym`;
- `Access`: `Private`;
- `Country`: `Poland`;
- `City`: `Bolechowice`;
- `Name`: `Garaż`.

### 6.2. Lokalizacja naturalna

Przykład:

- `Location type`: `Bouldering spot`;
- `Country`: `Poland`;
- `Area`: `Jura południowa`;
- `Site`: `Zimny dół`;
- `Access`: brak wartości w przykładzie.

### 6.3. Bieżący model lokalizacji

Z arkusza wynikają następujące właściwości lokalizacji:

| Właściwość | Znaczenie | Wymagalność wynikająca z arkusza |
| --- | --- | --- |
| Location type | Typ miejsca wspinaczkowego | wymagane |
| Access | Sposób dostępu, np. `Private` | opcjonalne |
| Country | Państwo | wymagane w przykładach |
| City | Miejscowość dla obiektu sztucznego/prywatnego | zależne od typu |
| Area | Większy rejon wspinaczkowy | zależne od typu |
| Site lub Name | Konkretne miejsce/obiekt | wymagane semantycznie, lecz pod dwiema nazwami |

Aktualny arkusz nie ustala pełnej listy typów lokalizacji ani ostatecznej hierarchii. Nie rozstrzyga również, czy `Name` i `Site` są jednym pojęciem, czy różnymi poziomami. Wcześniej w projekcie rozważano podejście „My Areas first” oraz opcjonalne sektory, ale sektor nie występuje w bieżącym modelu obliczeniowym.

## 7. Wpis treningowy

Wpis treningowy reprezentuje jedno przejście albo jedną próbę. Wpisy są uporządkowane w kolejności wykonania.

Wpis może wskazywać istniejącą wspinaczkę z katalogu, ale musi również pozwalać na wpisanie tymczasowej lub umownej nazwy bez wcześniejszego tworzenia pełnego rekordu drogi lub balda.

### 7.1. Dane wejściowe wpisu

| Pole | Znaczenie | Źródło |
| --- | --- | --- |
| Profil EDL | Charakter wspinaczki, np. `Bald`, `Obwód` | wybór ze słownika `Profil` |
| Climb name | Nazwa drogi, balda lub obwodu | katalog albo tekst użytkownika |
| Wycena | Trudność wspinaczki | słownik `Skale` |
| Styl | Wynik i znajomość problemu | słownik `Styl` |
| Długość całkowita | Pełna długość problemu wyrażona w ruchach | wartość użytkownika |
| Ruchy wykonane | Liczba faktycznie wykonanych ruchów w tej próbie | wartość użytkownika |
| Uwagi | Notatka do pojedynczego wpisu | wartość opcjonalna |

Wcześniejsze ustalenia przewidywały również numer próby lub blok prób. Pola te nie występują jeszcze w arkuszu.

### 7.2. Dane wyliczane albo pobierane ze słowników

| Pole | Znaczenie |
| --- | --- |
| EDL Base | Wartość startowa wynikająca z profilu |
| EDL count | Dostosowany dzielnik długości trudności |
| GradePoints | Punkty przypisane wycenie |
| GradeIndex | Porządkowy indeks wyceny |
| CurrentLevelIndex | Indeks aktualnego poziomu wspinacza dla danej kategorii |
| RelativeGradeDiff | Różnica między trudnością wpisu i poziomem wspinacza |
| MoveInt | Obiektywna intensywność ruchów wynikająca z wyceny oraz EDL |
| StyleMultiplier | Mnożnik stylu |
| RelativeEffortMultiplier | Mnożnik względnej trudności |
| ClassicLoad | Klasyczne punkty wpisu |
| AdjustedLoad | Obciążenie skorygowane względem poziomu wspinacza |

## 8. Słownik wycen

Skala jest słownikiem danych, a nie wartością zaszytą na stałe w logice aplikacji. Każda pozycja ma etykietę wyceny, punkty oraz indeks trudności.

| Wycena | GradePoints | GradeIndex |
| --- | ---: | ---: |
| 1 | 10 | 0 |
| 2 | 20 | 1 |
| 2+ | 25 | 2 |
| 3 | 30 | 3 |
| 3+ | 35 | 4 |
| 4 | 40 | 5 |
| 4+ | 45 | 6 |
| 5 | 50 | 7 |
| 5+ | 55 | 8 |
| 6A | 60 | 9 |
| 6A+ | 65 | 10 |
| 6B | 70 | 11 |
| 6B+ | 75 | 12 |
| 6C | 80 | 13 |
| 6C+ | 85 | 14 |
| 7A | 90 | 15 |
| 7A+ | 95 | 16 |
| 7B | 100 | 17 |
| 7B+ | 105 | 18 |
| 7C | 110 | 19 |
| 7C+ | 115 | 20 |
| 8A | 120 | 21 |
| 8A+ | 125 | 22 |
| 8B | 130 | 23 |

Słownik musi być rozszerzalny i nie może być technicznie ograniczony do `8B`.

Legenda arkusza zakłada zapis dróg i obwodów małą literą, np. `7a`, oraz baldów wielką literą, np. `7A`. Aktualny słownik stosuje jedną listę z wielkimi literami od `6A` wzwyż. Sposób rozróżnienia skal pozostaje otwartą decyzją.

## 9. Profile EDL

`EDL` oznacza expected difficulty length — spodziewaną długość trudności. Model zakłada, że nie wszystkie ruchy na drodze są równie trudne. Profil dostarcza uogólnioną liczbę ruchów trudnych, która następnie jest korygowana długością całkowitą.

| Profil | Kiedy używać | EDL Base |
| --- | --- | ---: |
| Bald | Klasyczny bald, zwykle 4–10 ruchów | 4 |
| Krótka droga / baldowa | 4–12 ruchów; w praktyce bald z liną | 8 |
| Krotka droga ciągowa | 8–15 ruchów ciągowych | 10 |
| Srednia Ciągowa | 15–40 ruchów bez wyraźnego cruxa | 28 |
| Srednia cruxowa | Wyraźny crux, miejsca odpoczynkowe i łatwiejsze sekcje | 15 |
| Długa Ciągowa | Długa wspinaczka, ponad około 35–40 ruchów | 40 |
| Długa Cruxowa | Długa wspinaczka z wyraźnym cruxem, odpoczynkami i łatwiejszymi sekcjami | 22 |
| Obwód | Treningowy obwód na panelu | 20 |
| Rozgrzewka | Ruchy rozgrzewkowe | 0 |

Nazwy w tabeli zachowują bieżące wartości słownikowe arkusza. Ich korekta językowa nie może utworzyć nowych, równoległych profili.

## 10. Style i mnożniki

Mnożnik stylu odzwierciedla rezultat oraz stopień znajomości problemu.

| Styl | Mnożnik | Znaczenie |
| --- | ---: | --- |
| OS/Flash | 1,50 | Udane przejście bez znajomości albo na podstawie opisu; OS i Flash nie są obecnie liczone oddzielnie |
| RP słaba znajomość | 1,30 | Udane przejście po słabym rozeznaniu; może to być np. druga próba po nieudanym OS; także problem przechodzony wcześniej mniej niż około 10 razy |
| RP normalny | 1,00 | Udane RP po około 11–20 próbach w przyjętym okresie |
| RP stały | 0,75 | Wielokrotnie powtarzane przejście dobrze znanego problemu, ponad około 20 przejść |
| Rozgrzewka | 0,00 | Nie generuje punktów, ale ruchy wliczają się do sumy sesji |
| Attempt OS/FL | 1,50 | Nieudana próba przejścia bez znajomości |
| Attempt słaba znajomość | 1,20 | Nieudana próba na wczesnym etapie poznawania problemu, do około 10 prób |
| Attempt normalny | 0,90 | Nieudana próba po około 11–20 próbach |
| Attempt stały | 0,50 | Regularne próbowanie bardzo dobrze znanego problemu, ponad około 20 prób |

Okres odniesienia dla liczby prób/przejść nie jest całkowicie jednolity w opisach arkusza: część definicji mówi o ostatnich dwóch latach, a część o sezonie. Wymaga to ujednolicenia.

## 11. Względny wysiłek

Najpierw wyznaczana jest różnica indeksów:

`RelativeGradeDiff = GradeIndex - CurrentLevelIndex`

Wartość ujemna oznacza wpis łatwiejszy od aktualnego poziomu. Zero oznacza wycenę równą aktualnemu poziomowi. Wartość dodatnia oznacza wpis trudniejszy.

| RelativeGradeDiff | RelativeEffortMultiplier |
| ---: | ---: |
| -6 | 0,20 |
| -5 | 0,25 |
| -4 | 0,35 |
| -3 | 0,45 |
| -2 | 0,55 |
| -1 | 0,80 |
| 0 | 1,00 |
| 1 | 1,30 |
| 2 | 1,80 |
| 3 | 2,60 |
| 4 | 3,50 |
| 5 | 5,00 — oznaczone w arkuszu jako „Poza skalą” |

Arkusz zwraca techniczne `0`, gdy nie znajdzie różnicy w słowniku. Nie powinno to automatycznie stać się regułą domenową aplikacji. Wcześniejsze ustalenia przewidywały, że wspinaczka daleko poza aktualnym poziomem może być oznaczona jako niemożliwa do normalnej oceny lub „poza skalą”. Zachowanie dla wartości mniejszych niż `-6` i większych niż `5` pozostaje otwarte.

## 12. Kolejność obliczeń wpisu

Obliczenia powinny przebiegać w określonej kolejności.

### Krok 1 — pobranie profilu i EDL Base

`EDL Base` jest pobierane ze słownika profili na podstawie profilu wpisu.

### Krok 2 — obliczenie EDL count

Dla wpisu innego niż rozgrzewka:

`EDL count = EDL Base + 0,2 × Długość całkowita`

Dla rozgrzewki arkusz stosuje wyjątek:

`EDL count = 0`

Przykład dla obwodu o 40 ruchach:

`20 + 0,2 × 40 = 28`

### Krok 3 — pobranie punktów i indeksu wyceny

Na podstawie wyceny pobierane są:

- `GradePoints`;
- `GradeIndex`.

### Krok 4 — wybór poziomu odniesienia

- profil `Bald` używa aktualnego poziomu balda;
- pozostałe profile używają aktualnego poziomu drogi/obwodu.

Z wybranego poziomu pobierany jest `CurrentLevelIndex`.

### Krok 5 — obliczenie różnicy względnej

`RelativeGradeDiff = GradeIndex - CurrentLevelIndex`

### Krok 6 — obliczenie MoveInt

`MoveInt = GradePoints / EDL count`

`MoveInt` jest obiektywnym wskaźnikiem intensywności ruchów w ramach modelu. Ta sama wycena ma większy `MoveInt`, jeżeli profil i długość wskazują na mniejszą liczbę trudnych ruchów.

Dla rozgrzewki `MoveInt` nie jest liczony.

### Krok 7 — pobranie mnożników

- `StyleMultiplier` jest pobierany ze słownika stylów;
- `RelativeEffortMultiplier` jest pobierany ze słownika względnego wysiłku.

### Krok 8 — obliczenie ClassicLoad

Dla wpisu innego niż rozgrzewka:

`ClassicLoad = GradePoints × StyleMultiplier × (Ruchy wykonane / Długość całkowita)`

Jeżeli długość całkowita jest równa zero, bieżący arkusz zwraca zero.

Dla rozgrzewki:

`ClassicLoad = 0`

Współczynnik `Ruchy wykonane / Długość całkowita` nie zastępuje zapisywania wykonanych ruchów. Służy wyłącznie do proporcjonalnego naliczenia klasycznych punktów przy niepełnej próbie.

### Krok 9 — obliczenie AdjustedLoad

`AdjustedLoad = Ruchy wykonane × MoveInt × StyleMultiplier × RelativeEffortMultiplier`

`AdjustedLoad` uwzględnia liczbę wykonanych ruchów, obiektywną intensywność ruchu, styl oraz trudność względem aktualnego poziomu wspinacza.

Dla rozgrzewki `AdjustedLoad = 0`.

## 13. Przykłady kontrolne

Przykłady są częścią specyfikacji i mogą służyć do sprawdzania zgodności przyszłej implementacji.

### 13.1. Obwód 6A+, pełne RP stałe

Dane:

- profil: `Obwód`;
- EDL Base: `20`;
- długość całkowita: `40`;
- ruchy wykonane: `40`;
- wycena: `6A+`;
- GradePoints: `65`;
- GradeIndex: `10`;
- CurrentLevelIndex: `12`;
- RelativeGradeDiff: `-2`;
- RelativeEffortMultiplier: `0,55`;
- styl: `RP stały`;
- StyleMultiplier: `0,75`.

Wyniki:

- `EDL count = 20 + 0,2 × 40 = 28`;
- `MoveInt = 65 / 28 = 2,321428571`;
- `ClassicLoad = 65 × 0,75 × 40/40 = 48,75`;
- `AdjustedLoad = 40 × 2,321428571 × 0,75 × 0,55 = 38,30357143`.

### 13.2. Nieudana próba balda 6B+

Dane:

- profil: `Bald`;
- EDL Base: `4`;
- długość całkowita: `5`;
- ruchy wykonane: `1`;
- wycena: `6B+`;
- GradePoints: `75`;
- GradeIndex: `12`;
- CurrentLevelIndex: `12`;
- RelativeGradeDiff: `0`;
- RelativeEffortMultiplier: `1`;
- styl: `Attempt normalny`;
- StyleMultiplier: `0,9`.

Wyniki:

- `EDL count = 4 + 0,2 × 5 = 5`;
- `MoveInt = 75 / 5 = 15`;
- `ClassicLoad = 75 × 0,9 × 1/5 = 13,5`;
- `AdjustedLoad = 1 × 15 × 0,9 × 1 = 13,5`.

Przykład pokazuje, że nieudana próba nadal tworzy obciążenie na podstawie faktycznie wykonanych ruchów.

## 14. Podsumowanie sesji

Źródłem prawdy dla podsumowania sesji są jej wpisy.

| Wskaźnik | Reguła |
| --- | --- |
| TotalMoves | suma wszystkich wykonanych ruchów, łącznie z rozgrzewką |
| ClassicLoad | suma `ClassicLoad` wpisów; rozgrzewka ma zero |
| AdjustedLoad | suma `AdjustedLoad` wpisów; rozgrzewka ma zero |
| AverageMoveInt | średnia `MoveInt` wpisów ocenianych; rozgrzewka jest pomijana |
| EntryCount | liczba wpisów w sesji |
| CompletedClimbs | liczba ukończonych przejść, bez nieudanych prób i rozgrzewki |
| Duration | różnica między godziną zakończenia i rozpoczęcia |

### 14.1. Wyniki sesji kontrolnych

| Data | TotalMoves | ClassicLoad | AdjustedLoad | AverageMoveInt |
| --- | ---: | ---: | ---: | ---: |
| 01.08.2026 | 130 | 123,75 | 106,61 | 2,38 |
| 05.08.2026 | 51 | 407,10 | 385,04 | 13,46 |

Arkusz miesięczny pokazuje również planowane pola `Ilość sesji`, `Suma punktów` i `Suma ruchów`. Obecnie liczba sesji wynosi `2`, natomiast miesięczne sumy punktów i ruchów nie mają jeszcze zdefiniowanych formuł.

## 15. Integralność danych i historia

### 15.1. Podstawowe ograniczenia

- `Ruchy wykonane` nie mogą być ujemne.
- `Długość całkowita` nie może być ujemna.
- Dla zwykłego wpisu `Ruchy wykonane` nie powinny przekraczać `Długości całkowitej`.
- Wpis oceniany punktowo musi mieć wycenę, profil, styl oraz dodatnią długość całkowitą.
- Profil, wycena i styl powinny pochodzić z właściwych słowników.
- Rozgrzewka liczy ruchy, ale ma zerowe wyniki punktowe.
- Kolejność wpisów w sesji musi być zachowana.
- Ukończone i nieukończone próby muszą być możliwe do rozróżnienia bez analizowania opisu tekstowego.

### 15.2. Zachowanie historii

Słowniki mogą się zmieniać. Zmiana punktów wyceny, bazowego EDL albo mnożnika stylu nie może przeliczyć dawnej sesji tak, jakby nowa reguła obowiązywała w dniu treningu.

Historyczny wpis powinien zachować wartości użyte podczas obliczenia, co najmniej:

- GradePoints;
- GradeIndex;
- EDL Base;
- EDL count;
- CurrentLevelIndex;
- RelativeGradeDiff;
- StyleMultiplier;
- RelativeEffortMultiplier;
- MoveInt;
- ClassicLoad;
- AdjustedLoad.

Referencje do aktualnych słowników mogą pozostać, ale nie są wystarczającym zapisem historycznym.

## 16. Otwarte decyzje przed projektowaniem bazy i API

Poniższych kwestii nie należy rozstrzygać przez zgadywanie:

1. **Hierarchia lokalizacji** — czy wspólny model ma postać `Country → Area/City → Site/Name → Sector`, czy lokalizacje sztuczne i naturalne mają różne struktury.
2. **Typy lokalizacji** — arkusz potwierdza `Bouldering Gym` i `Bouldering spot`, ale nie definiuje pełnego słownika.
3. **Rodzaj wspinaczki a profil EDL** — potrzebne jest osobne, jednoznaczne pole typu `Boulder/Route/Circuit`, ponieważ obecna kolumna `Climb typ` jest w praktyce profilem EDL.
4. **Notacja wycen** — czy drogi i obwody używają `7a`, a baldy `7A`, oraz czy wymagają oddzielnych skal.
5. **Wartości poza RelativeEffort** — zachowanie dla różnicy mniejszej niż `-6` albo większej niż `5`.
6. **Znaczenie aktualnego poziomu** — sposób wyznaczania poziomu, okres obowiązywania oraz moment wykonywania snapshotu.
7. **Zakres znajomości stylu** — ujednolicenie, czy liczba prób/przejść jest liczona w sezonie, w ostatnich dwóch latach, czy w innym okresie.
8. **Numeracja prób** — czy wpis ma numer próby, blok prób lub oba pola.
9. **Sesje przez północ** — sposób zapisu daty końca, jeżeli sesja kończy się następnego dnia.
10. **Miesięczne podsumowania** — jednoznaczna definicja `Suma punktów` oraz tego, czy jest to ClassicLoad, AdjustedLoad, czy oba wyniki.

## 17. Zakres odpowiedzialności przyszłej aplikacji

Użytkownik wprowadza dane faktyczne i wybiera wartości domenowe: sesję, lokalizację, profil, nazwę wspinaczki, wycenę, styl, długość oraz liczbę wykonanych ruchów.

Aplikacja odpowiada za:

- pobranie wartości ze słowników;
- wybranie właściwego poziomu odniesienia;
- wykonanie wszystkich obliczeń;
- walidację danych;
- tworzenie podsumowań sesji i okresów;
- zachowanie historii wyników;
- rozróżnienie przejść, prób i rozgrzewki;
- możliwość korekty wpisu bez utraty spójności całej sesji.

Arkusz jest prototypem modelu obliczeniowego. Docelowo użytkownik nie powinien ręcznie wpisywać indeksów, punktów, mnożników ani wyników wzorów.
