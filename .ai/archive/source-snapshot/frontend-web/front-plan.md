# ClimbBetter frontend web plan

Status: roboczy plan frontendu React.

## Technologia

- React z Vite i TypeScript.
- React Router dla widokow Dashboard, Calendar, Areas i szczegolow sesji.
- Aktualnie czesc danych jest mockowana; celem jest stopniowe podlaczanie API.
- UI powinien byc szybki, czytelny i roboczy, bardziej narzedziowy niz marketingowy.

## Najblizsze cele

- Podlaczyc Dashboard do danych z API.
- Ustabilizowac typy frontendowe pod kontrakty backendu.
- Rozbudowac Calendar o realne sesje i szczegoly dnia.
- Rozbudowac SessionDetails o wpisy treningowe i metryki.
- Przygotowac Areas jako miejsce do wyboru drog, projektow i ulubionych.
- Zaczac wydzielac warstwe API client/services zamiast korzystania z mockow w komponentach.

## Widoki

- Dashboard: profile summary, discipline summary, activity feed, goals.
- Calendar: widok miesiaca, lista sesji, obciazenie dzienne/tygodniowe.
- Session details: podsumowanie sesji, wpisy, notatki, punkty, rozklad trudnosci.
- Areas: moje miejsca, official areas, user areas, lista drog i filtrowanie.
- Future Training Cycle: zakres dat, obciazenie dzienne/tygodniowe, deloady, progres.

## UX

- Szybkie skanowanie danych treningowych.
- Male opory przy przechodzeniu z feedu do szczegolow sesji.
- Czytelne metryki: score, liczba sesji, streak, rozklad trudnosci.
- Spokojny, aplikacyjny layout bez landing page feel.
- Responsywnosc, ale web nie musi zastapic mobile w logowaniu offline.

## Dane i integracja

- Zmapowac DTO z backendu na typy UI.
- Utrzymac mocki tylko tam, gdzie backend jeszcze nie ma endpointow.
- Dodac obsluge loading/error/empty state dla widokow z API.
- Ustalic format daty SessionDate po stronie UI.
- Przygotowac miejsce na auth context, gdy backend dostanie uzytkownikow.

## Jakosc

- Uruchamiac lint i build przed wiekszymi zmianami.
- Dodac testy dla funkcji kalendarza i mapperow danych.
- Trzymac komponenty feature-first: dashboard, calendar, areas, sessions.
- Unikac mieszania logiki API bezposrednio w komponentach prezentacyjnych.

## Pytania otwarte

- Czy w pierwszej wersji web pozwala tworzyc sesje, czy tylko analizuje dane?
- Jakie wykresy sa potrzebne w MVP?
- Jak mocno dashboard ma przypominac feed spolecznosciowy?
- Jak bedzie wygladala nawigacja po cyklach treningowych?
