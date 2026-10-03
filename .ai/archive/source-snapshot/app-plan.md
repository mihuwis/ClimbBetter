# ClimbBetter - app plan

Status: roboczy plan rozwoju aplikacji.

## Cel produktu

ClimbBetter ma byc aplikacja do logowania treningu wspinaczkowego, analizy progresji i planowania obciazen. Nie traktujemy jej tylko jako logbooka. Rdzeniem produktu sa trening, kalendarz, obciazenie, progres i szybkie zapisywanie sesji na sciance oraz w skalach.

## Glowne moduly

- Dashboard: szybki obraz ostatniej aktywnosci, podsumowanie profilu, cele, feed.
- Training / Calendar: miesiac, tydzien, dzien i docelowo cykle treningowe.
- Areas: moje miejsca wspinania, oficjalne rejony i wlasne miejsca uzytkownika.
- Session logging: szybkie dodawanie sesji, drog, prob, jakosci przejscia i notatek.
- Analytics: punkty, obciazenie, streaki, progresja i porownania okresow.
- Mobile offline first: logowanie w terenie bez internetu i pozniejsza synchronizacja.
- Community: przyszly dodatek, nie glowny cel MVP.

## MVP

- Stabilny model treningu: sesja, wpis treningowy, droga/boulder, trudnosc, jakosc, area.
- Dashboard oparty o realne dane z API.
- Calendar/Training z widokiem miesiaca i szczegolami dnia/sesji.
- Areas z lista miejsc, drog i podstawowym filtrowaniem.
- Tworzenie sesji oraz dodawanie wpisow treningowych.
- Podstawowe metryki: liczba sesji, suma punktow, rozklad trudnosci, ostatnie aktywnosci.

## Status mobile Flutter

- Projekt Flutter zostal utworzony w `frontend-mobile`.
- Aktualny package/app name: `climbbetter_mobile`.
- Platformy w projekcie: Android i iOS.
- Pierwszy release target: Android.
- Na tym etapie testujemy bez telefonu przez `flutter analyze` i `flutter test`.
- `flutter doctor -v`: Flutter/Dart sa gotowe, Java 17 jest dostepna, brakuje Android SDK.
- Refaktor mobile zostal wykonany: `main.dart` jest punktem startowym, a kod UI jest podzielony na `app`, `features` i `shared`.
- Pierwszy ekran mobile obejmuje:
  - Board z ostatnimi treningami.
  - Nowy trening.
  - Ty z metrykami, kalendarzem i wykresem obciazenia.
  - Gorny pasek z awatarem profilu i ustawieniami.
  - Menu `...` na treningu z akcja `Ponow trening`.

## Zasady domenowe

- Historia treningowa nie moze znikac po zmianie lub archiwizacji drogi.
- Stare sesje moga byc edytowane.
- Wpis treningowy powinien przechowywac snapshot istotnych danych.
- Usuwanie drog i rejonow powinno byc zastapione archiwizacja.
- SessionDate jest lokalna data treningowa, ale sesja musi miec tez TimeZoneId, zeby kalendarz nie przesuwal wpisow po zmianie kraju/strefy.
- Mobile projektujemy jako offline first.

## Kolejnosc rozwoju

1. Domknac podstawowe API treningu i list slownikowych.
2. Podlaczyc web do realnych endpointow zamiast mockow.
3. Rozbudowac Calendar/Training o szczegoly sesji i metryki.
4. Rozbudowac Areas o wlasne miejsca, ulubione drogi i projekty.
5. Zaprojektowac kontrakt synchronizacji dla mobile.
6. Zainstalowac Android SDK/Android Studio i zaakceptowac licencje SDK.
7. Rozbudowac mobile o szczegoly treningu i roboczy formularz dodawania sesji.
8. Rozwinac szkielet Flutter o lokalny model danych i offline repository.
9. Przygotowac pierwszy kontrakt synchronizacji mobile-backend.
10. Dodac analityke cykli treningowych i obciazenia.

## Pytania otwarte

- Jak liczymy punkty i obciazenie dla roznych typow wspinania?
- Jak wyglada model uzytkownika, autoryzacja i prywatnosc danych?
- Ktore dane musza byc dostepne offline w pierwszej wersji mobile?
- Jak rozdzielamy official areas od user areas w API i UI?
