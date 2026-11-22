# Climbing achievement pyramid

Data aktualizacji: 2026-11-04. Status: backend raportu i informacyjny ważony Pyramid Support wdrożone; 120 testów backendu przeszło, local na V22. Ekran Statistics również wdrożony: 11 testów modelu, 22 przeglądarkowe i odczyt wszystkich kombinacji przez prawdziwe API. Reguły DEC-045 i wybór modelu ważonego obowiązują, wariant pełnych warstw odrzucono. Źródło: bieżące odpowiedzi użytkownika, kod, [raport backendu](../documentation/achievement-pyramid-backend.md) i [frontendu](../documentation/statistics-frontend.md).

## Cel i wybory widoku

Piramida pokazuje liczbę różnych ukończonych wspinaczek na kolejnych wycenach. Nie jest sumą wpisów sesji ani wykresem obciążenia.

Uzgodniono trzy niezależne wymiary wyboru:

- dyscyplina: baldy, drogi i obwody, widok zbiorczy według punktów;
- okres: cała zapisana historia albo ostatnie dwa lata;
- styl piramidy: RP albo OS, we wszystkich trzech widokach dyscypliny.

Proponowane etykiety angielskiego UI: `Boulders`, `Routes & circuits`, `Combined — points`; `All history`, `Last 2 years`; `RP (including OS & Flash)`, `OS only`. Domyślne wybory i szczegóły układu pozostają propozycją UI.

## Co kwalifikuje się jako osiągnięcie

Wymagane jest ukończenie `resultType = ASCENT`. Dla drogi z liną dodatkowo wymagane jest `protectionMode = LEAD`.

**Wędka (`TOP_ROPE`) nigdy nie jest osiągnięciem przejścia drogi:** nie trafia do żadnej piramidy, rekordów osobistych ani osiągnięć takich jak Fast RP, niezależnie od zadeklarowanego OS/Flash/RP. Jest wpisem treningowym. Ta zasada nie jest opcjonalnym filtrem, który można wyłączyć w statystykach osiągnięć.

Trening na wędkę pozostaje w sesji, wykonanych ruchach, intensywności, loadzie i historii kontaktów ze wspinaczką. Samo zapisanie `ASCENT` na wędce oznacza ukończenie wpisu treningowego, a nie kwalifikację do osiągnięć. Nie trzeba zmieniać rezultatu takiego wpisu na `ATTEMPT` ani usuwać go z historii.

`ATTEMPT` i `WARMUP` nie są osiągnięciami. Szacowane wyceny części cruxa również nie tworzą osobnych osiągnięć.

## Styl piramidy

- **RP:** kwalifikują się ukończenia w stylu RP, OS i Flash. OS jest najwyższym stylem i jego przejście wypełnia również warstwę piramidy RP.
- **OS:** kwalifikują się wyłącznie ukończenia zadeklarowane jako OS. Flash ani RP nie wchodzą do tego widoku.

Styl decyduje o kwalifikacji, a nie wadze: jedna wspinaczka nadal wnosi jeden element. Wyłączenie wędki obowiązuje przed wyborem stylu i we wszystkich trzech widokach dyscypliny.

To semantyka **piramidy RP**. Nie zmienia automatycznie znaczenia istniejącego pola `rp` w API personal bests, które obecnie wskazuje dokładnie zadeklarowane RP; jego ewentualna zmiana wymaga osobnego ustalenia.

## Jedna wspinaczka — jeden element

Tożsamością jest stabilne `climbId`, nie nazwa, liczba przejść ani para wspinaczka–styl. Reguła obejmuje również prywatne wspinaczki utworzone przy zapisie sesji.

Kolejność wyboru danych: widoczne sesje użytkownika → okres → kwalifikacja osiągnięcia i dyscypliny → styl piramidy → jedno reprezentatywne przejście per `climbId` → grupowanie po wycenie/punktach.

Użytkownik zatwierdził wycenę ze snapshotu **pierwszego kwalifikującego przejścia w wybranym okresie**. Wiele przejść tej samej wspinaczki, także w różnych stylach lub z różnymi zapisanymi wycenami, nie tworzy kilku elementów jednego widoku. Aktualny katalog nie przepisuje wyceny osiągnięcia. Kolejność wpisów w sesji rozstrzyga przejścia tego samego dnia/sesji; pełne deterministyczne rozstrzygnięcie remisów należy do implementacji.

Przykłady:

- Cztery ukończenia tej samej drogi 7a to jeden element warstwy 7a, a nie cztery.
- Pierwsze ukończenie pięć lat temu i powtórzenie teraz daje jeden element w całej historii i jeden w ostatnich dwóch latach.
- OS pięć lat temu i RP teraz: w całej historii droga jest raz w RP i raz w OS; w ostatnich dwóch latach jest raz w RP i nie występuje w OS.
- Flash w wybranym okresie: jeden element w RP, brak w OS.
- Wędka w dowolnym stylu: brak elementu w RP i OS, również w widoku zbiorczym.

Przykłady zakładają, że nie ma innych kwalifikujących przejść zmieniających wynik filtrów. Sum RP i OS nie dodajemy do siebie: widoki mogą zawierać tę samą wspinaczkę.

## Warstwy i prezentacja

Baldy używają skali Font, drogi i obwody skali francuskiej. W widoku zbiorczym równoważność wynika z jednakowych `gradePoints`, nie z loadu, długości, indeksu profilu ani mnożników stylu. Aktualna kalibracja daje 7a = 90 pkt = 6A+, natomiast 6B = 92,5 pkt i tworzy osobną warstwę; przybliżone łączenie przedziałami nie zostało zatwierdzone. Przyszłe łączenie różnych wersji kalibracji punktów wymaga jawnej polityki, bez cichego przeliczania snapshotów.

Propozycja UI: trudniejsze warstwy wyżej, szerokość odpowiada rzeczywistym licznościom, puste warstwy pozostają widoczne w prezentowanym zakresie. Kliknięcie warstwy otwiera listę wspinaczek z miejscem, datą, stylem i źródłową sesją. Nie dopasowujemy danych do sztucznie regularnego kształtu.

## Piramida a Pyramid Support

Piramida jest prezentacją faktów: jakie różne wspinaczki ukończono. **Pyramid Support** jest opisanym wcześniej algorytmem szacowania poziomu na podstawie maksimum i wsparcia na łatwiejszych wycenach. Zasady, progi i przykład znajdują się w [modelu](grading-and-scoring.md#8-poziom-wspinacza-i-trudność-względna).

Limity 2/4/8 i wagi 1/0,5/0,25 dotyczą wyliczenia wsparcia, nie limitu liczby elementów wykresu. Kolejny indeks skali, np. między 6B i 6B+, oznacza pół stopnia w przyjętej terminologii wspinaczkowej; nie należy mylić go z pełnym stopniem przesunięcia cruxa.

Decyzja użytkownika z 2026-11-03 po porównaniu przykładów: **pozostajemy przy dotychczasowym ważonym Pyramid Support**, z wagami 1,00/0,50/0,25, limitami 2/4/8 i progiem 3,00. Nie wymagamy pełnych warstw 1+2+4+8. Przykład 1×7a, 3×6c, 2×6b+, 5×6b nadal daje 6c+; przykład 5×6c, 3×7a, 1×7a+, 2×7b, 1×7b+ daje 7b ze wsparciem 3,25, podczas gdy kandydat 7b+ ma 2,25. Zakładamy różne kwalifikujące się wspinaczki. To podtrzymanie DEC-006 i DEC-032; nie zmieniamy opisanych fallbacków ani confidence.

Algorytm wdrożono 2026-11-03 jako informację w raporcie, z metodą, confidence i dowodami. Nie zmieniono resolvera poziomu ani loadu. Dla wspólnej punktacji nie ustalono analogicznego estymatora; pozostaje również do doprecyzowania treningowy fallback przy historii wyłącznie na wędkę. Te zagadnienia nadal należą do OPEN-20 w [rejestrze decyzji](../documentation/decisions.md#open-20--zakres-estymacji-poziomu-przy-wdrożeniu-piramidy).

### Propozycja 2026-11-03 — pełne, rozłączne warstwy podstawy

**Historyczna propozycja, odrzucona przez użytkownika 2026-11-03 jako zbyt skomplikowana.** Poniższy opis zachowuje przebieg analizy; nie stanowi obowiązującego modelu ani pracy do wdrożenia.

Użytkownik zakwestionował wynik 6c+ z przykładu w modelu: jedna droga 7a nie powinna zastępować brakującej szerokiej podstawy. Celem było premiowanie budowania podstaw i ograniczenie wpływu pojedynczego wysokiego przejścia. Po przeanalizowaniu konsekwencji i kolejnego przykładu użytkownik wybrał zachowanie dotychczasowego wzoru.

Proponowana interpretacja: dla kandydata `G` wymagamy jednocześnie 1 wspinaczki co najmniej na `G`, 2 kolejnych co najmniej na `G−1`, 4 kolejnych co najmniej na `G−2` i 8 kolejnych co najmniej na `G−3`. Są to kolejne indeksy skali (pół stopnia w tym zakresie). Ta sama wspinaczka zajmuje tylko jedno miejsce w przydziale dla danego kandydata. Trudniejsza może wypełnić łatwiejsze miejsce, ale po przydzieleniu nie służy do innej warstwy. Na wykresie osiągnięć zachowuje faktyczną wycenę; przydział dotyczy wyłącznie oceny kompletności podstawy.

Warunki liczności skumulowanych dla tej interpretacji, gdzie `N(T)` to liczba różnych kwalifikujących wspinaczek na `T` lub wyżej:

```text
N(G)   >= 1
N(G−1) >= 3
N(G−2) >= 7
N(G−3) >= 15
```

Spełnienie wszystkich warunków umożliwia rozłączny przydział 1/2/4/8. To nie jest zgoda na policzenie tej samej wspinaczki w kilku miejscach.

Dla przykładu 1×7a, 3×6c, 2×6b+, 5×6b i kandydata 6c+ można przydzielić: szczyt 1×7a; warstwa 2×6c; warstwa 1×6c + 2×6b+ (3 z wymaganych 4); podstawa 5×6b (5 z wymaganych 8). Brakuje jednej wspinaczki co najmniej 6b+ i trzech co najmniej 6b. Próbka zawiera łącznie 11 różnych wspinaczek, dlatego przy stałej głębokości 1+2+4+8 również niższy kandydat 6c nie ma pełnego potwierdzenia. Nie należy automatycznie przypisywać mu wyniku 6c.

Wariant bez oddzielnego miejsca szczytowego wymaga 2+4+8 = 14 wspinaczek. Pozwala wykorzystać 7a w środkowej warstwie i dla 6c+ pozostawia brak trzech wspinaczek podstawy; również nie potwierdza 6c przy 11 wspinaczkach. Oba warianty pozostają odrzucone; obowiązuje wcześniejszy model ważony.

Propozycja raportowania: zachować faktyczne maksimum 7a, a dla ścisłej oceny wskazać niepełną podstawę wraz z brakami. Stary fallback „brak kandydata → najwyższe przejście” oraz fallback małej próbki do maksimum nie mogą wystawiać tego maksimum jako poziomu w pełni potwierdzonego, bo odwracałoby to cel zaostrzenia. Tymczasowy poziom potrzebny do loadu i ewentualna ocena częściowego wsparcia wymagają osobnej reguły.

Ścisły warunek sprawdza szerokość zapisanych osiągnięć. Nie ustala samodzielnie rzeczywistych umiejętności ani wiarygodności wycen rejonu; niepełny dziennik również może dać niepełną podstawę. W tym kroku nie zmieniono starej specyfikacji wzoru, Javy, SQL ani danych i nie uruchamiano testów aplikacji.

## Stan wdrożenia backendu — 2026-11-03

Pełna proponowana kolejność prac: [achievement-pyramid-implementation-plan](../documentation/achievement-pyramid-implementation-plan.md), P01–P13 oraz osobny etap integracji poziomu z loadem. Plan zapisano na prośbę użytkownika 2026-11-03; nie jest potwierdzeniem implementacji.

Dane wejściowe obejmują tożsamość wspinaczki, wycenę i punkty ze snapshotu, styl, asekurację, datę i właściciela sesji. Wdrożono osobny raport pełnej historii w `reporting`, niezależny od limitu 20 sesji Dashboardu, z paginowaną listą wspinaczek warstwy. Wszystkie 12 kombinacji filtrów i ważona estymacja są dostępne przez API.

`DashboardSessionRepository.findPersonalBests`, nowe maksima sesji i `TrainingSessionService.isFastRp` wykluczają wędkę. Widoki V21/V22 i projekcja Details zapewniają poprawny odczyt historycznych maksimów i Fast RP bez nadpisywania surowych danych. Wędka nadal jest pracą treningową i kontaktem z drogą.

`ClimberLevelResolver` i estymacja z pierwszej sesji nadal działają według wcześniejszej reguły i uwzględniają wędkę przy wyborze maksimum treningowego. Ich dostosowanie wymaga rozstrzygnięcia OPEN-20 i jest oddzielnym etapem, ponieważ wpływa na load. Raportowy Pyramid Support już działa, lecz nie zasila tego resolvera.

Pełny zestaw 120 testów przeszedł. Local zastosował V21/V22; HTTP potwierdziło 12 kombinacji i listy. Historia sesji, wpisów i audytu pozostała identyczna. Frontend nie był zmieniany. [Kontrakt i weryfikacja](../documentation/achievement-pyramid-backend.md).
