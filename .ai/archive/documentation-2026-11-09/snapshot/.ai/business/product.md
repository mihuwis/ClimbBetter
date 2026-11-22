# Produkt ClimbBetter

Status: ustalony kierunek produktu i zakres pierwszego etapu; 2026-10-03.

## Cel i odbiorcy

ClimbBetter pomaga wspinaczowi rejestrować sesje na ścianie i w skałach, rozumieć wykonane obciążenie oraz obserwować postęp. Docelowo wspiera planowanie i propozycje treningów. Odbiorcą pierwszej wersji jest osoba prowadząca własną historię wspinania, obecnie także w Excelu.

Pierwszy użyteczny rezultat: użytkownik zapisuje prawdziwy trening w webie i po ponownym otwarciu aplikacji widzi jego historię oraz wyniki policzone przez Javę. Sam formularz na mockach nie spełnia celu.

## Jak wyjaśniamy sesję użytkownikowi

Zasada biznesowa zatwierdzona 2026-10-03 (DEC-018): **jedna sesja dotyczy jednego rejonu wspinaczkowego albo jednej ścianki**. Uwzględniamy ją w objaśnieniach formularza, materiałach wprowadzających i dokumentacji dla użytkowników.

Proponowane objaśnienie:

> W jednej sesji zapisujesz wspinanie w jednym rejonie lub na jednej ściance. Możesz dodawać różne drogi, baldy i sektory w tym miejscu. Jeśli przenosisz się do innego rejonu lub na inną ściankę, zapisz tam osobną sesję — nawet tego samego dnia.

Przejście do innego sektora tego samego rejonu nie wymaga nowej sesji. Dzięki temu miejsce i podsumowanie sesji mają dla użytkownika jednoznaczne znaczenie. Szczegóły reguły: [domain](domain.md).

## Obszary produktu

| Obszar              | Rola                                                                                        |
| ------------------- | ------------------------------------------------------------------------------------------- |
| Dashboard --------- | ostatnie sesje i czytelne podsumowania; wejście do dodawania treningu --------------------- |
| Training / Calendar | rejestrowanie, historia, szczegóły sesji; dalej widoki dnia, tygodnia i cyklu ------------- |
| Areas / Catalog --- | własne miejsca, sektory, drogi, podpowiedzi z historii i szybkie dodanie brakujących danych |
| Analytics --------- | ruchy, Classic Load, główny Adjusted Load, poziom i profil stylów ------------------------- |
| Mobile ------------ | szybkie logowanie w terenie; docelowo trwałość lokalna i synchronizacja offline ----------- |
| Planning ---------- | przyszłe plany, cykle i generowanie propozycji treningowych ------------------------------- |

## Bieżący etap: web i Java

Ustalenie z 2026-09-22, potwierdzone priorytetem użytkownika 2026-10-03: zaczynamy od `Record session` w webie i odtworzenia backendu Java zgodnego z Dashboardem i Session Details.

- Wejście: Dashboard → `+ Add session` → wybór intencji → `Record session`.
- Sesja i nowe wpisy pozostają lokalnym szkicem do `Finish session`; `Start` nie zapisuje rekordu na serwerze.
- Backend waliduje i zapisuje ukończony trening atomowo, z kolejnością wpisów i snapshotami.
- Dashboard prezentuje `Adjusted Load`, ruchy i najwyższe ukończone wyceny osobno dla skal.
- Szczegóły grupują próby według tożsamości wspinaczki, zachowując chronologię i historyczne parametry każdej próby.

Szczegółowy przepływ: [training](training.md). Wymagania ekranów: [frontend-web](../documentation/frontend-web.md). Kontrakt do dopracowania: [api](../documentation/api.md).

## Granice pierwszego etapu

`Add finished session` i `Add single climb` są przewidzianymi intencjami; ich szczegóły nie są jeszcze zatwierdzone. Nie powstają trzy osobne modele treningu. Pełny katalog, upload zdjęć, przebudowa wszystkich sekcji Dashboardu, cele i społeczność nie są warunkiem pierwszego przepływu.

Szersze MVP ze starszych planów obejmuje Calendar, Areas i podstawową analitykę. Nie utożsamiamy go z pierwszym użytecznym przyrostem webowym ani nie przypisujemy mu terminu przed ustaleniem zakresu.

Mobile offline-first pozostaje kierunkiem, ale nie wyprzedza obecnie webu. Społeczność, komentarze, partnerzy i globalny katalog są późniejszymi możliwościami, nie rdzeniem MVP.

## Warunek sukcesu

Sesja zawierająca nieudaną próbę i następnie przejście tej samej drogi zapisuje się trwale bez duplikacji. Wyniki na Dashboardzie i w szczegółach pochodzą z backendu; historia wcześniejszej próby wpływa na kolejną. Wymagania odporności szkicu oraz szczegóły kontraktu domykamy przed implementacją odpowiednich części.

Źródła: [pierwszy etap web](../archive/source-snapshot/docs/CB_web_stage_1.md), [plan produktu](../archive/source-snapshot/app-plan.md), [wcześniejsze cele](../archive/source-snapshot/docs/old-doc/project_goals_02.md). Starsze kolejności prac są historyczne.
