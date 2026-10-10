# Rejestrowanie, ocena i rozwój treningów

Status: wymagania z 2026-09-22 oraz jawnie oznaczone propozycje; konsolidacja i uzupełnienie o DEC-018 z 2026-10-03.

Najpierw wdrażamy przepływ `Record session` w webie i wspierający go backend Java. Poniższe szczegóły przeniesiono z [pierwszego etapu web](../archive/source-snapshot/docs/CB_web_stage_1.md), zachowując rozróżnienie wymagań, propozycji i pytań. Aktualizacja dokumentacji nie zatwierdza propozycji.

## Ocena sesji

Główna miara to `adjustedLoad`; `classicLoad` pozostaje porównawczy. Ruchy obejmują rozgrzewkę, ale rozgrzewka nie wnosi loadu. Intensywność agregujemy z właściwymi wagami. Poziom odniesienia, peak grade i profil stylów pokazujemy oddzielnie. Pełna definicja: [grading-and-scoring](grading-and-scoring.md).

## Późniejszy rozwój

Po stabilnym zapisie i historii: dodawanie już zakończonej sesji, rozstrzygnięcie znaczenia pojedynczej wspinaczki, edycja z audytem, integracja mobile i synchronizacja. Osobny przyszły moduł planowania obejmie plany, periodyzację, cykle i propozycje treningowe; zgodnie z `DEC-012` nie dodajemy teraz pól cykli do sesji v1.

Generowanie propozycji treningowych jest kierunkiem wskazanym przez użytkownika, a nie zatwierdzonym algorytmem. Nie ustalono jeszcze typów ćwiczeń poza wspinaniem, danych o celach i dostępności ani reguł doboru obciążeń (`OPEN-16`). Miary opisowe nie oznaczają automatycznie jakości treningu lub gotowości do kolejnego wysiłku.

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

Wymagania niezawodności zatwierdzone w DEC-021:

- Czas wynika z zapisanego początku i końca, a nie z liczby tyknięć timera w przeglądarce.
- Odświeżenie, zamknięcie karty i uśpienie urządzenia nie zerują sesji ani czasu.
- Robocza sesja i jej wpisy są utrwalane lokalnie w przeglądarce podczas treningu. Stan wyraźnie odróżnia lokalny szkic od sesji zapisanej na serwerze. Nie wymaga to pełnej synchronizacji offline mobile i nie gwarantuje odzyskania szkicu na innym urządzeniu ani po usunięciu danych przeglądarki.
- Ponowienie zapisu nie tworzy drugiej sesji ani drugiej próby; wykorzystujemy stabilne identyfikatory klienta/idempotencję.
- Szkic pozostaje do potwierdzonego sukcesu zapisu albo jawnego odrzucenia przez użytkownika. Nieudany zapis lub utrata odpowiedzi nie usuwa wprowadzonych danych.
- Do ustalenia w pozostałej części `OPEN-04`: czy v1 ogranicza użytkownika do jednego aktywnego szkicu w danej przeglądarce i jak rozwiązuje konflikt zmian między kartami. Sesja musi być przypisana lokalnie do użytkownika; nie jest to serwerowa gwarancja jednej aktywnej sesji na wszystkich urządzeniach.
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
- Jedno Area jest miejscem całej sesji (DEC-018). Wpisy mogą obejmować różne sektory i wspinaczki należące do tego miejsca.
- Pasujące miejsca z historii mają priorytet w sekcji `Recent areas`; analogicznie sektory i wspinaczki.
- Wybrane Area ogranicza sektory i wspinaczki do tego Area. Wybrany Sector dodatkowo ogranicza wspinaczki do tego sektora.
- Droga o tej samej nazwie z innego Area nie pojawia się w wynikach ograniczonych do wybranego miejsca.
- Wybór istniejącej wspinaczki wczytuje jej wycenę, liczbę ruchów i profil wysiłku.

Propozycja wyszukiwania: dopasowanie fragmentów słów z pominięciem wielkości liter i polskich znaków, tak aby `Bed` znajdowało `Dolina Będkowska`. Najpierw filtr miejsca i tekstu, następnie podział pasujących wyników na historię użytkownika i pozostałe. Wyniki nie dublują się między sekcjami. Można pokazywać datę ostatniego użycia.

Podczas wyboru miejsca zmiana Area czyści niepasujący Sector i Climb w selektorze; zmiana Sector czyści niepasujący Climb. Nie oznacza to usunięcia ani przeniesienia istniejących wpisów szkicu: muszą one nadal należeć do Area sesji. Przeniesienie treningu do innego Area wymaga osobnej sesji. Opóźniona odpowiedź wyszukiwania ze starego miejsca nie może zastąpić bieżących wyników.

Brak sektora nie blokuje zapisu. Gdy brak miejsca/drogi w katalogu, użytkownik może dodać własną nazwę i podstawowe parametry. Najpierw powstaje lokalny obiekt ze stabilnym ID klienta; przy końcowym zapisie sesji backend tworzy prywatny draft katalogowy zgodnie z istniejącym modelem. Rozbudowany katalog nie jest warunkiem logowania.

Ustalone 2026-10-03: jedna sesja obejmuje jedno Area — rejon lub ściankę wspinaczkową (DEC-018; zamknięte OPEN-03). Różne sektory w tym samym Area mogą należeć do jednej sesji. Wspinanie w innym Area zapisujemy jako kolejną sesję. Tę regułę wyjaśniamy użytkownikowi przy opisie logowania; przykładowa treść znajduje się w [product](product.md).

### Próba lub przejście

Użytkownik widzi/uzupełnia wycenę, profil wysiłku, całkowitą liczbę ruchów, wykonane ruchy i rezultat. Ukończenie bez odpadnięcia jest jawną deklaracją. `12/12` ruchów samo nie dowodzi czystego przejścia. `executedMoves` liczy rzeczywiście wykonaną pracę, również powtórzone ruchy po odpadnięciu, dlatego może przekroczyć `totalMoves`; wpis `12/20` jest poprawną próbą.

Backend wyprowadza znajomość z wcześniejszych wpisów tej samej wspinaczki, uwzględniając wcześniejsze wpisy bieżącej sesji. Obowiązuje [model wyceny](grading-and-scoring.md), w tym rozdzielenie rezultatu, OS/Flash/RP i familiarity band.

Brak wpisu w historii nie jest nieodwracalnym dowodem OS, bo użytkownik mógł wspinać się tu przed rozpoczęciem korzystania z aplikacji. W szybkim formularzu ukończony, nowo nazwany bald otrzymuje jednak edytowalną sugestię OS (DEC-028). Dzięki temu typowy nowy problem nie wymaga dodatkowego pytania, a użytkownik nadal może poprawić tryb. Istniejący wymóg potwierdzenia/audytu konfliktu OS/Flash z zapisaną historią pozostaje.

Jeżeli użytkownik nie poda długości balda, formularz proponuje `totalMoves = 8` (DEC-027). Domyślna wartość nie zastępuje wiedzy użytkownika: krótki bald może mieć np. 4 ruchy, a projekt 6–10. `executedMoves` zawsze opisuje wykonaną pracę i dla nieukończonej próby może wynosić np. 2–4.

`Repeat` może być etykietą ponownego przejścia znanej, wcześniej ukończonej drogi; nie zastępuje `ascentMode` ani nie oznacza automatycznie `ESTABLISHED`. Duża liczba prób i wcześniejsze ukończenie to różne informacje.

Szybkie akcje:

| Akcja                              | Zachowany kontekst                         | Dane nowego wpisu                             |
| ---------------------------------- | ------------------------------------------ | --------------------------------------------- |
| Kolejna próba tej samej wspinaczki | Area, Sector, Climb i parametry wspinaczki | nowy rezultat i ruchy; styl ponownie ustalany |
| Dodaj z tego samego sektora ------ | Area i Sector ---------------------------- | wybór kolejnej wspinaczki ------------------- |
| Dodaj z tego samego rejonu ------- | Area ------------------------------------- | wybór sektora/wspinaczki -------------------- |

Kopiowanie kontekstu nie kopiuje sukcesu, OS ani wykonanych ruchów. Korekta parametrów wpisu nie edytuje automatycznie katalogowej drogi.

### Scenariusz akceptacyjny: sesja boulderowa Bronx

Najbliższy pion zapisu ma obsłużyć bez zbędnych pytań następującą sesję:

1. Użytkownik wybiera lub tworzy Area `Bronx` i rozpoczyna lokalny szkic sesji.
2. Dodaje rozgrzewkę z `executedMoves = 20`; wpis nie ma wyceny, EDL ani loadu.
3. Dodaje trzy ukończone, nazwane baldy o wycenach `5C` lub `5C+`. Nie ma ich wcześniejszej historii, więc formularz proponuje OS i po 8 ruchów; użytkownik może poprawić obie wartości.
4. Dodaje dwa ukończone baldy `6A`, które istnieją już w historii. Backend sugeruje RP na podstawie wcześniejszych kontaktów; jeden z problemów ma rzeczywistą długość 4 ruchów zamiast domyślnych 8.
5. Dodaje próby projektów `6C`: wykonuje w nich po 2–4 ruchy, podczas gdy całe problemy mają 6–10 ruchów. Każda próba jest osobnym, uporządkowanym wpisem.
6. Kończy sesję. Backend oblicza wpisy i podsumowanie, zapisuje całość atomowo, a Dashboard odczytuje zapisaną sesję.

Podstawowe podsumowanie pokazuje sumę wykonanych ruchów, `classicLoad`, `adjustedLoad` i średnią intensywność ocenianych ruchów. Rekordy pojawiają się tylko wtedy, gdy mają podstawę w danych; kandydaci dla tego przepływu to największa liczba ruchów, największa praca na trudnych problemach i najwyższe RP. Dokładne nazwy oraz definicje rekordów trzeba ustalić przed rozszerzeniem DTO Dashboardu; nie wolno mieszać skal ani przedstawiać nieukończonej próby jako rekordu przejścia.

Propozycje angielskich etykiet, niezależne od istniejących kodów profili:

| Profil               | Proponowana etykieta          |
| -------------------- | ----------------------------- |
| Krótka droga baldowa | Short bouldery route -------- |
| Krótka droga ciągowa | Short sustained route ------- |
| Średnia ciągowa ---- | Medium-length sustained route |
| Średnia cruxowa ---- | Medium-length cruxy route --- |
| Długa ciągowa ------ | Long sustained route -------- |
| Długa cruxowa ------ | Long cruxy route ------------ |

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
