# Domena

Aktualizacja: 2026-11-09. Reguły DEC-001–052; implementacja rozróżniona od planów poniżej.

## Sesja, miejsce i tożsamość

`TrainingSession` jest granicą atomowego zapisu: jeden użytkownik, jedno `Area`, lokalna `sessionDate`, strefa IANA, początek/koniec i uporządkowane `TrainingEntry`. Może łączyć różne dyscypliny i sektory tego samego miejsca. `entryOrder` określa historię wewnątrz sesji; nazwa sesji może być wygenerowana.

Hierarchia katalogu: `Area → opcjonalny Sector → Climb`. Tożsamością jest UUID, a nie nazwa. Nowe prywatne miejsce/wspinaczka mogą powstać przy zapisie sesji ze stabilnych client IDs. Szybkie Area DRAFT i późniejsze scalanie należą do modelu; pełna obsługa porządkowania katalogu pozostaje dalszą pracą.

Firebase UID wskazuje wewnętrzny UUID użytkownika przez `identity.user_identities`. Nazwa profilu ani e-mail nie przenoszą własności danych. Użytkownik API wynika z weryfikowanego tokenu, a nie z parametru requestu.

## Wpis treningowy

| Pole             | Reguła                                                         |
| ---------------- | -------------------------------------------------------------- |
| `resultType`     | `ASCENT`, `ATTEMPT` albo `WARMUP`; brak osobnego `isCompleted` |
| `climbType`      | `BOULDER`, `ROUTE`, `CIRCUIT`                                  |
| `ascentMode`     | `OS`, `FLASH`, `RP`; niezależne od rezultatu i znajomości      |
| `protectionMode` | `LEAD` lub `TOP_ROPE` tylko dla ROUTE                          |
| `totalMoves`     | pełna długość ocenianej wspinaczki                             |
| `executedMoves`  | faktyczna praca; API wymaga wartości dodatniej                 |

ASCENT wymaga `executedMoves == totalMoves` (DEC-046). ATTEMPT może zawierać mniej ruchów lub więcej przez powtórzenia. Pełna liczba ruchów sama nie dowodzi czystego przejścia. WARMUP wymaga tylko ruchów i danych identyfikacyjnych wpisu; bez wspinaczki, wyceny, stylu i intensywności, z zerowym loadem.

`familiarityBand` wynika z wcześniejszych kontaktów, również wcześniejszych wpisów tej sesji. RP bez historii deklaruje znajomość sprzed aplikacji; konflikt OS/Flash wymaga ostrzeżenia, potwierdzenia i audytu. [Progi i mnożniki](grading-and-scoring.md).

TOP_ROPE pozostaje treningiem i kontaktem ze wspinaczką, lecz nie potwierdza osiągnięcia, Fast RP ani poziomu v3. [Unikalność, okresy i style osiągnięć](achievement-pyramid.md).

## Wyliczenia i historia

Baldy używają Font, drogi i obwody French. Indeksy porównujemy wewnątrz skali; wspólne punkty są wersjonowaną kalibracją. `EffortProfile` określa charakter i wagę wysiłku. `CalculationSnapshot` jest pojęciem domenowym — jego pola są zapisane we wpisie, nie w osobnej tabeli. [Wzory](grading-and-scoring.md).

Nowy POST i jawny PUT liczą v3; GET i identyczny retry odczytują utrwalone wyniki. Korekta zapisuje rewizję przed/po i nie przelicza późniejszych sesji. Usunięcie sesji jest logiczne; historia i audyt pozostają. Archiwizacja katalogu nie usuwa dawnych osiągnięć. Trwały `UserGradeReference` jest planem, obecny poziom v3 to snapshot obliczenia wpisu.

Źródła: [decyzje](../documentation/decisions.md), [schemat](../documentation/database.md), komendy i serwis `backend/src/main/java/pl/climbbetter/training`.
