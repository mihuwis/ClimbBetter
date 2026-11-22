# Katalog wspinaczek

Aktualizacja: 2026-11-09. Wdrożone scenariusze DEC-040–042; źródła: kontroler/serwisy `catalog`, migracja V15 i skrypty `infra/sql`.

Sesja może łączyć istniejące Climb wskazane UUID i nowe pozycje wskazane client ID. Osobny POST katalogowy pozwala zapamiętać prywatną wspinaczkę przed treningiem, bez utworzenia sesji ani kontaktu w historii. To zapis katalogu, nie planowanie treningu. POST z tym samym `clientClimbId` i treścią jest bezpiecznym retry; inna treść daje `409 CLIENT_CLIMB_ID_CONFLICT`.

Widoczność obejmuje własne prywatne i publiczne obiekty. Publiczne Area nie udostępnia cudzych prywatnych Climb. Archiwalne/scalone miejsca i archiwalne wspinaczki są pomijane w wyszukiwaniu, zachowując historyczne snapshoty. Sektory należą do wybranego Area; brak sektora jest dozwolony.

## Długość i wycena

`lengthMeters` jest dodatnią długością z dokładnością do 2 miejsc. Sugestia ruchów: jawne `suggestedMoveCount` → `ceil(lengthMeters × 2)` → 8 dla balda bez obu danych. Droga/obwód bez tych danych nie ma sugestii. Metry nie wyznaczają profilu ani rzeczywiście wykonanej pracy.

Obliczenia używają Font/French. Katalog przyjmuje też `gradeSystemCode=KURTYKA`; alias zachowuje oryginał i wersję mapowania, a zwraca kanoniczny `gradeLabel`. Wersja 1: m.in. `VI.2+ → 6c+`, `VI.5 → 7c+`, `VI.5+ → 8a`, `VI.5+/6 → 8a+`; `IV → 4a` jest jawnym przybliżeniem. Źródła mapowania zapisane historycznie: [PZA](https://www.ranking.pza.org.pl/info/regulamin), [UKA](https://topo.uka.pl/content/topo/skala.html). Nie weryfikowano tu ponownie zewnętrznych tabel.

POST sesji nadal przyjmuje kanoniczną etykietę. Bezpośredni zapis aliasu i jego oryginału we snapshotach wejścia sesji jest dalszym rozwinięciem. Projekt bez wyceny może istnieć w katalogu, ale oceniany wpis treningowy wymaga wyceny.

## Lokalne dane

`infra/sql/local-sample-catalog.sql` zawiera demonstracyjne Bronx/Garaż. `infra/sql/bolechowicka-public-catalog.sql` zawiera publiczną Dolinę Bolechowicką, sektor Filar Abazego i 28 pozycji, w tym 27 wycenionych; projekt ma brak wyceny, 20 m i sugestię 40 ruchów. To importy local, nie automatyczne dane każdej produkcji; dawna historia DEMO nie opisuje aktualnych liczników użytkownika.

[Endpointy katalogu](api.md) · [tabele i ograniczenia](database.md) · [historyczny opis importu](../archive/documentation-2026-11-09/snapshot/.ai/documentation/outdoor-catalog.md).
