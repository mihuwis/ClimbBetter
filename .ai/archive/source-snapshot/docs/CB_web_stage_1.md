# ClimbBetter — pierwszy użyteczny etap web

Data: 2026-09-22  
Status: wymagania użytkownika i propozycja do omówienia; bez implementacji.

## Cel i sposób pracy

Na koniec etapu użytkownik zapisuje i analizuje rzeczywiste treningi w webie zamiast w Excelu. Sam prototyp formularza z mockami nie spełnia tego celu: potrzebne są trwały zapis, historia i obliczenia backendu.

Użytkownik pisze kod. Asystent prowadzi dokumentację, proponuje rozwiązania i sprawdza kod. Ten dokument nie jest poleceniem implementacji przez asystenta.

Planujemy dokładnie bieżący etap. Przyszłe funkcje opisujemy przez potrzebne granice i zachowywane dane, bez szczegółowego wieloetapowego harmonogramu. Ograniczamy zakres zmian potrzebnych do rozszerzeń; nie zakładamy, że każda przyszła funkcja obejdzie się bez zmiany istniejącego kodu.

Wymagania poniżej pochodzą z rozmowy. Akapity oznaczone jako propozycja lub kwestia otwarta nie są zatwierdzonymi regułami domeny.

## Wymagany przepływ

Dashboard → `+ Add session` → wybór intencji:

- `Record session` — pierwszy opracowywany przepływ;
- `Add finished session` — wejście przewidziane, szczegóły do późniejszego omówienia;
- `Add single climb` — wejście przewidziane, znaczenie i powiązanie z sesją do ustalenia.

Nie obiecujemy działania dwóch pozostałych akcji przed ich implementacją. Wszystkie mają docelowo korzystać ze wspólnego modelu wpisu, wyboru wspinaczki i obliczeń. Nie tworzymy trzech niezależnych modeli treningu.

### Record session i czas

- Osobny widok z `Start session` i widocznym czasem trwania.
- Czas biegnie do `Stop`; nie ma pauzy.
- Pod zegarem użytkownik dodaje pierwszą i kolejne próby/przejścia.
- Po `Stop` użytkownik kończy i zapisuje sesję przez `Finish session`.

Ustalenie z rozmowy: `Start` i dodawanie wpisów nie tworzą rekordów w bazie backendu. Robocza sesja należy do frontendu. Dopiero zakończenie sesji powoduje zapis całego treningu w backendzie. Zgodnie z dotychczasowym podziałem przycisków `Stop` zapamiętuje lokalnie koniec czasu, a `Finish session` wysyła całość do zapisu. Sprawdzanie danych po zatrzymaniu nie wydłuża treningu.

Stany przygotowanie → rejestrowanie → sprawdzenie po zatrzymaniu → zapisana dotyczą interfejsu i lokalnego szkicu. Nie wymagamy endpointu rozpoczynającego sesję ani rekordów `IN_PROGRESS`/`STOPPED` w bazie backendu.

Propozycja niezawodności na ten etap:

- Czas wynika z zapisanego początku i końca, a nie z liczby tyknięć timera w przeglądarce.
- Odświeżenie, zamknięcie karty i uśpienie urządzenia nie zerują sesji ani czasu.
- Robocza sesja i jej wpisy są utrwalane lokalnie w przeglądarce podczas treningu. Stan wyraźnie odróżnia lokalny szkic od sesji zapisanej na serwerze. Nie wymaga to pełnej synchronizacji offline mobile i nie gwarantuje odzyskania szkicu na innym urządzeniu ani po usunięciu danych przeglądarki.
- Ponowienie zapisu nie tworzy drugiej sesji ani drugiej próby; wykorzystujemy stabilne identyfikatory klienta/idempotencję.
- Proponowane ograniczenie v1: jeden aktywny szkic na użytkownika w danej przeglądarce; powrót prowadzi do niego. Sesja musi być przypisana lokalnie do użytkownika. To nie jest serwerowa gwarancja jednej aktywnej sesji na wszystkich urządzeniach. Trzeba zapobiec cichej utracie zmian między kartami.
- Można skorygować błędne godziny, np. po zapomnianym `Stop`; zachowujemy informację, że czas został poprawiony.
- Odpoczynki między próbami wliczają się do czasu sesji.

Backend przyjmuje ukończoną sesję z rzeczywistym czasem, miejscem i uporządkowanymi wpisami. Lokalny szkic może być niekompletny; nie musi spełniać wszystkich warunków ukończonej sesji. Poprzednia propozycja zapisywania rozpoczętej sesji w backendzie została zastąpiona tym ustaleniem.

### Robocza sesja i pamięć podpowiedzi

Rozdzielamy dwa zadania po stronie frontendu:

- Odzyskiwalny szkic sesji: `clientSessionId`, początek/koniec, lokalny stan, miejsca/wspinaczki i uporządkowana lista wpisów z własnymi ID. To źródło danych jeszcze niezapisanych na serwerze.
- Cache wyników API: katalog, wcześniejsza historia, ostatnio używane miejsca i drogi. Cache można odtworzyć przez ponowne pobranie; nie zastępuje szkicu sesji.

Podpowiedzi łączą aktualny szkic z historią pobraną z API. Propozycja sekcji: `This session`, `Recent`, pozostałe pasujące wyniki. Nadal obowiązują filtry Area/Sector i brak duplikatów. Kolejna próba tej samej drogi korzysta z lokalnego kontekstu bez ponownego wyszukiwania.

Każda próba pozostaje osobnym wpisem. Liczba wcześniejszych prób jest wyprowadzana z listy wpisów; nie jest jedyną przechowywaną informacją. Usunięcie, korekta lub zmiana kolejności wcześniejszego wpisu powoduje ponowną ocenę zależnych sugestii i podglądu obliczeń.

Istniejąca droga używa `climbId`. Nowa droga otrzymuje stabilne `clientClimbId`, wspólne dla jej kolejnych prób; identyczna nazwa nie jest dowodem tożsamości. Nowe miejsca/drogi utworzone w ramach szkicu pozostają lokalne do końcowego zapisu sesji.

### Historia przy obliczaniu i końcowy zapis

Frontend ma komplet roboczych wpisów, ale backend nadal odpowiada za walidację stylu, znajomości i loadu. Dla każdego wpisu backend uwzględnia historię sprzed niego: właściwe wcześniejsze rekordy w bazie oraz wcześniejsze wpisy przesłanej sesji. Nie uwzględnia późniejszych wpisów jako wcześniejszych prób. Obowiązują dotychczasowe okna historii i kolejność domenowa, nie kolejność dotarcia requestów.

Przykład bez wcześniejszej historii: wpis 1 to nieudana pierwsza próba, wpis 2 to przejście tej samej drogi. Wpis 2 ma już jedną wcześniejszą próbę, więc aplikacja sugeruje RP, a nie OS. Istniejące zasady jawnego, audytowanego override OS/Flash pozostają bez zmian.

Propozycja podglądu w trakcie treningu: backend przyjmuje uporządkowany szkic i zwraca obliczenia bez zapisu sesji, prób ani roboczych obiektów katalogu do bazy. Podgląd i końcowy zapis używają tych samych reguł. Podgląd nie rezerwuje wyniku; po zmianie szkicu lub historii może być nieaktualny.

Zakończenie wysyła jedną ukończoną sesję wraz ze wszystkimi wpisami. Backend ponownie sprawdza bieżącą historię, rozwiązuje identyfikatory nowych miejsc/dróg, oblicza wpisy w kolejności i zapisuje sesję, wpisy oraz snapshoty atomowo. Sposób ochrony przed równoległymi zapisami zmieniającymi historię trzeba określić w kontrakcie implementacyjnym; wynik podglądu klienta nie jest źródłem prawdy.

Ponowienie zapisu z tym samym `clientSessionId` nie tworzy duplikatu i nie dolicza własnych już zapisanych prób drugi raz. Szkic pozostaje lokalnie do potwierdzenia sukcesu przez backend; błąd lub utrata odpowiedzi pozwala ponowić zapis. Zmiana treści pod tym samym ID po skutecznym zapisie wymaga jawnej edycji, a nie cichego ponownego utworzenia sesji.

### Wybór miejsca i wspinaczki

Wymagania:

- Wyszukiwanie Area, opcjonalnego Sector i Climb z podpowiedziami z własnej historii.
- Pasujące miejsca z historii mają priorytet w sekcji `Recent areas`; analogicznie sektory i wspinaczki.
- Wybrane Area ogranicza sektory i wspinaczki do tego Area. Wybrany Sector dodatkowo ogranicza wspinaczki do tego sektora.
- Droga o tej samej nazwie z innego Area nie pojawia się w wynikach ograniczonych do wybranego miejsca.
- Wybór istniejącej wspinaczki wczytuje jej wycenę, liczbę ruchów i profil wysiłku.

Propozycja wyszukiwania: dopasowanie fragmentów słów z pominięciem wielkości liter i polskich znaków, tak aby `Bed` znajdowało `Dolina Będkowska`. Najpierw filtr miejsca i tekstu, następnie podział pasujących wyników na historię użytkownika i pozostałe. Wyniki nie dublują się między sekcjami. Można pokazywać datę ostatniego użycia.

Zmiana Area czyści niepasujący Sector i Climb; zmiana Sector czyści niepasujący Climb. Opóźniona odpowiedź wyszukiwania ze starego miejsca nie może zastąpić bieżących wyników.

Brak sektora nie blokuje zapisu. Gdy brak miejsca/drogi w katalogu, użytkownik może dodać własną nazwę i podstawowe parametry. Najpierw powstaje lokalny obiekt ze stabilnym ID klienta; przy końcowym zapisie sesji backend tworzy prywatny draft katalogowy zgodnie z istniejącym modelem. Rozbudowany katalog nie jest warunkiem logowania.

Kwestia otwarta: jedna czy wiele Area w sesji. Obecny `TrainingSession.areaId` opisuje jedno Area. Wielorejonowa sesja wymaga lokalizacji przy wpisach i zmiany kontraktu; przyciski szybkiego dodawania same tego nie rozwiązują.

### Próba lub przejście

Użytkownik widzi/uzupełnia wycenę, profil wysiłku, całkowitą liczbę ruchów, wykonane ruchy i rezultat. Ukończenie bez odpadnięcia jest jawną deklaracją. `12/12` ruchów samo nie dowodzi czystego przejścia.

Backend wyprowadza znajomość z wcześniejszych wpisów tej samej wspinaczki, uwzględniając wcześniejsze wpisy bieżącej sesji. Obowiązuje [model wyceny](CB_climbing_effort_valuation.md), w tym rozdzielenie rezultatu, OS/Flash/RP i familiarity band.

Brak wpisu w historii nie dowodzi OS. OS i Flash wymagają deklaracji użytkownika; użytkownik mógł też wspinać się tu przed rozpoczęciem korzystania z aplikacji. Propozycja UI: sugestia trybu z krótkim uzasadnieniem i możliwością korekty. Istniejący wymóg potwierdzenia/audytu konfliktu OS/Flash z historią pozostaje.

`Repeat` może być etykietą ponownego przejścia znanej, wcześniej ukończonej drogi; nie zastępuje `ascentMode` ani nie oznacza automatycznie `ESTABLISHED`. Duża liczba prób i wcześniejsze ukończenie to różne informacje.

Szybkie akcje:

| Akcja | Zachowany kontekst | Dane nowego wpisu |
| --- | --- | --- |
| Kolejna próba tej samej wspinaczki | Area, Sector, Climb i parametry wspinaczki | nowy rezultat i ruchy; styl ponownie ustalany |
| Dodaj z tego samego sektora | Area i Sector | wybór kolejnej wspinaczki |
| Dodaj z tego samego rejonu | Area | wybór sektora/wspinaczki |

Kopiowanie kontekstu nie kopiuje sukcesu, OS ani wykonanych ruchów. Korekta parametrów wpisu nie edytuje automatycznie katalogowej drogi.

Propozycje angielskich etykiet, niezależne od istniejących kodów profili:

| Profil | Proponowana etykieta |
| --- | --- |
| Krótka droga baldowa | Short bouldery route |
| Krótka droga ciągowa | Short sustained route |
| Średnia ciągowa | Medium-length sustained route |
| Średnia cruxowa | Medium-length cruxy route |
| Długa ciągowa | Long sustained route |
| Długa cruxowa | Long cruxy route |

## Obliczenia i historia

Każdy wpis zachowuje dane i snapshot wymagane przez istniejący model: m.in. profil, `baseEdl`, `edlCount`, `moveIntensity`, ruchy, wycenę, styl i mnożniki, poziom odniesienia, `classicLoad`, `adjustedLoad` i wersję obliczeń. EDL nie jest synonimem całkowitej liczby ruchów.

Backend pozostaje źródłem obliczeń zarówno dla podglądu, jak i zatwierdzonego wyniku. Frontend wyświetla wartości i wyjaśnienia. Poziom balda i poziom drogi są oddzielne. Zmiana aktualnego poziomu nie przepisuje historycznych snapshotów.

Do doprecyzowania w kontrakcie rejestrowania: jak prezentujemy podgląd Adjusted Load, gdy użytkownik nie ma jeszcze poziomu, który zgodnie z modelem jest szacowany po sesji. Nie pokazujemy fikcyjnego zera. Trzeba jawnie określić moment obowiązywania nowego poziomu i rozliczenie pierwszej sesji.

### Training Density — propozycja włączenia do etapu

Model wyceny już wymienia `loadDensity` jako propozycję przyszłej statystyki. Nie jest to jeszcze zatwierdzona część v1. Proponujemy:

- `loadDensity = adjustedLoad / durationMinutes`, jednostka Adjusted Load/min;
- `moveDensity = executedMovesTotal / durationMinutes`, jednostka moves/min.

To dwie osobne miary, bez arbitralnego mieszania ruchów i punktów. Czas obejmuje odpoczynki. Brak wiarygodnego dodatniego czasu daje brak wyniku, nie zero. Czas zmierzony, wpisany ręcznie i szacowany powinny być rozróżnialne. Dla wielu sesji liczymy sumę loadu/ruchów przez sumę odpowiadającego im czasu, nie średnią gęstości sesji; ujawniamy niekompletność danych.

Wyższa gęstość opisuje więcej pracy w czasie; interfejs nie ocenia automatycznie, że oznacza lepszy trening. To metryka pochodna, niezależna od kalkulatora wpisu.

## Dashboard

- `+ Add session` uruchamia wybór intencji.
- `Adjusted Load` zastępuje `Score` na karcie.
- `Moves` pozostaje. `Max grade` powinno opisywać najwyższą ukończoną wycenę; przy samych próbach brak ukończonej wyceny. W sesji mieszanej skale balda i drogi pokazujemy oddzielnie.
- Pozostałe sekcje pozostają na tym etapie bez przebudowy.

Obecne słupki w `ActivityCard` mają stałe wysokości, więc nie reprezentują danych. Propozycja: rozkład prób/przejść według wycen, z podpisaną skalą i liczbą wpisów. Kolor oraz wypełnienie/oznaczenie rozróżniają próby i przejścia. Tooltip podaje liczby i load. Skale balda i drogi pozostają oddzielne. Szczegóły wykresu do omówienia.

## Szczegóły sesji skoncentrowane na wspinaczkach

Wymagania: wpisy pogrupowane według drogi/balda, krótki opis, możliwość przewidzenia zdjęcia, czytelność bez długiej rozwiniętej listy wszystkich prób.

Propozycja web: zwarte podsumowanie sesji nad dwoma panelami. Z lewej kompaktowa, filtrowalna lista unikalnych wspinaczek (nazwa, wycena, liczba prób/przejść); z prawej szczegóły wybranej wspinaczki. Na małym ekranie lista otwiera pojedynczy panel szczegółów. Przy dużej liczbie różnych dróg lista ma własne przewijanie lub stronicowanie; nie obiecujemy braku przewijania dla dowolnej sesji.

Panel drogi: nazwa, lokalizacja, wycena, profil, ruchy, krótki opis i opcjonalna miniatura; poniżej próby z TEJ sesji w zwartej tabeli (rezultat/styl, ruchy, Classic Load, Adjusted Load). Rozwinięcie obliczeń pokazuje EDL, Move Intensity, poziom odniesienia i mnożniki. Oddzielamy opis drogi od notatek o próbie.

Grupowanie po stabilnym `climbId`/rozwiązanym ID klienta, nigdy po samej nazwie. Nie tracimy `entryOrder`: powrót do tej samej drogi po innej drodze nadal jest widoczny w chronologii. `attemptBlockId` może wyróżniać kolejne bloki prób; nie zastępuje tożsamości drogi. Różne historyczne parametry wpisów pokazujemy przy próbach zamiast nadpisywać je jednym nagłówkiem grupy.

Zdjęcie jest opcjonalnym rozszerzeniem prezentacji katalogu. Miejsce w układzie można przewidzieć teraz; upload, przechowywanie i zarządzanie zdjęciami nie są automatycznie częścią bieżącego etapu.

URL do uzgodnienia: `/sessions/:sessionId` już poprawnie identyfikuje widok sesji. Obecny tekst `zimny-dol-2026-05-24` jest mockowym ID. `/sessions/:sessionId/details` też jest poprawne, ale samo `details` nie daje większej rozszerzalności. Propozycja: stabilne ID oraz `/sessions/:sessionId` dla podglądu, `/sessions/:sessionId/record` dla rejestrowania i osobne `/edit` po wdrożeniu edycji.

## Granice rozszerzeń

- `training` jest właścicielem sesji, wpisów, rejestrowania i ich formularzy; Dashboard tylko otwiera przepływ i prezentuje wyniki.
- `catalog` jest właścicielem wyboru miejsca/wspinaczki i historii użycia potrzebnej do podpowiedzi. Sposób budowania rankingu podpowiedzi pozostaje za jego publicznym API.
- Trzy intencje dodawania wykorzystują wspólny formularz wpisu i usługi; różnią się sposobem pozyskania czasu i kontekstu. Nie budujemy jednego ogromnego formularza z flagami dla wszystkich przyszłych przypadków.
- Stabilne ID, kolejność wpisów i snapshoty są fundamentem historii, grupowania, późniejszych zdjęć i analityki.
- Density i grupy dróg powstają w warstwie odczytu/raportowania bez przebudowy wzorów loadu.
- Nowy profil wysiłku powinien wynikać ze słownika i wersji modelu; tekst etykiety nie steruje logiką UI.

## Dowód ukończenia pierwszego przepływu

Użytkownik uruchamia sesję, znajduje znaną drogę z historii albo dodaje nieznaną, zapisuje nieudaną próbę i przejście tej samej drogi, dodaje inną drogę, odświeża widok bez utraty danych, zatrzymuje czas i kończy sesję. Po ponownym otwarciu aplikacji widzi ją na Dashboardzie oraz poprawnie pogrupowane szczegóły z trwałymi wynikami backendu.

Weryfikacja obejmuje odróżnienie dróg o tej samej nazwie w różnych miejscach, uwzględnienie wcześniejszych prób bieżącej sesji, brak duplikatów po ponowieniu zapisu i porównanie wyliczeń z zatwierdzonymi przykładami/realnymi sesjami z Excela. Sam obecny arkusz nie został ponownie odczytany w tej rozmowie; zgodność wszystkich jego kolumn trzeba jeszcze sprawdzić przed uznaniem aplikacji za pełny zamiennik.

Najbliższe dwa kroki pracy: doprecyzować kontrakt ukończonej sesji z uporządkowanymi wpisami i kontekstem historii, następnie przygotować mały zakres implementacji zapisu/odczytu oraz sprawdzenia przypadku próba → przejście tej samej drogi. Nie zaczynamy od endpointu `Start session`. Pozostałe intencje i przyszłe moduły nie otrzymują teraz szczegółowego harmonogramu.
