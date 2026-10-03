# Frontend web

Status: prototyp React na mockach; odczyt kodu 2026-10-03, bez uruchamiania builda.

## Co istnieje

[package.json](../../frontend-web/package.json) deklaruje React `^19.2.6`, React Router `^7.16.0`, TypeScript `~6.0.2` i Vite `^8.0.12`. To zakresy z repozytorium, nie rekomendacja aktualizacji ani raport z uruchomienia.

Routing w [App.tsx](../../frontend-web/src/App.tsx): `/`, `/calendar`, `/areas`, `/sessions/:sessionId`. Strony znajdują się w `src/pages`; elementy funkcji w `src/features`, typy i mocki w `src/shared`.

| Element            | Stan potwierdzony w plikach                                                 |
| ------------------ | --------------------------------------------------------------------------- |
| Dashboard -------- | importuje `activityFeedMock`, `profileSummaryMock`, `disciplineSummaryMock` |
| ActivityCard ----- | pokazuje `score`; pięć słupków ma stałe wysokości ------------------------- |
| SessionDetailsPage | odczytuje mock, sam sumuje `points` i ruchy, pokazuje płaską listę wpisów   |
| Typ SessionDetails | rozszerza `ActivityFeedItem` w `shared/types/dashboard.types.ts` ---------- |
| Calendar --------- | maj 2026 ustawiony na stałe, agreguje `score` z mocków -------------------- |
| Areas ------------ | dane `myAreasMock` -------------------------------------------------------- |
| Narzędzia -------- | skrypty `dev`, `build`, `lint`, `preview`; brak skryptu testowego --------- |

Nie znaleziono warstwy HTTP ani wdrożonego `Record session`. Stan UI nie potwierdza trwałego zapisu.

## Docelowy podział odpowiedzialności

Propozycja z wcześniejszego planu, jeszcze niewdrożona:

| Moduł       | Odpowiedzialność                                                    |
| ----------- | ------------------------------------------------------------------- |
| `training`  | sesje, wpisy, kalendarz, formularze, szczegóły, własny model danych |
| `catalog`   | miejsca, sektory, wspinaczki, selektory i podpowiedzi ------------- |
| `dashboard` | złożenie kart i podsumowań, własne modele prezentacji ------------- |
| `app`       | routing, layout, providery ---------------------------------------- |
| `shared`    | uniwersalne UI, klient HTTP i formatowanie ------------------------ |

Model sesji nie powinien dziedziczyć po karcie Dashboardu. DTO mapujemy do modeli widoku w jednym miejscu. Cache API i odzyskiwalny szkic treningu mają odrębne zadania. Własność grupowania po stronie API/web należy doprecyzować w `OPEN-06`.

TanStack Query, Vitest, React Testing Library i MSW są propozycjami starszego planu, nie obecnymi zależnościami ani potwierdzonym wyborem na tę iterację (`OPEN-17`).

## Warunki integracji

Każdy podłączony widok obsługuje loading, empty, error i właściwe not found. `sessionDate` nie jest przesuwana strefą przeglądarki. Wyniki i summary pochodzą z backendu. Mocki mogą służyć jako fixtures, ale zintegrowany przepływ produkcyjny nie powinien ich importować. Testy porównają próbę i przejście tej samej drogi, powtórzony zapis, miejsca o tych samych nazwach oraz brak ukończonej wyceny.

Pełny przepływ rejestrowania i szkicu: [training](../business/training.md). Dalej przeniesiono sekcje Dashboardu, Details i granic rozszerzeń z [ustaleń web](../archive/source-snapshot/docs/CB_web_stage_1.md), zachowując ich status. Starszy [plan web](../archive/source-snapshot/docs/CB_Front-web.md) zachowuje dalsze checklisty; nie wyznacza kolejności obecnego etapu.

## Dashboard

- `+ Add session` uruchamia wybór intencji.
- `Adjusted Load` zastępuje `Score` na karcie.
- `Moves` pozostaje. `Max grade` powinno opisywać najwyższą ukończoną wycenę; przy samych próbach brak ukończonej wyceny. W sesji mieszanej skale balda i drogi pokazujemy oddzielnie.
- Pozostałe sekcje pozostają na tym etapie bez przebudowy.

Obecne słupki w `ActivityCard` mają stałe wysokości, więc nie reprezentują danych i należy je usunąć przy integracji prawdziwego Dashboardu. Zgodnie z DEC-022 pierwszy przyrost pokazuje tekstowe metryki z rzeczywistych danych, bez wykresu zastępczego. Rozkład prób/przejść według wycen wymaga osobnego zatwierdzenia; jeśli powstanie, skale balda i drogi pozostają oddzielne.

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
