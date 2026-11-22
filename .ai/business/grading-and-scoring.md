# Wyceny i model obciążenia

Aktualizacja: 2026-11-09. Obowiązuje **v3**: intensywność profili z v2 oraz poziom odniesienia z aktualnością dowodów. Nowy POST i jawny PUT liczą v3; GET i identyczny retry zachowują snapshoty v1/v2/v3. Decyzje DEC-029, 031–032, 043–047; źródła: kalkulatory i resolvery `training`, słowniki Flyway. Pełny opis starego EDL: [archiwum](../archive/documentation-2026-11-09/snapshot/.ai/business/grading-and-scoring.md).

## Skale i punkty

`BOULDER` używa `BOULDER_FONT`, `ROUTE` i `CIRCUIT` — `ROUTE_FRENCH`. Indeks jest porządkiem wewnątrz skali; punktacja to przybliżona wspólna kalibracja loadu, obecnie `gradeCalibrationVersion=1`. Nie jest ścisłą konwersją sportową. Alias Kurtyki wskazuje kanoniczny Grade, zachowując własną wersję i oryginalną etykietę w katalogu.

| `ROUTE_FRENCH` | `gradePoints` | `gradeIndex` |
| -------------- | ------------: | -----------: |
| 1              | 10.0          | 0            |
| 2              | 20.0          | 1            |
| 3              | 25.0          | 2            |
| 4a             | 30.0          | 3            |
| 4b             | 35.0          | 4            |
| 4c             | 40.0          | 5            |
| 5a             | 45.0          | 6            |
| 5b             | 50.0          | 7            |
| 5c             | 55.0          | 8            |
| 6a             | 60.0          | 9            |
| 6a+            | 65.0          | 10           |
| 6b             | 70.0          | 11           |
| 6b+            | 75.0          | 12           |
| 6c             | 80.0          | 13           |
| 6c+            | 85.0          | 14           |
| 7a             | 90.0          | 15           |
| 7a+            | 95.0          | 16           |
| 7b             | 100.0         | 17           |
| 7b+            | 105.0         | 18           |
| 7c             | 110.0         | 19           |
| 7c+            | 115.0         | 20           |
| 8a             | 120.0         | 21           |
| 8a+            | 125.0         | 22           |
| 8b             | 130.0         | 23           |
| 8b+            | 135.0         | 24           |
| 8c             | 140.0         | 25           |
| 8c+            | 145.0         | 26           |
| 9a             | 150.0         | 27           |
| 9a+            | 155.0         | 28           |
| 9b             | 160.0         | 29           |
| 9b+            | 165.0         | 30           |
| 9c             | 170.0         | 31           |

| `BOULDER_FONT` | `gradePoints` | `gradeIndex` |
| -------------- | ------------: | -----------: |
| 2              | 25.0          | 0            |
| 3              | 40.0          | 1            |
| 4-             | 55.0          | 2            |
| 4              | 62.5          | 3            |
| 4+             | 67.5          | 4            |
| 5              | 72.5          | 5            |
| 5+             | 77.5          | 6            |
| 6A             | 85.0          | 7            |
| 6A+            | 90.0          | 8            |
| 6B             | 92.5          | 9            |
| 6B+            | 95.0          | 10           |
| 6C             | 97.5          | 11           |
| 6C+            | 100.0         | 12           |
| 7A             | 105.0         | 13           |
| 7A+            | 107.5         | 14           |
| 7B             | 112.5         | 15           |
| 7B+            | 117.5         | 16           |
| 7C             | 122.5         | 17           |
| 7C+            | 130.0         | 18           |
| 8A             | 135.0         | 19           |
| 8A+            | 140.0         | 20           |
| 8B             | 145.0         | 21           |
| 8B+            | 150.0         | 22           |
| 8C             | 155.0         | 23           |
| 8C+            | 160.0         | 24           |
| 9A             | 165.0         | 25           |
| 9A+            | 170.0         | 26           |

## Intensywność i load

| Profil                 | Waga `profileIntensityIndex` | Charakter                                |
| ---------------------- | ---------------------------- | ---------------------------------------- |
| `BOULDER`              | 10                           | bald, zwykle 4–10 ruchów                 |
| `SHORT_BOULDERY_ROUTE` | 10                           | krótka droga baldowa, ok. 4–12 ruchów    |
| `MEDIUM_CONTINUOUS`    | 7                            | średnia ciągowa, zwykle 15–30 ruchów     |
| `MEDIUM_CRUX`          | 7                            | średnia cruxowa, orientacyjnie 12–20 m   |
| `LONG_CONTINUOUS`      | 5                            | długa ciągowa, zwykle ponad 35–40 ruchów |
| `LONG_CRUX`            | 5                            | długa cruxowa, orientacyjnie ponad 25 m  |
| `CIRCUIT`              | 7                            | obwód treningowy                         |

Przedziały są podpowiedzią, nie automatycznym wyborem profilu. `SHORT_CONTINUOUS_ROUTE` wycofano z nowych wyborów; historia pozostaje czytelna. Wagi są startową kalibracją, nie pomiarem fizjologicznym.

```text
moveIntensity = gradePoints / 100 × profileIntensityIndex
classicLoad = gradePoints × styleMultiplier × executedMoves / totalMoves
adjustedLoad = executedMoves × moveIntensity × styleMultiplier × relativeEffortMultiplier
```

`totalMoves > 0` opisuje długość, dodatnie `executedMoves` — wykonaną pracę. ASCENT wymaga ich równości; ATTEMPT dopuszcza nadmiarowe powtórzenia, bez ograniczania udziału do 1. WARMUP ma tylko ruchy, oba loady 0 i brak intensywności. Długość nie jest dzielnikiem intensywności. Kontrola przy obu mnożnikach równych 1: bald 6B / 4 ruchy → intensywność 9,25 i load 37; 7a średnia ciągowa / 20 → 6,3 i 126; 7a długa ciągowa / 120 → 4,5 i 540.

## Crux

MEDIUM_CRUX i LONG_CRUX korzystają z wagi odpowiadającego profilu ciągowego. Podział nie wymaga opisu sekcji ani miejsca odpadnięcia:

```text
hardMoveCount = ceil(0.15 × executedMoves)
easyMoveCount = executedMoves - hardMoveCount
baseIntensity = routeGradePoints / 100 × profileIntensityIndex
hardMoveIntensity = baseIntensity × hardFontPoints / baseFontPoints
easyMoveIntensity = baseIntensity × easyFontPoints / baseFontPoints
moveWork = hardMoveCount × hardMoveIntensity + easyMoveCount × easyMoveIntensity
moveIntensity = moveWork / executedMoves
adjustedLoad = moveWork × styleMultiplier × relativeEffortMultiplier
```

Bazą Font jest najwyższa aktywna pozycja z punktami ≤ `routeGradePoints + 2.5`, a przy jej braku minimum. Trudna/łatwa część przesuwa bazę o ±2 pozycje aktywnego słownika (pełny stopień w tym zakresie), z ograniczeniem do granic. Load liczymy z pracy części przed zaokrągleniem średniej. Mniejszy średni load cruxa od ciągowej tej samej wyceny jest zaakceptowany.

Przykład 7a z wagą 7: baza Font 6B/92,5 pkt, trudne 6C/97,5, łatwe 6A/85; intensywności 6,6405405405 i 5,7891891892. Z 30 wykonanych ruchów 5 jest trudnych, z 40 — 6. Korekta poziomem nadal używa całej wyceny drogi i poziomu French. Etykiety i intensywności obu części są snapshotami, nie osiągnięciami boulderowymi.

## Styl i znajomość

Kontakty tej samej wspinaczki w ostatnich dwóch latach, bez rozgrzewek: 0 → FIRST_CONTACT, 1–10 → LOW, 11–20 → NORMAL, ponad 20 → ESTABLISHED. Uwzględniamy wcześniejsze wpisy bieżącej sesji. RP bez historii deklaruje wcześniejszą znajomość i używa LOW. OS/Flash z historią wymaga jawnego potwierdzenia konfliktu; zachowuje mnożnik 1,50 i audyt rzeczywistej znajomości.

| Rezultat / tryb      | FIRST_CONTACT          | LOW                   | NORMAL                | ESTABLISHED           |
| -------------------- | ---------------------- | --------------------- | --------------------- | --------------------- |
| ASCENT OS lub FLASH  | 1,50                   | 1,50 po potwierdzeniu | 1,50 po potwierdzeniu | 1,50 po potwierdzeniu |
| ASCENT RP            | 1,30 (deklarowane LOW) | 1,30                  | 1,00                  | 0,75                  |
| ATTEMPT OS lub FLASH | 1,50                   | 1,50 po potwierdzeniu | 1,50 po potwierdzeniu | 1,50 po potwierdzeniu |
| ATTEMPT RP           | 1,20 (deklarowane LOW) | 1,20                  | 0,90                  | 0,50                  |

Fast RP oznacza kwalifikujące RP w drugiej lub trzeciej próbie w całej znanej historii; nie zmienia loadu. Wędka pozostaje kontaktem i treningiem, ale nie osiągnięciem. [Reguły piramidy i personal bests](achievement-pyramid.md).

## Poziom odniesienia v3

Osobno Font i French. Dla zapisywanej sesji bierzemy wcześniejsze aktywne osiągnięcia bez TOP_ROPE/ATTEMPT, nie przyszłe sesje. W tym samym dniu początek historii nie może być późniejszy od początku zapisywanej sesji. Każdy `climbId` wnosi najnowsze kwalifikujące przejście ze snapshotem wyceny; powtórzenie odświeża datę, bez zwiększania sample size. Edytowana sesja nie jest własnym dowodem.

```text
ageDays = sessionDate - achievedOn
weight = max(0, 1 - ageDays / 730)^2
```

Waga: dziś 1, po 365 dniach 0,25, od 730 dni 0. Historia jest liczona względem daty treningu. Przy 1–9 różnych wspinaczkach obowiązuje wyjątek: maksimum bez osłabiania wagami. Od 10 działa `recency-weighted-peak-v1`: sortowanie po indeksie malejąco i zebranie najwyższej jednej jednostki masy.

```text
remaining = 1; effectiveLevel = 0
for evidence in descendingGradeOrder:
    contribution = min(weight, remaining)
    effectiveLevel += contribution × gradeIndex
    remaining -= contribution
effectiveLevel += remaining × currentSessionFallback
```

Fallback: najwyższe kwalifikujące przejście bieżącej sesji; przy jego braku indeks o jeden poniżej najłatwiejszego wpisu treningowego (próby lub wędki), minimum 0. To prowizoryczne odniesienie, nie osiągnięcie. Przykład przy próbce ≥10: dawne 7b/index 17 o wadze 0,25 oraz świeże 7a/index 15 wypełniające pozostałe 0,75 → efektywny indeks 15,5. Rzeczywiste wyceny nie zmieniają się. Wyjątek małej próbki i granica 10 nadal mogą powodować skok.

`effectiveRelativeGradeDifference = gradeIndex - effectiveCurrentLevelIndex`; `currentLevelIndex` jest podłogą efektywnej wartości dla zgodności. Mnożnik interpolujemy liniowo między sąsiadami tabeli:

| Różnica | -6   | -5   | -4   | -3   | -2   | -1   | 0    | 1    | 2    | 3    | 4    | 5    |
| ------- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| Mnożnik | 0,20 | 0,25 | 0,35 | 0,45 | 0,55 | 0,80 | 1,00 | 1,30 | 1,80 | 2,60 | 3,50 | 5,00 |

Lookup ma floor/cap na −6/+5, ale snapshot zachowuje rzeczywistą różnicę. `outsideCalibratedRange` jest true dopiero poza granicami. Np. −0,5 daje 0,90. Brak lub nakładanie reguł jest błędem, nigdy mnożnikiem 0. Raportowy Pyramid Support to [osobny estymator](achievement-pyramid.md), bez tego ważenia czasem.

## Agregacja, precyzja i trwałość

Sumujemy ruchy wszystkich wpisów i oba loady. Średnia intensywność = suma `(moveIntensity × executedMoves)` ocenianych wpisów / suma ich ruchów; rozgrzewka nie wchodzi do mianownika, pusty mianownik daje NULL. Liczba wpisów ASCENT nie jest liczbą różnych osiągnięć.

Obliczenia dziesiętne (`BigDecimal`/`numeric`), pośrednio skala 10 i HALF_UP; intensywność, efektywne indeksy i wagi zachowują 10 miejsc. Load zapisujemy do 4 miejsc. Interpolowany mnożnik v3 zaokrąglamy HALF_UP do 4 miejsc przed użyciem. UI zwykle pokazuje 2 miejsca. Sumujemy wartości źródłowe, bez średniej ze średnich i bez formatowanych tekstów.

Snapshot utrwala wejścia, reguły, wyniki i wersje. Upływ czasu, odczyt i zmiana słownika nie przeliczają zapisanych wyników. PUT przelicza tylko edytowaną sesję, z audytem. [Implementacja v3](../documentation/recency-load.md), [agregaty miesiąca/roku](../documentation/period-training-summary.md).
