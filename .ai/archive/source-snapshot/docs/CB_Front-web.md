# ClimbBetter — frontend web React, plan i kanban

Status: prototyp UI oparty na mockach, plan v1 po zakończonej iteracji dokumentacji 4 z 4  
Data przeglądu kodu: 2026-08-15  
Katalog: `frontend-web/`

Aktualizacja kierunku 2026-09-22: pierwszy użyteczny etap ma umożliwić zapis i analizę treningów w webie zamiast w Excelu, zaczynając od `Record session`. Wymagania i propozycje do omówienia: [CB_web_stage_1.md](CB_web_stage_1.md). Poniższy wcześniejszy backlog pozostaje materiałem odniesienia; jego kolejność nie jest aktualnym zobowiązaniem do realizacji. Użytkownik pisze kod, asystent prowadzi dokumentację i review.

## 1. Cel aplikacji webowej

React jest głównym interfejsem do przeglądania i analizowania historii. W pierwszych iteracjach ma obsłużyć:

- Dashboard;
- Calendar Month/Day, później Week/Cycle;
- Session Details;
- My Areas i katalog wspinaczek;
- czytelne prezentowanie `classicLoad`, `adjustedLoad`, ruchów i intensywności pochodzących z backendu.

Web nie jest źródłem obliczeń treningowych. Może formatować i wizualizować liczby, ale nie wylicza wyników domenowych niezależnie od API.

## 2. Stan bieżący potwierdzony w kodzie

- [x] Projekt działa na React + TypeScript + Vite.
- [x] Jest routing i wspólny layout aplikacji.
- [x] Istnieją strony `DashboardPage`, `CalendarPage`, `SessionDetailsPage` i `AreasPage`.
- [x] Istnieją komponenty feedu, profilu, podsumowania dyscyplin i kart Area.
- [x] Istnieje prototyp stylów i układu ekranów.
- [ ] Wszystkie cztery strony importują dane produkcyjne bezpośrednio z plików mock.
- [ ] Nie istnieje klient HTTP ani konfiguracja URL backendu.
- [ ] Nie istnieje warstwa server state/cache.
- [ ] Nie istnieje standard obsługi loading, empty i API error.
- [ ] Nie ma zależności ani skryptu do testów komponentowych.
- [ ] `CalendarPage` jest na stałe ustawiony na maj 2026.
- [ ] `CalendarPage` sam grupuje sesje i sumuje niejednoznaczne `score`.
- [ ] `SessionDetailsPage` sam sumuje `points` i ruchy z wpisów.
- [ ] Typy używają starszych nazw `score`, `points`, `totalLength` i sformatowanego `duration`.
- [ ] `goal`, `method` i achievements są mockami bez zatwierdzonego modelu backendowego.

Wniosek: nie potrzeba przepisywać Reacta od zera. Potrzebny jest celowany refactor danych, stanów asynchronicznych i testów, a istniejące komponenty prezentacyjne można w większości zachować.

## 3. Zasady refaktoru

1. DTO HTTP nie są automatycznie modelami komponentów.
2. Dane z backendu są mapowane w jednym miejscu, a nie w każdej karcie osobno.
3. `score` znika na rzecz jawnych `classicLoad` i `adjustedLoad`.
4. Agregaty sesji, dnia i okresu przychodzą z backendu.
5. Data treningowa jest parsowana jako `YYYY-MM-DD`, bez przypadkowego przesunięcia przez strefę przeglądarki.
6. Każdy ekran API ma co najmniej stany: loading, success, empty, error oraz — gdzie dotyczy — not found.
7. Mock może pozostać fixture’em testowym, ale nie może być importowany przez zintegrowany kod produkcyjny.
8. Integrujemy ekranami, nie jednym wielkim refaktorem całej aplikacji.

## 4. Kolejność integracji

```text
fundament HTTP i testów
        ↓
Session Details
        ↓
lista/feed sesji
        ↓
Calendar
        ↓
Dashboard summary
        ↓
Areas
```

Session Details jest pierwsze, ponieważ najłatwiej na nim potwierdzić pełny kontrakt sesji, snapshoty i wartości wyliczone przez backend.

## 5. Etap 0 — uzgodnienie kontraktu i nazewnictwa

- [x] Ustalić `adjustedLoad` jako metrykę główną, a `classicLoad` jako mniej ważną wartość porównawczą.
- [x] Zatwierdzić `edlCount`, `moveIntensity`, `classicLoad` i `adjustedLoad`.
- [ ] Zatwierdzić reprezentację dyscypliny wyprowadzanej z wpisów.
- [ ] Usunąć założenie, że sesja ma zapisywane `primaryDiscipline`.
- [ ] Rozdzielić typ domenowy sesji od modelu karty/widoku.
- [ ] Ustalić, które achievements i goals są poza pierwszym kontraktem.
- [ ] Ustalić format `ProblemDetail` zwracany przez backend.
- [ ] Ustalić standard dat: `LocalDate` jako tekst ISO oraz `Instant` dla czasu absolutnego.

## 6. Etap 1 — fundament integracji

- [ ] Dodać `VITE_API_URL` i plik `.env.example`.
- [ ] Dodać mały, typowany wrapper nad `fetch`.
- [ ] Dodać obsługę JSON, `204`, przerwania requestu i błędów sieci.
- [ ] Zmapować backendowy `ProblemDetail` na czytelny model błędu UI.
- [ ] Dodać TanStack Query dla server state, cache, retry i invalidacji.
- [ ] Ustawić `QueryClientProvider` przy root aplikacji.
- [ ] Ustalić query keys według zasobu i filtrów.
- [ ] Wygenerować typy TypeScript z OpenAPI albo wprowadzić jawne DTO chronione testem kontraktu.
- [ ] Oddzielić `api DTO` od istniejących view models.
- [ ] Dodać wspólne formatowanie LocalDate, Instant, minut, loadu i wycen.
- [ ] Nie mapować `YYYY-MM-DD` przez `new Date()` bez jawnej polityki strefy.
- [ ] Dodać Vitest.
- [ ] Dodać React Testing Library.
- [ ] Dodać MSW do testów zachowania API.
- [ ] Dodać skrypty `test` i `test:watch`.
- [ ] Dodać pierwszy test success/loading/error dla query.

Rezultat: aplikacja potrafi bezpiecznie wywołać testowy endpoint i pokazać wszystkie podstawowe stany.

## 7. Etap 2 — Session Details

- [ ] Zdefiniować DTO `TrainingSessionDetailsResponse`.
- [ ] Zdefiniować view model wpisu z jawnymi nazwami metryk.
- [ ] Pobrać sesję po `sessionId` z route params.
- [ ] Usunąć import `sessionDetailsMock` z kodu produkcyjnego strony.
- [ ] Usunąć lokalne `reduce` liczące punkty i ruchy.
- [ ] Wyświetlić backendowe `SessionSummary`.
- [ ] Wyświetlić wpisy według `entryOrder`.
- [ ] Pokazać nazwę, typ, wycenę, styl, wynik oraz ruchy.
- [ ] Pokazać osobno rezultat, OS/Flash/RP, automatyczny familiarity band oraz `fastRp`.
- [ ] Pokazać `classicLoad` i `adjustedLoad` wpisu.
- [ ] Eksponować `adjustedLoad`; `classicLoad` pokazywać drugorzędnie lub w szczegółach.
- [ ] Pokazać `moveIntensity` i relative effort w sekcji szczegółowej/wyjaśnieniu.
- [ ] Pokazać notatki sesji i wpisu.
- [ ] Obsłużyć loading skeleton.
- [ ] Obsłużyć `404`, błąd sieci i retry.
- [ ] Obsłużyć sesję bez wpisów.
- [ ] Dodać testy success/loading/empty/error/not found.
- [ ] Zachować mock wyłącznie jako fixture testową, jeśli nadal jest przydatny.

Rezultat: strona szczegółów pokazuje wynik zapisany przez Javę bez samodzielnego liczenia domeny.

## 8. Etap 3 — lista sesji i Dashboard feed

- [ ] Zdefiniować stronicowane DTO listy sesji.
- [ ] Dodać hook/query listy z zakresem dat i paginacją.
- [ ] Podłączyć `ActivityFeed` do API.
- [ ] Zamienić `ActivityFeedItem.score` na jawne wartości modelu.
- [ ] Pokazywać charakter sesji wyprowadzony przez read model.
- [ ] Pokazywać `maxCompletedGrade`, ruchy, miejsce i datę.
- [ ] Linkować kartę do Session Details.
- [ ] Dodać load more albo paginację kursorem/stroną zgodnie z API.
- [ ] Obsłużyć pustą historię nowego użytkownika.
- [ ] Obsłużyć błąd pojedynczej sekcji bez wyłączania całego Dashboardu.
- [ ] Usunąć produkcyjny import `activityFeedMock`.
- [ ] Dodać testy feedu i mapowania metryk.

## 9. Etap 4 — Calendar

- [ ] Zastąpić stały maj 2026 stanem wybranego miesiąca.
- [ ] Dodać nawigację poprzedni/następny miesiąc i „dzisiaj”.
- [ ] Pobrać `calendar/month` dla widocznego zakresu.
- [ ] Usunąć filtrowanie `activityFeedMock` po stronie widoku.
- [ ] Usunąć lokalne sumowanie `score`.
- [ ] Wyświetlać backendowe agregaty dnia.
- [ ] Oznaczać dzień z wieloma sesjami.
- [ ] Dodać wybór dnia i panel jego sesji.
- [ ] Dodać link z sesji dnia do szczegółów.
- [ ] Dodać widok Day.
- [ ] Dodać widok Week po ustaleniu read modelu.
- [ ] Utrzymać grupowanie według `sessionDate`, nie strefy przeglądarki.
- [ ] Dodać testy przełomu miesiąca, roku i lokalnej daty.
- [ ] Dodać testy loading/empty/error.

## 10. Etap 5 — Dashboard Summary

- [ ] Zdefiniować zakres dat używany przez summary.
- [ ] Pobrać liczbę sesji, sesje tygodnia i ostatnią aktywność.
- [ ] Zaimplementować streak dopiero po zatwierdzeniu reguły backendu.
- [ ] Podłączyć `ProfileSummaryCard` do API.
- [ ] Podłączyć `DisciplineSummaryCard` do read modelu.
- [ ] Pokazywać `adjustedLoad` jako metrykę główną, a `classicLoad` jako wartość porównawczą.
- [ ] Pokazać osobny bieżący poziom balda i drogi wraz ze źródłem, sample size i confidence.
- [ ] Pokazać peak grade, wspierany poziom piramidy i jej kompletność jako różne pojęcia.
- [ ] Wyjaśniać confidence przez `sampleSize`, support score, liczności trzech warstw i użyte okno historii.
- [ ] Pokazać profil najwyższych OS, Fast RP i maksymalnego RP oraz osobną historię Flash dla balda i drogi.
- [ ] Pokazać różnice `RP Max - OS`, `Fast RP - OS` i `RP Max - Fast RP` jako profil projektowania wraz z sample size/confidence; nie formułować kategorycznej porady z pojedynczego wyniku.
- [ ] Dodać ręczny override poziomu po gotowym audycie backendu.
- [ ] Pokazywać ruchy oraz kompletność danych.
- [ ] Cele tygodniowe pozostawić jako sekcję „future” albo podłączyć dopiero po modelu `UserGoal`.
- [ ] Nie generować achievements na podstawie tekstowych heurystyk w UI.
- [ ] Usunąć pozostałe produkcyjne importy `dashboard.mock.ts`.
- [ ] Dodać test niezależnego błędu sekcji Dashboardu.

## 11. Etap 6 — Areas i katalog

- [x] Przyjąć pełne Area i minimalne prywatne drafty Area bez generycznego drzewa.
- [ ] Dostosować `AreaType` do kontraktu backendu.
- [ ] Pobrać `My Areas`.
- [ ] Rozdzielić uporządkowane „My Areas” od draftów utworzonych przez szybkie logowanie.
- [ ] Mapować techniczny status Area `DRAFT` bez ujawniania użytkownikowi zbędnego żargonu.
- [ ] Pozwolić uzupełnić draft Area albo połączyć go z Area użytkownika/oficjalnym.
- [ ] Pozwolić połączyć prywatny draft Climb z katalogową wspinaczką bez utraty historii.
- [ ] W przyszłości pozwolić pobrać oficjalny pakiet rejonu i porównać/scalić go z prywatnymi draftami.
- [ ] Pokazać type/facility type, lokalizację i status official/private.
- [ ] Pokazać liczbę wspinaczek i opcjonalnych sektorów.
- [ ] Pokazać favorite, recently used i liczbę projektów po wdrożeniu statusów użytkownika.
- [ ] Dodać szczegóły Area.
- [ ] Dodać listę/selekcję sektorów i wspinaczek.
- [ ] Dodać filtrowanie i wyszukiwanie.
- [ ] Usunąć produkcyjny import `areas.mock.ts`.
- [ ] Dodać testy pustej listy, miejsca bez sektorów i miejsca prywatnego.

## 12. Etap 7 — edycja danych

- [ ] Dodać edycję metadanych sesji.
- [ ] Dodać add/update/delete wpisu.
- [ ] Używać wersji/ETag lub pola `version` do optimistic locking.
- [ ] Pokazywać czytelny konflikt `409` i możliwość odświeżenia.
- [ ] Po mutacji invalidować tylko właściwe query keys.
- [ ] Nigdy nie przeliczać summary lokalnie jako wyniku końcowego.
- [ ] Dodać potwierdzenie operacji wpływających na historię.
- [ ] Dodać osobny ekran/proces reewaluacji dopiero po gotowym audycie backendu.
- [ ] Pozwolić skorygować automatyczne `familiarityBand` i deklarację OS/Flash/RP; korekta musi tworzyć revision backendu.
- [ ] Dla OS/Flash sprzecznego z historią pokazać ostrzeżenie, wymagać jawnego potwierdzenia i wysłać override do audytu backendu.

## 13. Etap 8 — UX, dostępność i jakość

- [ ] Ujednolicić język interfejsu: polski, angielski albo jawne i18n.
- [ ] Dodać semantyczne nagłówki, etykiety pól i obsługę klawiatury.
- [ ] Zweryfikować kontrast i focus states.
- [ ] Zapewnić responsywność Dashboardu, tabel i kalendarza.
- [ ] Dodać error boundary dla nieoczekiwanych błędów renderowania.
- [ ] Dodać toast tylko dla skutku akcji, nie jako jedyny komunikat błędu formularza.
- [ ] Dodać testy najważniejszych ścieżek użytkownika.
- [ ] Kontrolować bundle i nie dodawać ciężkiej biblioteki wykresów przed realną potrzebą.
- [ ] Dodać wykresy loadu dopiero po stabilnym read modelu.

## 14. Definition of Done integracji web

- [ ] `npm run lint`, `npm run build` i testy przechodzą.
- [ ] URL API jest konfigurowalny.
- [ ] Typy transportowe odpowiadają OpenAPI.
- [ ] Session Details, feed, Calendar i Areas używają API.
- [ ] Żaden zintegrowany ekran nie importuje produkcyjnego mocka.
- [ ] Żaden ekran nie liczy samodzielnie domenowych punktów/loadu.
- [ ] Każdy ekran obsługuje loading, empty i error.
- [ ] LocalDate nie zmienia dnia w zależności od strefy przeglądarki.
- [ ] Użytkownik może przejść od karty sesji do jej pełnych szczegółów.
- [ ] React pokazuje tę samą sesję i wyniki co Flutter.

## 15. Później, nie na critical path

- Cycle View i zaawansowana analityka;
- cele i challenges;
- social feed, komentarze i partnerzy;
- administracja słownikami;
- porównywanie wersji modelu;
- rozbudowane wizualizacje z sekcji Propozycje modelu wyceny.



Widać też konkretny problem: SessionDetails jest zdefiniowany w dashboard.types.ts i rozszerza ActivityFeedItem. Przez to model sesji zależy od sposobu prezentowania jej na dashboardzie. Kalendarz również korzysta z mocków dashboardu. Samo przeniesienie stron tego nie naprawi — trzeba uporządkować odpowiedzialność za dane.

src/
├── app/
│   ├── App.tsx
│   ├── AppLayout.tsx
│   └── router.tsx
│
├── features/
│   ├── training/
│   │   ├── pages/
│   │   │   ├── CalendarPage.tsx
│   │   │   └── SessionDetailsPage.tsx
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── services/
│   │   ├── model/
│   │   ├── mocks/
│   │   ├── utils/
│   │   └── index.ts
│   │
│   ├── catalog/
│   │   ├── pages/
│   │   │   └── AreasPage.tsx
│   │   ├── components/
│   │   ├── services/
│   │   ├── model/
│   │   └── mocks/
│   │
│   └── dashboard/
│       ├── pages/
│       │   └── DashboardPage.tsx
│       ├── components/
│       ├── services/
│       └── model/
│
├── shared/
│   ├── ui/
│   ├── api/
│   └── utils/
│
└── main.tsx



To propozycja struktury rozwijanej stopniowo — foldery tworzymy, gdy pojawia się dla nich kod.
Podział odpowiedzialności byłby następujący:
Obszar	Co do niego należy
training	Sesje, wpisy treningowe, logowanie, edycja i kalendarz treningów
catalog	Miejsca, sektory, drogi i baldy
dashboard	Ekran zbierający podsumowania i ostatnią aktywność
app	Składanie aplikacji: routing, layout, providery
shared	Uniwersalne elementy: przyciski, klient HTTP, formatowanie dat


training i catalog odpowiadają obszarom z planu backendu. Frontend może jednak mieć własne moduły ekranowe, takie jak dashboard — nie musi kopiować struktury Javy.
Najważniejsze zasady:
- Model ma właściciela. Sesja należy do training, nawet jeśli korzystają z niej trzy ekrany. Sam fakt współdzielenia nie oznacza, że trzeba przenieść ją do shared.
- Dashboard składa dane do prezentacji. Może korzystać z publicznie udostępnionych elementów training i mieć własny model karty, ale nie powinien definiować podstawowego modelu sesji.
- services obsługuje komunikację z API, model zawiera typy i mapowania danych, a hooks łączy pobieranie i stan danych z Reactem. Wspólny wrapper fetch trafia do shared/api; zapytanie o sesję do training/services.
- Moduły udostępniają wybrane elementy przez index.ts. Pozostały kod nie powinien sięgać do ich przypadkowych plików wewnętrznych. Unikamy zależności w obie strony.
Nie wydzielałbym teraz osobnego modułu dla każdego wpisu, sektora czy rodzaju wspinaczki. W CB sesja i jej wpisy są mocno powiązane, więc dobrze zacząć od wspólnego training.
