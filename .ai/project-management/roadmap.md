# Kierunek rozwoju

Aktualizacja: 2026-11-09. Status wynika z kodu i datowanych odbiorów, bez prognozy terminu. Bieżące zadania są wyłącznie w [priorities](priorities.md).

| ID     | Przyrost               | Stan / warunek dalszej pracy                                                                 |
| ------ | ---------------------- | -------------------------------------------------------------------------------------------- |
| APP-01 | zapis/odczyt Java      | wdrożone: transakcja, snapshoty, retry, CRUD, audyt i katalog                                |
| APP-02 | użyteczny przepływ web | wdrożone API/UI, szkic i odczyt; kolejne oceny ergonomii według potrzeb                      |
| APP-03 | historia i korekty     | wdrożone Calendar, Areas, edycja/usuwanie i powtarzanie; pełna edycja katalogu/media później |
| APP-04 | mobile ze wspólnym API | niewdrożone; potwierdzić zapis/odczyt tej samej sesji co web                                 |
| APP-05 | mobile offline         | niewdrożone; trwały szkic/outbox, retry bez duplikacji i jawne konflikty                     |
| APP-06 | planowanie/propozycje  | odłożone; najpierw dane celów i zasady doboru treningu                                       |

Poza pierwotną numeracją APP wykonano model v2/v3, piramidę, podsumowanie miesiąca/roku oraz lokalne Firebase. Nie oznacza to ukończenia całego produktu ani odbioru wszystkich ekranów.

Najbliższy etap wydania: płatna produkcja dla 5–10 osób, ochrona danych, monitoring i powtarzalne wydanie GitHub Actions/GHCR. To plan bez zakupionej infrastruktury; warunki w [Environments plan](../documentation/Environments_plan.md).

Pierwotne 3 iteracje dokumentacji opisują historyczną organizację prac. Przegląd z 2026-11-09 aktualizuje i skraca aktywną wiedzę; nie rozstrzyga za użytkownika nadal otwartych decyzji biznesowych. [Pełne poprzednie plany](../archive/documentation-2026-11-09/README.md).
