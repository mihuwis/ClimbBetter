# ClimbBetter — frontend mobile Flutter, plan i kanban

Status: prototyp UI i formularza w pamięci, plan v1 po zakończonej iteracji dokumentacji 4 z 4  
Data przeglądu kodu: 2026-08-15  
Katalog: `frontend-mobile/`

## 1. Cel aplikacji mobilnej

Flutter jest narzędziem do szybkiego logowania treningu w miejscu wspinania. Docelowy priorytet:

```text
szybki zapis → trwałość lokalna → synchronizacja → czytelny feedback
```

Mobile ma działać offline-first, ponieważ trening często odbywa się bez stabilnego internetu. W pierwszym pionowym przepływie możemy rozpocząć od prawdziwego zapisu online, ale kontrakt i identyfikatory od początku muszą być zgodne z późniejszą synchronizacją.

## 2. Stan bieżący potwierdzony w kodzie

- [x] Projekt Flutter istnieje i ma podział `app`, `features`, `shared`.
- [x] Jest shell z zakładkami Board/Home, Nowy i profil użytkownika.
- [x] Board pokazuje przykładowe ostatnie treningi.
- [x] Profil pokazuje prototyp metryk, kalendarza i wykresu loadu.
- [x] Istnieje rozbudowany formularz `Log done session`.
- [x] Formularz pozwala wybrać miejsce, dzień, start i czas trwania.
- [x] Formularz pozwala dodać wiele wspinaczek do sesji.
- [x] Istnieją wybory profilu, wyceny, ukończenia, stylu i ruchów.
- [x] Istnieje jeden widget test.
- [ ] Board korzysta z `mock_training_sessions.dart`.
- [ ] Formularz przechowuje dane wyłącznie w lokalnym stanie widgetu.
- [ ] Zapis kończy się tylko komunikatem `SnackBar`.
- [ ] Miejsca, wspinaczki, wyceny, style i profile są hardcoded.
- [ ] Nie ma klienta HTTP, DTO, repository ani obsługi API error.
- [ ] Nie ma lokalnej bazy ani outboxa.
- [ ] Nie ma state management poza lokalnym `setState`.
- [ ] Nie ma client UUID i modelu synchronizacji.
- [ ] Model karty zawiera sformatowane teksty i `Color`, więc nie nadaje się jako model danych/repository.
- [ ] Formularz używa `defaultEdl` profilu również jako długości wpisu; pojęcia `baseEdl` i `totalMoves` muszą zostać rozdzielone.
- [ ] Brakuje stabilnego `sessionName` i pełnej notatki sesji w payloadzie.

Wniosek: istniejący UI jest użytecznym prototypem. Refactor powinien zachować przepływ formularza, ale wydzielić modele danych, repository oraz dwa niezależne etapy: zapis lokalny i synchronizację.

## 3. Reguły architektoniczne mobile

1. Widgety nie wykonują bezpośrednio requestów HTTP ani zapytań SQL.
2. Model prezentacyjny karty nie jest modelem API ani tabelą lokalnej bazy.
3. Każda sesja, wpis i robocza wspinaczka dostaje client UUID przy tworzeniu.
4. Backend jest źródłem prawdy dla obliczeń, a mobile może przechowywać ich ostatni zsynchronizowany snapshot.
5. Użytkownik powinien móc zapisać sesję lokalnie nawet bez sieci.
6. Synchronizacja jest powtarzalna i idempotentna.
7. Słowniki są cache’owane lokalnie razem z czasem/wersją synchronizacji.
8. UI pokazuje status: draft, oczekuje na synchronizację, synchronizacja, zsynchronizowane, błąd/konflikt.
9. `sessionDate` pozostaje lokalną datą, `timeZoneId` używa IANA, a chwile start/koniec są przekazywane jako UTC/Instant.
10. Obliczenia wyświetlane przed synchronizacją muszą być jednoznacznie oznaczone jako preview albo w pierwszej wersji nie są liczone lokalnie.

## 4. Docelowy przepływ danych

```text
Widget formularza
      ↓
stan formularza / controller
      ↓
TrainingRepository
      ├── LocalDataSource → lokalna relacyjna DB + outbox
      └── RemoteDataSource → Java REST API
                               ↓
                          PostgreSQL
```

Board obserwuje lokalną bazę. Udana odpowiedź serwera aktualizuje lokalny rekord i snapshot wyników, dzięki czemu UI nie zależy od natychmiastowego ponownego requestu.

## 5. Decyzje technologiczne do potwierdzenia

Rekomendacja robocza, bez dodawania zależności w tej iteracji dokumentacyjnej:

- klient HTTP: `dio` albo standardowy `http`; `dio` jest wygodniejszy dla interceptors, timeoutów i anulowania;
- lokalna relacyjna baza: `drift` nad SQLite, ponieważ model ma relacje, zapytania i outbox;
- serializacja: `json_serializable`/`freezed` dopiero po zatwierdzeniu kształtu DTO;
- state management: Riverpod jako propozycja, ale nie jest konieczny do pierwszego małego requestu;
- routing: obecny prosty shell można zachować, dopóki deep links i auth nie uzasadnią osobnej biblioteki.

Wybór biblioteki lokalnej bazy musi nastąpić przed pełnym offline-first, ale nie blokuje bootstrapa Javy ani pierwszego prototypu online.

## 6. Etap 0 — poprawa modelu formularza

- [x] Przyjąć zatwierdzone `DEC-006`, `DEC-007`, `DEC-008` i `DEC-016` jako podstawę kontraktu formularza.
- [ ] Oddzielić `ClimbType` od `EffortProfile`.
- [ ] Oddzielić `EffortProfile.baseEdl` od `TrainingEntry.totalMoves`.
- [ ] Zmienić lokalne preset models na jawne DTO/view models.
- [ ] Dodać opcjonalne `sessionName`; przy braku nazwy pokazywać nazwę wygenerowaną w stylu `Wieczorna sesja boulderowa na Bronx`.
- [ ] Dodać notatkę sesji niezależną od notatki wpisu.
- [ ] Zapisywać jednoznaczne `resultType` oraz deklarowane `ascentMode`; nie tworzyć osobnego `isCompleted`.
- [ ] Pobierać z backendu wyprowadzone `familiarityBand` i wynikową `StyleRule`.
- [ ] Dodać `clientSessionId` i `clientEntryId`.
- [ ] Dodać `clientAreaId` dla szybkiej lokalizacji.
- [ ] Dodać `clientClimbId` dla wpisu bez katalogowej wspinaczki.
- [ ] Pozwolić rozpocząć sesję po podaniu samej nazwy miejsca; bez formularza kraju, regionu i sektora.
- [ ] Oznaczyć takie miejsce jako prywatne Area `DRAFT`, domyślnie ukryte w „My Areas” i możliwe później do uzupełnienia/merge.
- [ ] Pokazywać pełną skalę `1–9c/9C` właściwą dla `ClimbType`; obwód używa skali drogi.
- [ ] Użytkownik wybiera `resultType` i tryb OS/Flash/RP, a backend dobiera `familiarityBand` z historii dwóch lat.
- [ ] Pokazywać wyprowadzoną znajomość, osobne osiągnięcie Flash oraz `fastRp` dla RP w drugiej lub trzeciej próbie w całej znanej historii wspinaczki.
- [ ] Ostrzegać przy OS/Flash sprzecznym z lokalnie znaną historią i wymagać jawnego potwierdzenia.
- [ ] Gdy konflikt wykryje dopiero backend podczas synchronizacji, oznaczyć operację jako wymagającą decyzji użytkownika i nie ponawiać jej automatycznie.
- [ ] Przechowywać `sessionDate` jako wartość daty, nie sformatowany label.
- [ ] Pobierać rzeczywisty IANA `timeZoneId`.
- [ ] Reprezentować czas trwania w minutach.
- [ ] Rozróżnić czas domyślny od zmierzonego/ręcznie podanego.
- [ ] Dodać opcjonalny onboarding: najwyższy bald i droga pokonane w ostatnich dwóch latach.
- [ ] Pozwolić pominąć poziomy; po pierwszej sesji pokazać poziom wyznaczony przez backend wraz ze źródłem i confidence.
- [ ] W profilu rozdzielić najwyższe przejście od poziomu popartego piramidą i kompletności piramidy.
- [ ] Wyjaśniać confidence przez `sampleSize`, support score i liczności warstw zwrócone przez backend.
- [ ] Dodać możliwość edycji i usunięcia draftu wpisu przed zapisem sesji.
- [ ] Zachować `entryOrder` po zmianach listy.
- [ ] Wysyłać wpisy w tej kolejności, ponieważ wcześniejsza próba tej samej sesji wpływa na znajomość następnego wpisu.

Rezultat: formularz tworzy poprawny domenowo draft bez zależności od API.

## 7. Etap 1 — fundament danych i testów

- [ ] Dodać konfigurowalny URL API przez `--dart-define`.
- [ ] Udokumentować `10.0.2.2` dla emulatora Android.
- [ ] Utworzyć niezmienne DTO request/response zgodne z OpenAPI.
- [ ] Utworzyć osobne modele domenowe/draft i modele prezentacyjne.
- [ ] Utworzyć interfejs `TrainingRepository`.
- [ ] Utworzyć `RemoteTrainingDataSource`.
- [ ] Ukryć klienta HTTP za data source/repository.
- [ ] Dodać mapowanie `ProblemDetail` i błędów sieci.
- [ ] Zdefiniować timeout, retry i anulowanie requestu.
- [ ] Nie ponawiać automatycznie mutacji bez client ID/idempotencji.
- [ ] Dodać fake repository do widget tests.
- [ ] Dodać testy serializacji DTO.
- [ ] Dodać test repository dla success, validation error, timeout i retry.
- [ ] Rozbudować widget tests formularza.

## 8. Etap 2 — pierwszy działający przepływ online

- [ ] Pobrać Areas z API.
- [ ] Obok selektora Area umożliwić szybkie wpisanie nowej nazwy lokalizacji.
- [ ] Pobrać Grades, reguły stylu i EffortProfiles.
- [ ] Pobrać sugestie Climb dla wybranego Area.
- [ ] Cache’ować dane w pamięci na czas działania aplikacji.
- [ ] Obsłużyć loading/empty/error dla każdego selektora.
- [ ] Wysłać atomowo ukończoną sesję z listą wpisów.
- [ ] Pozwolić backendowi utworzyć draft Area i prywatne Climbs na podstawie client ID.
- [ ] Przygotować cache katalogu na przyszłe oficjalne, wersjonowane pakiety Area/Sector/Climb.
- [ ] Wysyłać fakty, ID i client ID, nigdy obliczenia jako źródło prawdy.
- [ ] Zablokować wielokrotne kliknięcie zapisu.
- [ ] Obsłużyć idempotentny retry po niepewnej odpowiedzi sieci.
- [ ] Pokazać walidację per pole i błąd całej sesji.
- [ ] Po sukcesie pokazać wartości policzone przez backend.
- [ ] Odświeżyć Board prawdziwą listą sesji.
- [ ] Dodać szczegóły sesji z wpisami i snapshotami.
- [ ] Usunąć produkcyjny import `mock_training_sessions.dart` po zielonej integracji.
- [ ] Dodać test: zapis → odpowiedź → sesja widoczna na Board.

Rezultat: sesja z telefonu trafia do PostgreSQL i wraca z wynikami Javy.

## 9. Etap 3 — lokalna baza

- [ ] Zatwierdzić bibliotekę relacyjnej bazy mobilnej.
- [ ] Zaprojektować tabele lokalne niezależne od widgetów.
- [ ] Dodać lokalne sesje, wpisy i snapshoty serwera.
- [ ] Dodać cache Areas, Climbs i słowników.
- [ ] Dodać metadane wersji/czasu odświeżenia słowników.
- [ ] Dodać lokalne migracje schematu.
- [ ] Zapisać draft sesji w trakcie wprowadzania.
- [ ] Zapisywać ukończoną sesję lokalnie przed próbą wysłania.
- [ ] Przełączyć Board na obserwowanie lokalnej bazy.
- [ ] Oznaczać rekordy lokalne i zsynchronizowane.
- [ ] Dodać test migracji lokalnej bazy.
- [ ] Dodać test restartu aplikacji z niedokończonym draftem.
- [ ] Dodać test restartu z sesją oczekującą na sync.

Rezultat: zamknięcie aplikacji lub brak internetu nie traci treningu.

## 10. Etap 4 — outbox i synchronizacja offline-first

- [ ] Dodać lokalny outbox zmian.
- [ ] Każdy wpis outboxa ma stable operation ID, payload, kolejność i retry count.
- [ ] Dodać worker synchronizacji reagujący na odzyskanie sieci i ręczne retry.
- [ ] Wysyłać operacje w wymaganej kolejności.
- [ ] Utrzymywać mapowanie client UUID → server UUID.
- [ ] Aktualizować lokalny snapshot wynikiem backendu.
- [ ] Dodać exponential backoff z limitem.
- [ ] Rozróżnić błąd przejściowy, walidację i konflikt.
- [ ] Nie zapętlać automatycznie błędów domenowych.
- [ ] Dodać tombstone dla usunięcia przed synchronizacją.
- [ ] Zaimplementować politykę konfliktów po jej zatwierdzeniu.
- [ ] Pokazać status sync na karcie i szczegółach.
- [ ] Dodać ekran/kolejkę problemów wymagających uwagi.
- [ ] Dodać test wielokrotnej dostawy tej samej sesji.
- [ ] Dodać test utraty odpowiedzi po zapisie na serwerze.
- [ ] Dodać test zmiany kolejności i częściowej awarii batcha.
- [ ] Dodać test edycji na dwóch urządzeniach.

Rezultat: synchronizacja jest bezpieczna, obserwowalna i nie tworzy duplikatów.

## 11. Etap 5 — Board i szczegóły treningu

- [ ] Zastąpić `TrainingSession` zawierający label/Color warstwowym view modelem.
- [ ] Pokazać datę, miejsce, czas i stan synchronizacji.
- [ ] Pokazać `adjustedLoad` jako główną metrykę oraz drugorzędny `classicLoad`, ruchy i liczbę wpisów.
- [ ] Pokazać wyprowadzony charakter sesji.
- [ ] Otwierać pełne Session Details.
- [ ] Pokazać wpisy w kolejności i oba loady.
- [ ] Pokazać offline cached snapshot.
- [ ] Oznaczyć, kiedy wyniki oczekują na obliczenie backendu.
- [ ] Dodać „Ponów trening” jako nowy draft z nowymi client ID.
- [ ] Nie kopiować historycznych wyników/snapshotów jako wejścia nowej sesji.
- [ ] Dodać filtrowanie po dacie/miejscu po pojawieniu się realnej historii.

## 12. Etap 6 — edycja i konflikty

- [ ] Edytować metadane sesji lokalnie i synchronizować zmianę.
- [ ] Dodawać, edytować i usuwać wpisy.
- [ ] Zachować kolejność i version serwera.
- [ ] Po sync zastępować lokalne preview snapshotem backendu.
- [ ] Pokazać konflikt wersji bez utraty lokalnej edycji.
- [ ] Umożliwić odrzucenie lokalnej zmiany albo ponowne zastosowanie po odświeżeniu.
- [ ] Dodać testy optimistic locking i konfliktów.

## 13. Etap 7 — Profile, Calendar i statystyki

- [ ] Podłączyć liczbę sesji i ruchów do prawdziwych read modeli.
- [ ] Podłączyć wykres loadu.
- [ ] Podłączyć kalendarz lokalnych dat sesji.
- [ ] Pokazać `adjustedLoad` jako główny trend, a `classicLoad` jako wartość porównawczą.
- [ ] Dodać zakresy 7/28 dni dopiero po wsparciu backendu.
- [ ] Pokazywać kompletność/jakość danych dla statystyk orientacyjnych.
- [ ] Pokazać najwyższe OS, Fast RP i maksymalne RP oraz historię Flash, gdy backend udostępni read model profilu.
- [ ] Pokazać różnice `RP Max - OS`, `Fast RP - OS` i `RP Max - Fast RP` razem z jakością danych, bez lokalnego wyliczania rekomendacji.
- [ ] Nie obliczać zaawansowanych statystyk niezależnym algorytmem na mobile.

## 14. Etap 8 — auth, UX i niezawodność

- [ ] Dodać OIDC po gotowym backendzie security.
- [ ] Bezpiecznie przechowywać tokeny w systemowym secure storage.
- [ ] Odświeżać token bez utraty lokalnego outboxa.
- [ ] Oddzielić dane lokalne użytkowników na współdzielonym urządzeniu.
- [ ] Dodać dostępność: semantics, rozmiary dotyku, focus i skalowanie tekstu.
- [ ] Ujednolicić język i przygotować i18n.
- [ ] Dodać telemetrykę crash/error bez wysyłania danych treningowych lub tokenów.
- [ ] Dodać testy na małym i dużym ekranie.
- [ ] Dodać testy wydajności listy dłuższej historii.

## 15. Definition of Done pierwszej integracji

- [ ] `flutter analyze` i `flutter test` przechodzą.
- [ ] Formularz używa osobnych `baseEdl` i `totalMoves`.
- [ ] Słowniki i Area pochodzą z API.
- [ ] Każdy nowy obiekt ma client UUID.
- [ ] Sesja jest zapisywana w PostgreSQL przez Javę.
- [ ] Wyniki na ekranie pochodzą z backendu.
- [ ] Board i Session Details odczytują prawdziwe dane.
- [ ] Loading, empty, validation, network error i retry są obsłużone.
- [ ] Kod produkcyjny zintegrowanego przepływu nie importuje mocków.
- [ ] Powtórny request nie tworzy duplikatu.

## 16. Definition of Done offline-first

- [ ] Sesję można utworzyć, zamknąć i zobaczyć bez sieci.
- [ ] Restart aplikacji nie traci draftu ani outboxa.
- [ ] Synchronizacja po odzyskaniu sieci jest idempotentna.
- [ ] UI pokazuje aktualny stan synchronizacji.
- [ ] Konflikt nie nadpisuje po cichu zmian.
- [ ] Client/server ID są trwale zmapowane.
- [ ] Aktualizacja słowników nie niszczy historycznych snapshotów.
- [ ] Testy obejmują brak odpowiedzi, retry, restart i wiele urządzeń.

## 17. Później, nie na critical path

- nagrywanie sesji na żywo;
- inteligentne sugestie stylu i profilu;
- automatyczne rozpoznawanie miejsca;
- import zdjęć/topo;
- zaawansowana analityka i planowanie cykli;
- społeczność, komentarze i partnerzy.
