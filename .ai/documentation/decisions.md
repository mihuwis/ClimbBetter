# Decyzje i otwarte ustalenia

Aktualizacja: 2026-11-15. Zachowane ID; skrót obowiązujących ustaleń. Stan wdrożenia opisuje [progress](../project-management/progress.md). Pełne uzasadnienia i kolejne wersje: [rejestr sprzed skrócenia](../archive/documentation-2026-11-09/snapshot/.ai/documentation/decisions.md). Propozycja asystenta nie jest decyzją użytkownika.

## Bieżące decyzje

### DEC-043 — intensywność profilu

2026-11-01, wdrożone: EDL zastąpione `profileIntensityIndex`, aby długość nie obniżała intensywności. Startowe wagi 10/7/5, CIRCUIT=7; krótka ciągowa wycofana. V16–V20, zachowany odczyt starej historii. Nowy zapis/edycja używają obecnie v3, które zachowuje tę intensywność. [Wzory](../business/grading-and-scoring.md#intensywność-i-load).

### DEC-044 — crux

2026-11-01, wdrożone: ceil(15% wykonanych ruchów) trudnych, reszta łatwiejsza; trudności ±pełny stopień od bazy. Użytkownik zaakceptował proporcje Font i niższy średni load cruxa. Wybór bazy Font i przesunięcia po słowniku są opisanym przybliżeniem implementacyjnym, zamrożonym w snapshotach. [Crux](../business/grading-and-scoring.md#crux).

### DEC-045 — piramida i wędka

2026-11-01, wdrożone backend/web: jedno climbId raz po filtrach, snapshot pierwszego kwalifikującego przejścia, all-time/2 lata, RP obejmuje OS/Flash/RP, OS tylko OS. TOP_ROPE nigdy nie jest osiągnięciem, pozostaje treningiem. 2026-11-03 użytkownik podtrzymał ważony Pyramid Support i odrzucił pełne warstwy. [Reguły](../business/achievement-pyramid.md).

### DEC-046 — Completed

2026-11-04, wdrożone: ASCENT wymaga dokładnie `executedMoves == totalMoves`, ATTEMPT zachowuje powtórzenia. Zawęża DEC-024 dla ukończeń; nie zmienia odczytu dawnej historii ani identycznego retry.

### DEC-047 — aktualność poziomu v3

2026-11-04, wdrożone: kwadratowy zanik przez 730 dni, dowody bez TOP_ROPE, maksimum bez ważenia dla 1–9 różnych wspinaczek. Wybór implementacyjny od 10: najwyższa jednostka masy dowodów, fallback bieżącej sesji, interpolacja mnożnika. Wycen/punktów nie postarzamy; Pyramid Support pozostaje osobny. [Model](../business/grading-and-scoring.md#poziom-odniesienia-v3).

### DEC-048 — podsumowanie okresowe

Potwierdzone 2026-11-04, wdrożone 2026-11-07: pełny czas sesji zawierających dyscyplinę, bez sztucznego podziału. Koło stylów: jedno climbId w okresie, najlepszy udany OS > Flash > RP, bez prób/wędki. [Raport i agregacja](period-training-summary.md).

### DEC-049 — środowiska

Aktualizacja użytkownika 2026-11-08 zastępuje wcześniejszy plan 3 środowisk: dev local + płatna produkcja, bez QA/staging, pierwsza grupa 5–10 osób. Samodzielne utrzymanie zaakceptowane; awaria serwera nie może oznaczać utraty danych. Docelowo rozważano ok. 100 użytkowników w pierwszym roku i do ok. 10 tys., bez benchmarku ani gwarancji skalowania. Dostawca, koszt i mechanizm ochrony niezatwierdzone. Dwa VPS/replikacja oraz GitHub Actions/GHCR są propozycją wykonawczą. [Plan](Environments_plan.md).

### DEC-050 — plan Firebase local

2026-11-08: zlecony przegląd i plan Google/Firebase, mapowania istniejącego Mihu oraz profilu. Zastąpiony operacyjnie ukończonym wdrożeniem DEC-052; dopuszczony awaryjny reset/replay nie był potrzebny.

### DEC-051 — podstawowy profil

2026-11-08: nazwa i jej potwierdzenie z serwerowym stanem ukończenia. Ankieta później, bez dodatkowych danych fizycznych i zmian loadu; zamknięte OPEN-26. Wdrożone.

### DEC-052 — wdrożenie Firebase i modeli

2026-11-08: użytkownik zlecił implementację, wskazał MiniJirę jako referencję i utworzył `climbbetter-dev`. Wdrożono Google, profil, ochronę API/web i V24. Powiązano prawdziwe konto z dotychczasowym UUID Mihu, użytkownik potwierdził profil/historię, dane zachowano bez replay. Konta modelowe mają osobny emulator i bazę, bez nowych skrzynek. ADMIN poza zakresem. [Wynik i uruchomienie](firebase-authentication-local.md).

### DEC-053 — sesje użytkownika bez nakładania czasu

2026-11-15, zlecone i wdrożone: aktywne sesje tego samego użytkownika nie mogą nakładać się w czasie, niezależnie od miejsca treningu. Reguła obejmuje dodawanie i edycję, również przy jednoczesnych żądaniach. Przedział obejmuje początek i pomija koniec (`[startedAt, endedAt)`), więc następna sesja może zaczynać się dokładnie z końcem poprzedniej. Usunięta sesja zwalnia przedział, a różni użytkownicy mogą trenować w tych samych godzinach.

[V25](../../backend/src/main/resources/db/migration/V25__prevent_overlapping_training_sessions.sql) wymaga obu granic czasu i blokuje kolizje w PostgreSQL. Backend porządkuje zapisy użytkownika blokadą transakcyjną; konflikt daje `409 SESSION_TIME_CONFLICT`. Odrzucona edycja zachowuje poprzedni stan, wersję i audyt, a formularz zachowuje szkic do poprawy. [Szczegóły bazy](database.md), [wyniki weryfikacji](backend-verification.md).

Odbiór automatyczny: 242 testy backendu, w tym 19 scenariuszy tej reguły, oraz 15 testów formularza w przeglądarce; build i lint frontendu poprawne. Lokalnie zastosowano V25 po wykonaniu prywatnej kopii bazy. Sumy kontrolne sesji, wpisów, użytkowników, miejsc i wspinaczek pozostały zgodne.

## Wcześniejsze decyzje

DEC-001–017 zachowują źródła z [pierwotnego rejestru](../archive/source-snapshot/docs/CB_project_decisions.md). Nowsze decyzje zastępują tylko wskazany zakres.

| ID      | Obowiązujący sens / zastąpienie                                                                                                                      |
| ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| DEC-001 | Oddzielne skale Font/French; obwód używa French. Indeksy lokalne, wspólne punkty według DEC-029.                                                     |
| DEC-002 | Konfiguracyjne bandy względnej trudności, floor 0,20 i cap 5,00; rzeczywista różnica zachowana.                                                      |
| DEC-003 | Adjusted Load główną metryką, Classic Load porównawczą; zachowujemy obie.                                                                            |
| DEC-004 | Średnia intensywność ważona ruchami ocenianych wpisów; bez rozgrzewki, pusty mianownik → NULL.                                                       |
| DEC-005 | BigDecimal/numeric, HALF_UP i kontrolowana precyzja; szczegóły w aktualnym modelu.                                                                   |
| DEC-006 | Poziom z historii i Pyramid Support. Raport wdrożony; odniesienie do loadu zastąpione DEC-047.                                                       |
| DEC-007 | Rezultat, tryb i znajomość osobno; kontakty z 2 lat, Fast RP z całej znanej historii.                                                                |
| DEC-008 | OS/Flash sprzeczne z historią wymagają ostrzeżenia, potwierdzenia i audytu; mnożnik według DEC-031.                                                  |
| DEC-009 | Area → opcjonalny Sector → Climb, bez generycznego drzewa geograficznego.                                                                            |
| DEC-010 | Brak katalogu nie blokuje logowania treningu: prywatne obiekty i stabilne client IDs.                                                                |
| DEC-011 | Jawna korekta zachowuje stan przed/po i audyt; zmiana słownika nie przepisuje historii.                                                              |
| DEC-012 | Planowanie i periodyzacja odłożone do osobnego modułu.                                                                                               |
| DEC-013 | Jawne nazwy metryk; `edlCount` pozostało historyczne po DEC-043.                                                                                     |
| DEC-014 | Lokalna data, strefa IANA i czas sesji; podróż nie zmienia historycznego dnia.                                                                       |
| DEC-015 | Nazwa sesji opcjonalna: wygenerowana albo użytkownika; zakres faktycznie zapisywanych pól wynika z kodu.                                             |
| DEC-016 | Quick location jako prywatny Area DRAFT; przyszłe uzupełnianie/scalanie ma zachować historię.                                                        |
| DEC-017 | Ogólny poziom osobno na dyscyplinę; profil OS/Flash/Fast RP/RP Max jest osobną informacją.                                                           |
| DEC-018 | Jedna sesja = jedno Area, wiele sektorów dozwolone. Inne miejsce oznacza osobną sesję (2026-10-03).                                                  |
| DEC-019 | Aktywna `.ai` i archiwum wersjonowane w Git; stare `docs` pozostają historyczne, poza Git (2026-10-03).                                              |
| DEC-020 | Samodzielny odczyt, uzgodnione testy i dokumentacja dozwolone; zmiana kodu wymaga wyraźnego zlecenia. Bieżące polecenie określa zakres (2026-10-03). |
| DEC-021 | Szkic przetrwa odświeżenie i niepotwierdzony zapis; usuwany po sukcesie lub jawnym odrzuceniu (2026-10-03).                                          |
| DEC-022 | Dashboard najpierw z prawdziwymi metrykami tekstowymi; rozkład wycen wymaga osobnego zakresu (2026-10-03).                                           |
| DEC-023 | Osobny local Compose i Testcontainers na PostgreSQL 17.6-alpine; trwały wolumen, host 5433 (2026-10-10).                                             |
| DEC-024 | Faktycznie wykonane ruchy mogą przekroczyć długość przez powtórzenia; ASCENT zawężony DEC-046 (2026-10-10).                                          |
| DEC-025 | TOP_ROPE jest sposobem asekuracji ROUTE, oddzielnym od OS/Flash/RP (2026-10-10).                                                                     |
| DEC-026 | WARMUP wymaga wyłącznie dodatnich ruchów, bez danych oceny i z zerowymi loadami (2026-10-10).                                                        |
| DEC-027 | Nowy bald ma edytowalną sugestię 8 ruchów (2026-10-10).                                                                                              |
| DEC-028 | Nowy ukończony bald bez znanej historii ma edytowalną sugestię OS (2026-10-10).                                                                      |
| DEC-029 | Zakresy French 1–9c i Font 2–9A+; punkty Font to przybliżona kalibracja v1, zachowana w snapshotach (2026-10-11).                                    |
| DEC-030 | Dodatkowe skale wejściowe/prezentacji mapują się wersjonowanie na Font/French; nie tworzą nowej punktacji (2026-10-11).                              |
| DEC-031 | RP bez historii używa LOW; potwierdzone OS/Flash z historią zachowuje 1,50 i audyt (2026-10-11).                                                     |
| DEC-032 | Fallback indeksu min. 0; brak kandydata Pyramid Support → maksimum/LOW; −6 i +5 jeszcze w zakresie kalibracji (2026-10-11).                          |
| DEC-033 | Największa długość/ruchy jednej wspinaczki nie jest rekordem; wycena, styl i load opisują różne cechy (2026-10-11).                                  |
| DEC-034 | Personal bests: maksimum sesji/lifetime/recent i style osobno dla skal, z datą i źródłową sesją (2026-10-11).                                        |
| DEC-035 | Details zwraca płaskie wpisy w kolejności; web grupuje po climbId; brak/cudza sesja → to samo 404 (2026-10-11).                                      |
| DEC-036 | Recent personal best obejmuje ruchome 12 miesięcy względem dnia odczytu, obok lifetime (2026-10-11).                                                 |
| DEC-037 | UUID jest tożsamością Area/Climb; identyczne nazwy nie łączą rekordów (2026-10-11).                                                                  |
| DEC-038 | Edycja przelicza tylko daną sesję; późniejsze snapshoty bez automatycznej kaskady (2026-10-11).                                                      |
| DEC-039 | Konflikt OS/Flash → 409 ASCENT_MODE_HISTORY_CONFLICT i możliwość jawnego potwierdzenia (2026-10-11).                                                 |
| DEC-040 | Prywatny Climb można zapisać przed treningiem bez sesji; mieszanie nowych/istniejących pozycji dozwolone (2026-10-17).                               |
| DEC-041 | Metry są osobnym faktem; sugestia ceil(metry × 2), ręczna liczba ruchów ma pierwszeństwo (2026-10-17).                                               |
| DEC-042 | Publiczna Dolina Bolechowicka z sektorem Filar Abazego; import i mapowanie Kurtyki z zachowaniem oryginału (2026-10-17).                             |

## Ustalenia web i organizacji

- WEB-001: web jako pierwszy klient. WEB-002: Start/Stop i wpisy lokalne, Finish zapisuje całość, bez pauzy i serwerowego Start. WEB-003: tylko wcześniejsza historia oraz wcześniejsze wpisy sesji.
- WEB-004: atomowy zapis i retry bez duplikatu, zmiana po sukcesie przez edycję. WEB-005: Dashboard z loadem/ruchami i maksimami osobno dla skal. WEB-006: Details grupuje po stabilnej tożsamości, zachowując próby.
- WEB-007: filtrowanie Area/Sector/Climb, brak sektora dozwolony. WEB-008: zdjęcia opcjonalne, upload poza pierwszym zakresem.
- ORG-001: utworzenie `.ai` i zachowanie źródeł; pierwotny plan 3 iteracji to historia porządkowania. ORG-002: polski język współpracy, angielskie identyfikatory i małe kroki. Użytkownik wybrał angielski UI 2026-10-17.

## Otwarte lub częściowo rozstrzygnięte

| ID      | Pozostały zakres i moment decyzji                                                                                                                                                                |
| ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| OPEN-04 | Szkic wdrożony; dalsza polityka wielu kart/liczby szkiców i serwerowy preview przed ich rozwojem.                                                                                                |
| OPEN-06 | Płaskie Details i grupowanie wdrożone; źródła i historyczność mediów przed uploadem.                                                                                                             |
| OPEN-07 | Finalny zapis ma fallback v3; prezentacja poziomu i wyników przed zapisem wymaga decyzji o preview.                                                                                              |
| OPEN-08 | Kod wymaga start/koniec i zaokrągla dodatni czas w górę do minuty; jakość/źródło czasu i dokładne reguły północy pozostają do doprecyzowania produktu.                                           |
| OPEN-12 | DEC-053: zakaz nakładania wdrożony. Nadal otwarta kolejność historii przy dopisywaniu wcześniejszych sesji; bez kaskady korekt (DEC-038). Retry i wersja rekordu działają.                       |
| OPEN-14 | Obecne URL/DTO/błędy wdrożone i opisane w API. Zamrożenie kolejnych rozszerzeń oraz pozostałe szczegóły kontraktu przy ich przyroście; stary zapis „przed endpointami” nie jest bieżącą blokadą. |
| OPEN-15 | Gęstość load/min i moves/min: zakres oraz jakość czasu przed dodaniem raportu.                                                                                                                   |
| OPEN-16 | Dane celów/ćwiczeń i reguły propozycji przed modułem planowania.                                                                                                                                 |
| OPEN-17 | UI, fetch/localStorage i testy web wybrane; biblioteki oraz konflikty offline mobile nadal otwarte.                                                                                              |
| OPEN-18 | Zakres wydania, dostępność i jednostka prognozy przed podaniem terminu; nieznany nakład nie jest zerem.                                                                                          |
| OPEN-20 | Load ma odrębne v3 bez wędki. Ewentualne ważenie Pyramid Support, wspólny estymator punktowy i trwały UserGradeReference pozostają dalszymi decyzjami.                                           |
| OPEN-21 | Dostawca, lokalizacje, rozmiar i pełny koszt produkcji; bez zakupu.                                                                                                                              |
| OPEN-22 | Zakres ADMIN: katalog, konta/role, wyjątki dostępu i prywatna historia innych osób; propozycja oddzielnej tożsamości oraz audytu. Mihu nie jest automatycznie administratorem.                   |
| OPEN-23 | Zatwierdzenie mechanizmu ochrony potwierdzonych zapisów przy utracie jednego VPS, dopuszczalna przerwa i wyniki prób restore/failover. Sam backup nie spełnia celu RPO=0.                        |
| OPEN-24 | Operator/model płatności, funkcje płatne, podatki/zwroty i dostęp po wygaśnięciu; wyłączone dla pierwszej grupy.                                                                                 |
| OPEN-25 | Własność domeny/kont, alarmy i czas reakcji, odzyskanie dostępu, prywatność/retencja oraz eksport/usunięcie konta przed zaproszeniem grupy. Utrzymanie zaakceptowane.                            |

## Zamknięte pytania — zachowane ID

| ID      | Rozstrzygnięcie                                                                                     |
| ------- | --------------------------------------------------------------------------------------------------- |
| OPEN-01 | DEC-019: `.ai` i archiwum w Git                                                                     |
| OPEN-02 | DEC-020: zgoda na odczyt/testy/dokumentację                                                         |
| OPEN-03 | DEC-018: jedno Area                                                                                 |
| OPEN-05 | DEC-022: metryki zamiast dekoracyjnych słupków                                                      |
| OPEN-09 | DEC-026: osobny WARMUP                                                                              |
| OPEN-10 | DEC-031: RP bez historii i konflikt OS/Flash                                                        |
| OPEN-11 | DEC-032: fallback i granice kalibracji                                                              |
| OPEN-13 | DEC-023: osobna lokalna baza                                                                        |
| OPEN-19 | DEC-043–044: CIRCUIT=7, wycofanie krótkiej ciągowej, crux i przejście v1/v2; frontend też ukończony |
| OPEN-26 | DEC-051: nazwa i potwierdzenie profilu                                                              |
