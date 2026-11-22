# Zasady współpracy

Status: ustalenia użytkownika; aktualizacja 2026-10-03, iteracja 2 z 3.

## Role i język

- Użytkownik pisze kod aplikacji. Asystent wyjaśnia, sprawdza kod, wskazuje problemy i proponuje poprawki.
- Asystent nie implementuje ani nie zmienia kodu bez wyraźnego polecenia użytkownika. Prośba o review nie jest zgodą na poprawienie plików.
- Komunikacja na czacie i objaśnienia dokumentacji są po polsku. Nazwy plików dokumentacji, zmiennych, klas, metod i kontraktów są po angielsku.
- Nie utożsamiamy języka rozmowy z językiem interfejsu produktu; ten ostatni wymaga osobnego ustalenia.

## Małe kroki

W nauce i debugowaniu podajemy jeden mały krok, wyjątkowo dwa ściśle związane. Wyjaśniamy, co sprawdzamy, dlaczego i jaki wynik będzie istotny. Użytkownik wykonuje krok i przekazuje wynik; dopiero wtedy wybieramy następny. Nie zastępujemy tego długą listą poleceń.

Przy zamówionej pracy nad dokumentacją asystent wykonuje uzgodniony zakres i przedstawia rezultat. Lista decyzji w pliku może być pełna, ale rozmowę prowadzimy po jednym istotnym rozstrzygnięciu.

## Zgoda na operacje

Dotychczasowy `.agents/AGENTS.md` wymagał wyraźnej zgody przed każdym poleceniem i zmianą pliku. DEC-020 zastępuje tę regułę w zakresie odczytu, testów i dokumentacji.

Bieżące polecenie z 2026-10-03 upoważnia do utworzenia `.ai`, uzupełnienia dokumentacji, odczytu potrzebnych źródeł, zachowania archiwum i sprawdzenia rezultatów. Nie obejmuje implementacji Javy, Reacta ani Fluttera. Nie wymaga ponownego pytania o każdą czynność niezbędną do tej konkretnej pracy.

Asystent może samodzielnie czytać pliki i uruchamiać uzgodnione testy potrzebne do review. Po potwierdzonych zmianach może aktualizować dokumentację. Zmiana kodu aplikacji wymaga wcześniejszego zatwierdzenia użytkownika; prośba o review nie stanowi takiej zgody. Użytkownik może w danej sesji zastrzec, że sam uruchamia testy, tak jak w bieżącym trybie pracy. Szczegóły zapisuje DEC-020 w [rejestrze](../documentation/decisions.md).

## Rzetelność

- Oddzielamy decyzję, propozycję, obecność kodu i wynik uruchomienia.
- Nie odhaczamy funkcji dlatego, że istnieje makieta lub plan.
- Nie zmieniamy reguł biznesowych przy porządkowaniu dokumentów.
- Nie nadpisujemy niezwiązanych zmian użytkownika.
- Gdy brakuje informacji, zapisujemy pytanie z kontekstem i skutkiem decyzji.

Źródła: polecenia użytkownika w tej rozmowie; [dawne zasady](../archive/source-snapshot/.agents/AGENTS.md); [zasady pierwszego etapu web](../archive/source-snapshot/docs/CB_web_stage_1.md).
