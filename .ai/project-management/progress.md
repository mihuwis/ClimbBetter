# Stan aplikacji

Stan bazowy: 2026-11-09; przyrost DEC-053 wdrożony i przetestowany 2026-11-15. Odbiór Google/profilu przez użytkownika: 2026-11-08. [Wyniki z datami](../documentation/backend-verification.md).

| Obszar          | Stan                                                                                                                                |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Java/PostgreSQL | atomowy CRUD sesji, audyt, retry, blokada kolizji czasu (V25/DEC-053), katalog, Dashboard i personal bests                          |
| Model           | v3 dla nowych zapisów/edycji; intensywność profili/cruxów z v2, odniesienie poziomu z aktualnością; odczyt historii v1/v2 zachowany |
| React           | rzeczywiste API, sesje ręczne/Start now, trwały szkic, Details/edycja/usuwanie, Calendar, Areas, Repeat climb/session               |
| Statistics      | piramida: 12 kombinacji i lista warstw; Training summary: miesiąc/rok, wykresy, style i linki do sesji                              |
| Auth/profil     | Google/Firebase, ochrona API/tras, mapowanie UID → UUID; profil Mihu i historia odebrane przez użytkownika                          |
| Baza local      | V25 potwierdzone 2026-11-15; historia zachowana; osobna baza/emulator dla kont modelowych                                           |
| Produkcja/CI    | plan dev local + prod gotowy do dalszych decyzji; brak obrazu aplikacji, konfiguracji prod i workflowów GitHub Actions              |
| Flutter         | prototyp na mockach, bez API/auth/trwałej bazy/synchronizacji                                                                       |

Lokalny etap Firebase jest zakończony. Raporty Statistics są wdrożone i automatycznie sprawdzone; pełny ręczny odbiór oraz ocena ergonomii pozostają odrębnym zadaniem. Dawne wymagania restartu do v2/piramidy/summary nie są bieżącym potwierdzeniem stanu procesu — po nich udokumentowano uruchomienie backendu z auth/V24.

Najbliższa większa luka dotyczy produkcji: wybór kosztu i ochrony danych, zakres ADMIN oraz implementacja/odbiór hostingu i wydania. [Priorytety](priorities.md), [warunki prod](../documentation/Environments_plan.md). Nie podajemy procentu ani daty ukończenia bez zamkniętego zakresu i dostępności (OPEN-18); znany czas jest w [work-log](work-log.md).
