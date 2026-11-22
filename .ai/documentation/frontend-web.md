# Frontend web

Aktualizacja: 2026-11-09. React/TypeScript/Vite korzysta z rzeczywistego API; angielski UI. Źródła: `frontend-web/src`, [wyniki weryfikacji](backend-verification.md).

## Ekrany i funkcje

| Adres                                                      | Działanie                                                                        |
| ---------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `/login`, `/profile/setup`                                 | Google/Firebase, nazwa profilu i potwierdzenie                                   |
| `/`                                                        | Dashboard z sesjami, ruchami, loadem, intensywnością i maksimami osobno dla skal |
| `/calendar`                                                | historia z zakresu dat i wejście w sesję                                         |
| `/areas`                                                   | miejsca/wspinaczki oraz niezależne zapamiętanie prywatnego Climb                 |
| `/sessions/new`, `/sessions/record`                        | ręczna sesja albo Start now z lokalnym szkicem                                   |
| `/sessions/:sessionId`                                     | Details, grupowanie po `climbId`, edycja, usunięcie, Repeat session              |
| `/sessions/:sessionId/edit`, `/sessions/:sessionId/repeat` | korekta istniejącej sesji albo nowa kopia z datą                                 |
| `/statistics`                                              | Achievement pyramid; `?report=summary` wybiera Training summary                  |

Statistics zachowuje filtry i wybór w URL oraz powrót ze źródłowej sesji. [Piramida](achievement-pyramid-backend.md) ma 12 kombinacji i listy warstw; [Training summary](period-training-summary.md) ma miesiąc/rok, wykres pracy, pierścienie i wyróżnienia. Recharts jest ładowany dopiero z raportem okresowym.

## Szkic i formularz

Struktura: `app`, `pages`, domenowe `features`, `shared`. Wspólny klient fetch i `useResource`; brak Redux/TanStack Query. `localStorage` zachowuje szkice według UUID użytkownika, trybu i edytowanej/powtarzanej sesji. Inna karta może wymusić przeładowanie po wykryciu konfliktu; nie ma atomowej blokady między kartami.

Zapis zachowuje dokładną treść do retry. Błąd/niepewny wynik nie kasują szkicu. PUT używa ostatniej wersji; konflikt nie nadpisuje serwera. Zmiana konta anuluje żądania i odrzuca spóźnione odpowiedzi; pojedyncze odświeżenie tokenu przy 401 pozostaje w tym samym koncie. [Szczegóły auth](firebase-authentication-local.md).

Formularz używa aktywnych profili, pokazuje intensywność i crux w Details, rozróżnia historyczne wersje. EDL usunięto z nowych wyborów. Stary szkic z wycofanym profilem wymaga jawnej korekty bez utraty wpisów. Completed wymaga równości ruchów. Repeat climb kopiuje wpis z nowym ID; Repeat session tworzy nowy szkic z datą. Reguły podpowiedzi i czasu: [training](../business/training.md).

Nie ma serwerowego preview ani automatycznej reewaluacji historii. [Uruchomienie i polecenia testów](development.md).
