# Rejestrowanie treningu

Aktualizacja: 2026-11-09. Działające przepływy web; szczegóły techniczne w [frontend-web](../documentation/frontend-web.md).

## Zapis i trwałość

1. Wybierz miejsce całej sesji oraz tryb: ukończona sesja z datą albo Start now.
2. Dodaj rozgrzewkę, próby i przejścia. Każda próba ma własne ID i kolejność; istniejący Climb zachowuje UUID, nowy — wspólne `clientClimbId` dla kolejnych prób.
3. W Start now zegar działa od Start do Stop, bez pauzy; odpoczynki należą do czasu. Stop utrwala koniec lokalnie. Dopiero Finish wysyła cały trening.
4. Backend rozwiązuje katalog i historię, oblicza wpisy oraz zapisuje całość w jednej transakcji. Dashboard i Details pokazują utrwalony wynik.

Start i dodawanie wpisów nie zapisują sesji w backendzie. Szkic w przeglądarce jest przypisany do UUID użytkownika i trybu/sesji; przeżywa odświeżenie i błąd zapisu. Znika po potwierdzonym sukcesie albo jawnym odrzuceniu. Retry zachowuje dokładny request i ID. To lokalna trwałość, bez synchronizacji szkiców między urządzeniami.

## Podpowiedzi i powtarzanie

- Nowy bald: edytowalna sugestia 8 ruchów; nowy ukończony bald bez znanej historii — sugestia OS. Znajomość sprzed aplikacji można wskazać jako RP.
- Katalog dostarcza wycenę, profil i sugerowaną długość. Ręczna liczba ruchów ma pierwszeństwo przed sugestią `ceil(lengthMeters × 2)`. Wybór miejsca ogranicza wspinaczki; identyczne nazwy nie łączą ich historii.
- Repeat climb kopiuje pojedynczy wpis z nowym ID i osobną edycją; wartości pozostają do świadomego przeglądu. Completed wymaga dokładnie pełnej liczby ruchów.
- Repeat session pyta najpierw o datę, tworzy nowy szkic w tym samym miejscu i nowe ID sesji/wpisów. Zachowuje wspinaczki, lecz nie dawne wyniki i potwierdzenia konfliktów; powtarzane OS/Flash proponuje jako RP.
- Ręczna zmiana początku podpowiada koniec +1 h, nadal edytowalny. Nie zastępuje to rzeczywistego pomiaru w Start now.

## Odczyt i dalszy rozwój

Details grupuje płaską listę wpisów po `climbId`, zachowując chronologię. Edycja wymaga wersji i powodu, usunięcie jest logiczne. Zmiana sesji nie zmienia automatycznie katalogu ani późniejszych snapshotów.

Przykład kontrolny: rozgrzewka → nieudana próba nowej wspinaczki → przejście tej samej wspinaczki → zapis → odczyt → identyczny retry. Przejście widzi wcześniejszą próbę; osiągnięcia i load mają różne zasady kwalifikacji.

Podgląd obliczeń przed zapisem, pojedyncza wspinaczka jako odrębna intencja, gęstość treningu, sezon, plany i generowanie propozycji pozostają dalszym zakresem. Nierozstrzygnięte reguły są wyłącznie w [rejestrze decyzji](../documentation/decisions.md).
