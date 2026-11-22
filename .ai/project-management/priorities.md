# Bieżące priorytety

Aktualizacja: 2026-11-09. Punkt wyjścia: [działający zakres](progress.md), zakończone lokalne Firebase i oba raporty Statistics. Lista obejmuje pozostałe zadania; nie jest automatycznym zleceniem zmian kodu ani zakupu.

## Teraz

1. Wybrać hosting, pełny budżet oraz ochronę potwierdzonych zapisów i dopuszczalny czas przerwy — OPEN-21/23. [Plan produkcji](../documentation/Environments_plan.md).
2. Ustalić zakres ADMIN i kont operatorów; własność domeny, alarmy i zasady danych przed zaproszeniem osób — OPEN-22/25.
3. Po zleceniu implementacji przygotować obraz web/API, konfigurację prod/Firebase, izolację seedów local, CI GitHub Actions → GHCR i ręczne wydanie.
4. Przed udostępnieniem sprawdzić restore/WAL, awarię primary/repliki, failover, izolację kont i rollback aplikacji według bramek planu.
5. Zebrać ręczną ocenę Statistics i ergonomii powtarzania sesji/wpisu. Domknąć pełny odbiór piramidy (dawne P13): filtry, listy, telefon i zmiany danych po CRUD na zestawie testowym.

## Później

- Sezon i kolejne raporty po zdefiniowaniu granic; ewentualny rozwój estymatorów/poziomu (OPEN-20).
- Preview i dalsza niezawodność szkicu, kolejność historii przy dopisywaniu wcześniejszych sesji (OPEN-04/07/12). Zakaz nakładania czasu z DEC-053 wdrożony 2026-11-15.
- Ankieta, płatności (OPEN-24), media i porządkowanie katalogu.
- Flutter/API, trwały szkic, offline/outbox i konflikty; osobny moduł planowania/propozycji treningów.

Pełne pytania i decyzje mają jedno miejsce: [rejestr](../documentation/decisions.md). Nie ponawiamy zamkniętego mapowania Mihu, replay v3 ani historycznych checklist wdrożenia v2.
