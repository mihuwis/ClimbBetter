# Bieżące priorytety

Data aktualizacji: 2026-11-08. To krótka lista zakresu, nie zestaw poleceń do wykonania jednocześnie.

Lokalny przyrost Google/Firebase i profilu Mihu zakończony (DEC-052). Użytkownik potwierdził prawdziwe logowanie, nazwę i historię w UI. V24 działa na właściwej bazie, UID prowadzi do pierwotnego UUID002;17 aktywnych sesji/137 wpisów i cała historia pozostały bez zmian. Backend220/220, web build/lint/30 model/37 browser,4 testy auth i1 test rzeczywistego UI/API przeszły. Konta modelowe działają na osobnej bazie bez skrzynek. [Wynik i uruchomienie](../documentation/firebase-authentication-local.md). Hosting i ADMIN pozostają osobnymi zadaniami.

Zakres 2026-11-08: tani dev local + production dla 5–10 osób, bez QA/staging. Użytkownik akceptuje utrzymanie serwera; utrata danych przy awarii jest niedopuszczalna. [Environments plan](../documentation/Environments_plan.md) skrócony i ponownie porównany cenowo. Następnie wybór wariantu i budżetu (OPEN-21), potwierdzenie ochrony danych/prób awarii (OPEN-23) oraz zakresu ADMIN (OPEN-22). Bez implementacji i zakupu.

Training summary miesiąc/rok wdrożony w backendzie i Statistics. Backend 210/210, web build/lint/30 model/37 browser i realny test sześciu widoków UI/API przeszły. Niezależna kontrola 18 raportów na obecnych 17 sesjach/137 wpisach potwierdziła agregację i niezmienność danych. Nie potrzeba migracji ani replay. Następny krok: restart backendu8080 i ocena interfejsu przez użytkownika. [Raport](../documentation/period-training-summary.md).

Wdrożono model v3 poziomu odniesienia do Adjusted Load (DEC-047): 730 dni/zanik kwadratowy, bez wędki, maksimum dla mniej niż 10 wspinaczek. Pełny backend 176/176, frontend build/lint/24 model oraz realny browser CRUD v3 przeszły. Local V23 zachowała wszystkie stare wyniki. Po teście izolowanym i restarcie procesu 8080 ukończono właściwy chronologiczny replay: 16 aktywnych sesji/133 wpisy v3, z zachowanymi wejściami, wycenami, punktami i intensywnościami. Dashboard zwraca 16 sesji; wszystkie 12 widoków piramidy sprawdzone przez HTTP. Historia jest gotowa do oceny użytkownika w UI. [Reguły i stan](../documentation/recency-load.md).

Przed pracą nad ważeniem aktualności użytkownik zlecił eksport i usunięcie swoich sesji. Zabezpieczono pełne 35 sesji/278 wpisów/30 revisions oraz replay 16 aktywnych sesji/133 wpisów. Po testach izolowanych i odwracalnym usunięciu historii właściwy import do v3 został ukończony i zweryfikowany. Oryginalne 35 sesji/278 wpisów/46 revisions po archiwizacji, katalog i dane innych kont zachowały sumy kontrolne. Aktywna historia i piramidy zawierają odtworzone przejścia. [Kopia, narzędzia i wynik importu](../documentation/training-history-export.md).

Kolejne usprawnienie wdrożone: Repeat climb w formularzu kopiuje pojedynczy wpis, a Completed wymaga dokładnie pełnej długości (DEC-046, potwierdzenie użytkownika). Kopia zachowuje wspinaczkę i wartości, ma nowe ID oraz osobną edycję. Build/lint, 24 testy modelu, 31 browser i 136 backend przeszły. Backend użytkownika działa już z v3 po restarcie na 8080. [Opis](../documentation/frontend-web.md#repeat-climb-i-completed--2026-11-04).

Po ocenie Statistics użytkownik zamówił dwa usprawnienia sesji: automatyczny koniec +1 h od ręcznego startu i Repeat session z datą przed edycją kopii. Wdrożone; build/lint, 16 testów modelu i 26 przeglądarkowych przeszły. Pozostaje ocena tych dwóch zachowań przez użytkownika. [Opis](../documentation/frontend-web.md#ręczna-data-i-repeat-session--2026-11-04).

Najnowszy przyrost: frontend Statistics wdrożony, P10–P12 wykonane. Build/lint, 11 testów modelu i 22 przeglądarkowe przeszły; dodatkowy test potwierdził UI/API we wszystkich 12 kombinacjach na local V22 bez zmiany historii. Kolejny krok: ręczna ocena UI i pełniejszy odbiór P13. Statistics ma być miejscem kolejnych zestawów raportów miesięcznych i sezonowych; obecna piramida używa dwóch uzgodnionych okresów. [Raport](../documentation/statistics-frontend.md).

Backend piramidy i baza wdrożone na polecenie użytkownika 2026-11-03. Raport obsługuje 12 kombinacji, deduplikację i snapshoty, estymację ważoną oraz paginację. Wędka jest wykluczona z obecnych i historycznych osiągnięć przy zachowaniu treningu/loadu. 120 testów przeszło, local na V22, HTTP i sumy kontrolne historii potwierdzone. Frontend wdrożony następnego dnia według [kontraktu backendu](../documentation/achievement-pyramid-backend.md). Integracja poziomu z loadem i reszta OPEN-20 pozostają osobnym etapem.

Zakończony zakres implementacji v2 z 2026-11-01: backend, baza i frontend. Backend: nowe obliczenia, crux ceil(15% wykonanych), snapshoty, API/audyt, migracje V16–V20 i poprawka poziomu dla pustej historii; pełny zestaw 74 testów przeszedł. Migracje local zachowały wyniki 19 sesji/145 wpisów. Frontend: bez EDL, Details z intensywnością i cruxem, aktywne profile oraz naprawa szkiców, dokładne retry. Build/lint, 6 testów modelu i 13 przeglądarkowych przeszły. Pozostaje wspólny test UI z rzeczywistym backendem V20. [Raport frontendu](../documentation/frontend-web.md#plan-dostosowania-do-modelu-v2--2026-11-01), [model](../business/grading-and-scoring.md#zatwierdzona-zmiana-v2--edl-zastępujemy-przez-profileintensityindex), [weryfikacja backendu](../documentation/backend-verification.md).

Poprzedni zakres z 2026-10-17: ocena wdrożonych punktów React 1–5 — Dashboard, Details z edycją/usuwaniem, Calendar, ręczne dodanie i Start now. Build/lint, 4 testy modelu, 6 scenariuszy przeglądarkowych i pełny CRUD przez realny backend przeszły; backend miał 52 zielone testy. Ocena interfejsu i poprawki ergonomii pozostają do wykonania. [Raport React](../documentation/frontend-web.md), [kontrakt API](../documentation/backend-crud-handoff.md).

## TO DO NOW

- [x] Sprawdzić przygotowanie backendu, Reacta, bazy i narzędzi historii oraz zapisać plan Firebase Auth na lokalny dev (DEC-050).
- [x] Rozstrzygnąć zakres profilu: użytkownik wybrał nazwę i potwierdzenie, ankieta później (DEC-051, OPEN-26 zamknięte).
- [x] Wdrożyć kod backend/frontend auth i profilu, przygotować V24, sprawdzić backup/restore i konta modelowe oraz wykonać testy (DEC-052).
- [x] Dokończyć odbiór Mihu: prawdziwe Google Sign-In, mapowanie do UUID002, potwierdzony profil i historia w UI oraz końcowe porównanie danych. [Wynik](../documentation/firebase-authentication-local.md).

- [x] Skrócić Environments plan do dev local + production, porównać tanie VPS/Neon i opisać ochronę potwierdzonych zapisów przed awarią jednego serwera (DEC-049, aktualizacja 2026-11-08).
- [ ] Wybrać hosting/koszt (OPEN-21) oraz mechanizm ochrony danych i dopuszczalną przerwę (OPEN-23), następnie ustalić zakres ADMIN (OPEN-22). Warunki pierwszego udostępnienia są w planie; implementacja wymaga zlecenia.

- [x] Zapisać potwierdzone zasady czasu sesji mieszanych i unikalnych udanych stylów w raporcie okresowym (DEC-048).
- [x] Wdrożyć raport Training summary miesiąc/rok: backend agregacji, web i Recharts, testy oraz odczyt rzeczywistej historii. [Weryfikacja](../documentation/period-training-summary.md).
- [ ] Zrestartować backend8080 do nowego endpointu i zebrać ocenę raportu miesiąca/roku w UI.

- [x] Ustalić unikalność, okna historii, kwalifikację wędki i widoki RP/OS piramidy (DEC-045).
- [x] Wyjaśnić i potwierdzić metodę Pyramid Support: użytkownik zachował model ważony i odrzucił pełne warstwy 2026-11-03.
- [x] Zapisać pełny plan backendu i frontendu piramidy, P01–P13 oraz osobny etap integracji z loadem, na prośbę użytkownika 2026-11-03. [Plan](../documentation/achievement-pyramid-implementation-plan.md).
- [x] Wdrożyć backend/DB Statistics i spójną kwalifikację osiągnięć: 120 testów, V22 local i kontrola historii.
- [x] Wdrożyć frontend Statistics P10–P12: filtry, wykres/listy i testy, również odczyt prawdziwego API.
- [ ] Wykonać ręczną ocenę UI i pełny odbiór P13 na zestawie danych z dodaniem/edycją/usunięciem sesji v2.
- [ ] Rozwijać następne zestawy Statistics dla dłuższych okresów; przed okresem sezonowym ustalić jego granice i rozszerzyć kontrakt zakresów.
- [ ] Rozstrzygnąć pozostałe OPEN-20 przed podłączeniem estymatora do poziomu/loadu; raport może powstać niezależnie.

- [x] Zapisać DEC-043, nowe wzory i przykład obliczeń z wagami profili 10/7/5; oznaczyć model v2 jako zatwierdzony, do wdrożenia.
- [x] Przygotować V16 i potwierdzić migrację oraz regresję v1 przez `TrainingSessionFlowIntegrationTest` na PostgreSQL Testcontainers (użytkownik, 2026-11-01).
- [x] Wdrożyć backend i bazę v2, crux, snapshoty, audyt oraz migracje local; pełny zestaw 74 testów przeszedł.
- [x] Dostosować frontend do kontraktów i wyników v2; build/lint, testy modelu 6/6 i przeglądarkowe 13/13.
- [ ] Uruchomić UI z backendem V20, wspólnie dodać sesję i sprawdzić zapisane wyniki oraz edycję.

Katalog skalny: V15 potwierdzona również na pustym PostgreSQL Testcontainers; testy sektorów, metrów i aliasów wycen przeszły w pełnym zestawie. Publiczne 28 pozycji Filaru Abazego pozostaje w local. [Raport](../documentation/outdoor-catalog.md).

Scenariusze A/B są potwierdzone przez zielony `CatalogClimbScenariosIntegrationTest`: nowe i znane Climb w sesji oraz prywatne Climb zapisane przed treningiem. [Kontrakt i przykład Swagger](../documentation/catalog-climb-scenarios.md).

- [ ] Dokończyć porządkowanie dokumentacji w trzech iteracjach: pierwsza zweryfikowana, druga w toku po zapisaniu DEC-018–022; pozostałe tematy są w [rejestrze decyzji](../documentation/decisions.md).
- [x] Podłączyć Dashboard, Details, edycję/usuwanie, kalendarz i oba formularze sesji do API; potwierdzić trwały lokalny szkic i rzeczywisty zapis/odczyt przez UI.
- [x] Dashboard z Adjusted Load i najwyższymi ukończonymi wycenami per skala; Details grupowane po wspinaczce z zachowaniem chronologii, ruchów i obu loadów.
- [ ] Zebrać uwagi użytkownika do pięciu ekranów i poprawić ergonomię. Szczegóły testów i ograniczenia: [frontend-web](../documentation/frontend-web.md).

Ustalone: jedna sesja obejmuje jedno Area (DEC-018); `.ai` jest wersjonowane w Git (DEC-019); asystent może czytać i wykonywać uzgodnione testy, lecz nie zmienia kodu bez zatwierdzenia (DEC-020); lokalny odzyskiwalny szkic jest wymagany (DEC-021); pierwszy Dashboard używa tekstowych metryk bez dekoracyjnego wykresu (DEC-022).

Pierwszy backendowy pion zapisu i odczytu jest potwierdzony: `POST /api/v1/training/sessions → PostgreSQL → Dashboard/Personal Bests/Details`. Test zapisuje sesję Bronx, odczytuje podsumowanie, średnią intensywność, wyniki życiowe i trzy chronologiczne wpisy. Potwierdzono również atomowy rollback, bezpieczne identyczne ponowienie, konflikt `409` dla zmienionego żądania i `404` Details.

Wcześniejsza kolejność z 2026-10-11 (zastąpiona powyższym zakresem z 2026-10-17):

1. następny dzień rozpocząć od Docker Desktop, `docker version` i celowanego `./mvnw -Dtest=TrainingSessionFlowIntegrationTest test`;
2. domknąć weryfikację `ASCENT_MODE_HISTORY_CONFLICT`: 2026-10-17 użytkownik potwierdził test OS, rollback i ponowienie z jawnym potwierdzeniem oraz flagami audytu; pozostają asercja mnożnika `1.50` i wariant Flash;
3. dodać małe endpointy wyboru istniejących Area i Climb, zachowując możliwość szybkiego utworzenia nowych w POST sesji;
4. uzgodnić i rozpocząć edycję całej sesji z ponownym obliczeniem oraz audytem; edycja pojedynczego wpisu w UI korzysta z tego samego kontraktu agregatu;
5. domknąć ochronę równoległego retry i scenariusz sesji zawierającej wyłącznie próby;
6. rozszerzyć personal bests o najlepsze wyniki z ruchomego okna ostatnich 12 miesięcy.

Ustalono: recent personal best używa 12 miesięcy; Area i Climb zachowują UUID, a nazwa służy do wyszukiwania; edycja nie przelicza automatycznie późniejszych sesji; konflikt OS/Flash używa `409 ASCENT_MODE_HISTORY_CONFLICT`. Przed katalogiem pozostaje ustalenie minimalnego kształtu wyników wyszukiwania oraz obsługi prywatnych i publicznych pozycji.

## TO DO LATER

- Pozostałe intencje dodawania sesji i edycja z audytem historii.
- Podstawowa analityka; rozbudowa już podłączonych Calendar i Areas według uwag użytkownika.
- Integracja Fluttera, trwałość lokalna i synchronizacja offline.
- Planowanie, cykle i generowanie propozycji treningowych.
- Rozszerzenie katalogu, oficjalne pakiety i zdjęcia.
- Logowanie produkcyjne, wdrożenie i utrzymanie przed publicznym udostępnieniem.
- Społeczność i zaawansowana analityka po ustabilizowaniu rdzenia.

Warunki ukończenia przyrostów: [roadmap](roadmap.md). Faktyczny stan: [progress](progress.md).



## Start dnia pracy 

Dzisiaj pracujemy nad backendem Java, mam około 5 godziny. Stwr zplan działań na dzisiaj w oparciu o poprzednie ustalenia.
 Przeczytaj AGENTS.md, .ai\documentation\decisions.md, priorytety, postęp i potrzebne dokumenty oraz sprawdź obecny kod. Przypomnij mi o uruchomieniu Docker Desktop i sprawdzeniu `docker version` przed testami integracyjnymi: Testcontainers uruchamia PostgreSQL w kontenerze. Ja koduję i uruchamiam testy. Ty wyjaśniasz, robisz review i aktualizujesz dokumentację po potwierdzonych zmianach. Prowadź mnie po jednym małym kroku. Pamiętaj, że uczę się Javy i Reacta i nie znam jeszcze wszystkich możliwości tych języków. Pomagaj.


## Koniec dnia pracy

Sesja z 2026-10-10 zakończona po około 5 godzinach łącznej pracy. Potwierdzono model obliczeń i agregacji, rozgrzewkę, `V5` oraz integracyjny odczyt po migracji. Następna sesja zaczyna się od brakującego `TrainingEntryCommand`, a celem jest zapis scenariusza Bronx i odczyt jego podsumowania na Dashboardzie. Szczegóły oraz granice potwierdzenia zapisano w [work-log](work-log.md).

Sesja z 2026-10-11 zakończona po około 7 godzinach. Ukończono backendowy zapis scenariusza Bronx, migracje do `V12`, rollback i idempotencję z fingerprintem, średnią intensywność, personal bests oraz Details. Pełny zestaw testów przeszedł. Następny dzień zaczyna się od konfliktu OS/Flash, a następnie przechodzi do katalogu Area/Climb potrzebnego formularzowi webowemu. Szczegóły zapisano w [work-log](work-log.md).
