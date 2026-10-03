# Rozwój iteracyjny i przyrostowy

Data: 2026-10-03. Rozdzielamy trzy iteracje porządkowania dokumentacji od przyrostów działającej aplikacji. Trzy iteracje dokumentacji nie oznaczają ukończenia produktu w trzech iteracjach.

## Trzy iteracje dokumentacji

| Iteracja               | Rezultat                                                                                                                                         | Warunek przejścia                                                                |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| 1 — porządkowanie ---- | uzgodniona `.ai`, pełne źródła w archiwum, aktualny opis kodu, rozdzielenie ustaleń od propozycji, pierwsze diagramy i rejestr pytań ----------- | źródła i odsyłacze sprawdzone; użytkownik otrzymuje decyzje do omówienia ------- |
| 2 — doprecyzowanie --- | odpowiedzi przeniesione do modelu, kontraktu ukończonej sesji, Dashboardu/Details i kryteriów akceptacji; dokładny zakres najbliższego przyrostu | kluczowe luki danego przyrostu rozstrzygnięte; pozostałe jawnie odłożone ------- |
| 3 — kontrola spójności | przegląd decyzji, dokumentów, diagramów i pokrycia archiwum; kontrola wykonania zasad wersjonowania i starych lokalizacji z DEC-019 ------------- | brak sprzecznych aktywnych źródeł; jasno ustalony następny mały krok użytkownika |

Nie domykamy drugiej i trzeciej iteracji bez rozstrzygnięć użytkownika. To zaplanowane punkty wspólnej pracy, nie blokada wykonywania uzgodnionej iteracji 1.

## Przyrosty aplikacji

To orientacyjna kolejność, bez zobowiązania czasowego. Pierwszy przyrost rozpisujemy szczegółowiej; późniejsze są celami do doprecyzowania.

| ID     | Przyrost                                    | Dowód użyteczności / ukończenia                                                                                                                                           |
| ------ | ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| APP-01 | Java zapisuje i odczytuje prawdziwy trening | ukończona sesja z próbą i przejściem tej samej drogi trafia atomowo do PostgreSQL; odczyt zachowuje wynik, kolejność i snapshoty; retry bez duplikatu ------------------- |
| APP-02 | Web zastępuje pierwszy przepływ Excela ---- | Record session → Finish → karta Dashboardu → pogrupowane Details; dane wracają po ponownym otwarciu, load pochodzi z Javy; odporność szkicu według zatwierdzonego zakresu |
| APP-03 | Użyteczna historia i korekty -------------- | uzgodnione kolejne intencje, kalendarz/katalog i edycja; audyt nie pozwala po cichu zmienić historii -------------------------------------------------------------------- |
| APP-04 | Mobile korzysta ze wspólnego API ---------- | zapis i odczyt tej samej sesji w Flutterze i webie ---------------------------------------------------------------------------------------------------------------------- |
| APP-05 | Mobile działa bez sieci ------------------- | trwały szkic i outbox przeżywają restart; synchronizacja i konflikty nie gubią ani nie dublują danych ------------------------------------------------------------------- |
| APP-06 | Planowanie i propozycje treningów --------- | osobno ustalone dane wejściowe, reguły i sposób oceny użyteczności propozycji ------------------------------------------------------------------------------------------- |

Przed udostępnieniem rzeczywistym użytkownikom dochodzą właściwa autoryzacja, izolacja danych, konfiguracja wdrożenia i sprawdzone odtwarzanie kopii. To warunek takiego wydania, nie potwierdzenie, że pierwszy lokalny prototyp ma te elementy.

## Najbliższy przyrost APP-01

Przed implementacją kontraktu rozstrzygamy pytania wpływające na DTO, dane i kalkulator. Budujemy na istniejącym bootstrapie Javy. Dowód działania obejmuje migrację pustej bazy, zatwierdzone przypadki obliczeń, słowniki/katalog, atomowy zapis i read modele potrzebne Dashboardowi oraz Details. Nie rozbudowujemy wszystkich CRUD-ów przed pierwszym zapisem treningu.

Przypadek przewodni: brak wcześniejszej historii → nieudana pierwsza próba → przejście tej samej drogi → inna droga → zapis → odczyt → ponowienie tego samego requestu. Drugi wpis widzi pierwszy jako wcześniejszy kontakt. Warianty obejmują błędny wpis i rollback, sesję mieszaną oraz brak ukończonej wyceny.

## Zasada iteracji implementacyjnej

Wybieramy mały sprawdzalny rezultat, wyjaśniamy go, użytkownik implementuje, asystent robi review, a wynik weryfikacji trafia do [progress](progress.md) i [work-log](work-log.md). Dopiero potem wybieramy następny krok. Każdy przyrost rozwija działający przepływ; nie odhaczamy całej warstwy tylko dlatego, że powstały katalogi.
