# Historia prac i pomiary

Data utworzenia: 2026-10-03. To ewidencja zdarzeń i rzeczywiście znanego czasu, nie odtworzenie nakładu z dat Git.

## Znane zdarzenia

| Data       | Źródło                                    | Rezultat / zdarzenie                                                                                                                   | Nakład       | Granica dowodu                                                                              |
| ---------- | ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- | ------------ | ------------------------------------------------------------------------------------------- |
| 2026-07-12 | commit `119841d` ------------------------ | zapis etapu prototypu mobile --------------------------------------------------------------------------------------------------------- | nieznany --- | opis historycznego commitu; nie aktualny wynik testów ------------------------------------- |
| 2026-08-15 | dokumenty modelu i wyceny --------------- | konsolidacja specyfikacji, dawna iteracja 4 z 4 -------------------------------------------------------------------------------------- | nieznany --- | stan dokumentacji ------------------------------------------------------------------------- |
| 2026-08-15 | commit `da1324a` ------------------------ | migracja kierunku backendu do Java 21 ------------------------------------------------------------------------------------------------ | nieznany --- | historyczny commit ------------------------------------------------------------------------ |
| 2026-08-16 | commity `91ae403`, `5cca895` ------------ | bootstrap Java/Spring i Clock -------------------------------------------------------------------------------------------------------- | nieznany --- | historyczne commity; obecność plików potwierdzona 2026-10-03 ------------------------------ |
| 2026-08-18 | commit `fe7ec39` ------------------------ | dodanie testów Clock ----------------------------------------------------------------------------------------------------------------- | nieznany --- | kod testów istnieje; nie uruchamiano go teraz --------------------------------------------- |
| 2026-09-22 | `CB_web_stage_1.md` i aktualizacje planów | web-first, lokalny szkic, atomowy zapis, Dashboard i Details ------------------------------------------------------------------------- | nieznany --- | data dokumentu, nie implementacji --------------------------------------------------------- |
| 2026-10-03 | polecenie użytkownika i nowe `.ai` ------ | iteracja 1: struktura, materiały, archiwum, status, diagramy i otwarte decyzje ------------------------------------------------------- | niezmierzony | zweryfikowano wymagane pliki, 21 kopii SHA-256 i aktywne linki; bez implementacji aplikacji |
| 2026-10-03 | decyzja użytkownika DEC-018 ------------- | zamknięto OPEN-03: jedna sesja to jedno Area; zapisano regułę i objaśnienie dla użytkowników, ujednolicono formularz, API, model i ERD | niezmierzony | aktualizacja dokumentacji; bez implementacji reguły w kodzie ------------------------------ |
| 2026-10-03 | decyzje użytkownika DEC-019–022 --------- | rozpoczęto iterację 2: `.ai` w Git, nowe zasady uprawnień, wymagany odzyskiwalny szkic i Dashboard bez dekoracyjnego wykresu ---------- | niezmierzony | odpowiedzi zapisano w aktywnej dokumentacji; bez zmian kodu aplikacji --------------------- |

Potwierdzenie APP-01 z 2026-10-03: użytkownik zaimplementował `FamiliarityBand.fromPriorContactCount` i uruchomił celowany test granic oraz wyjątku. Wynik: 7 testów, 0 failures, 0 errors. Zakres dowodu obejmuje wyłącznie tę regułę domenową.

## Ewidencja kolejnych prac

Każdy nowy wpis powinien mieć: datę, ID przyrostu/zadania, krótki rezultat, status przed/po, oszacowanie (jeśli przyjęte), rzeczywisty nakład, źródło pomiaru i wynik sprawdzenia. Oddzielamy czas użytkownika, pracę asystenta i czas oczekiwania. Jeśli czas nie był mierzony lub podany, wpisujemy `nieznany`.

| Data       | ID              | Rezultat                                                                 | Status                          | Estymata | Czas użytkownika | Czas asystenta  | Weryfikacja                                                          |
| ---------- | --------------- | ------------------------------------------------------------------------ | ------------------------------- | -------- | ---------------- | --------------- | -------------------------------------------------------------------- |
| 2026-10-03 | DOC-02 / APP-01 | rama `.ai`, decyzje DEC-018–022 i `FamiliarityBand` z testami            | sesja zakończona; prace trwają  | brak     | 4 h              | nieznany        | archiwum 21/21, linki, kompilacja oraz `FamiliarityBandTest` 7/7     |

## Obserwacje do wykresów

Po ustaleniu jednostki w OPEN-18 zapisujemy na koniec pracy lub iteracji stan zakresu i pozostałą pracę. Do burnup dopisujemy każdą zmianę całego zakresu. Nie zmieniamy wcześniejszych obserwacji w celu poprawienia przebiegu wykresu; korektę oznaczamy osobno.

| Data       | Iteracja/przyrost           | Jednostka   | Zakres na start | Zmiana zakresu | Ukończone narastająco | Pozostałe     | Źródło                       |
| ---------- | --------------------------- | ----------- | --------------- | -------------- | --------------------- | ------------- | ---------------------------- |
| 2026-10-03 | przed planowaniem APP-01/02 | nieustalona | nieoszacowane   | nieoszacowane  | niezmierzone -------- | nieoszacowane | przegląd dokumentacji i kodu |

Interpretacja i bieżąca prognoza: [progress](progress.md). Zakres przyrostów: [roadmap](roadmap.md).
