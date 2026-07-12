# ClimbBetter – ustalenia projektowe (Dashboard, Training, Areas)

## 1. Wizja produktu

ClimbBetter nie jest wyłącznie logbookiem wspinaczkowym.

Docelowo aplikacja ma łączyć cechy:

* Strava (feed aktywności, społeczność, obserwowanie znajomych)
* Polar Flow (kalendarz treningowy, przegląd tygodni i miesięcy)
* TrainingPeaks (analiza obciążeń, własne wskaźniki treningowe)
* specjalistycznego narzędzia dla wspinaczy

Kluczowy nacisk kładziemy na:

* analizę treningu wspinaczkowego,
* śledzenie progresji,
* obciążenia treningowe,
* planowanie cykli treningowych,
* szybkie logowanie sesji w skałach i na ściankach.

Społeczność jest ważna, ale nie jest głównym celem MVP.

---

# 2. Główne moduły aplikacji

## Dashboard

Odpowiada na pytanie:

> Co ostatnio robiłem?

Zawiera:

* Activity Feed
* Goals
* Profile Summary
* Discipline Summary

Inspiracja:

* Strava

---

## Training

Odpowiada na pytanie:

> Jak trenuję?

Docelowo składa się z widoków:

### Month

Pokazuje:

* kalendarz
* miesięczne obciążenia
* liczbę sesji
* sumę punktów

### Week

Pokazuje:

* mikrocykl
* rozkład treningów
* obciążenie tygodnia
* regenerację

### Day

Pokazuje:

* szczegóły dnia
* szczegóły sesji
* osiągnięcia
* notatki

### Cycle

Najważniejszy przyszły widok.

Pokazuje:

* mezocykl
* własny zakres dat
* progresję obciążenia
* deloady
* analizę planu treningowego

To ma być jedna z głównych przewag ClimbBetter nad Stravą i innymi logbookami.

---

## Areas

Odpowiada na pytanie:

> Gdzie się wspinam?

Nie jest celem budowa pełnego światowego katalogu dróg.

To niewykonalne i niepotrzebne.

Kluczowe jest:

### My Areas

Miejsca, w których użytkownik rzeczywiście trenuje.

Przykłady:

* Bronx
* Avatar
* Garaż
* Zimny Dół
* Dolina Bolechowicka
* Sadystówka

To jest główny widok modułu Areas.

---

### Official Areas

Dostarczane przez ClimbBetter.

Przykłady:

* Zimny Dół
* Dolina Bolechowicka
* Sadystówka
* inne ikoniczne rejony

Baza oficjalna jest ograniczona.

---

### User Areas

Użytkownik może tworzyć własne rejony.

Przykłady:

* Garaż
* Home Wall
* Ścianka firmowa
* lokalna sala boulderowa

---

# 3. Model Areas

## Area

Area może opcjonalnie posiadać sektory.

Przykłady:

### Małe area

Garaż

```txt
Garaż
 ├ Droga A
 ├ Droga B
 └ Droga C
```

Brak sektorów.

---

### Duże area

Siurana

```txt
Siurana
 ├ Sector A
 ├ Sector B
 ├ Sector C
 └ ...
```

Sektory są potrzebne.

---

Decyzja:

```txt
Area
 └ optional Sector
      └ Climb
```

Sector jest opcjonalny.

---

# 4. Model Climb

Minimalne pola:

* Name
* Type

  * Boulder
  * Route
  * Circuit
* Grade
* Area
* Optional Sector
* IsArchived
* IsFavorite
* IsProject

Dodatkowe pola wynikają z obecnego modelu API.

---

# 5. Favorites / Projects / Recently Used

Dla każdego Area użytkownik powinien szybko widzieć:

### Recently Used

Najczęściej używane drogi.

### Favorites

Ulubione drogi.

### Projects

Aktualne projekty.

To jest kluczowe dla szybkiego logowania treningów.

---

# 6. Offline First

To jedna z najważniejszych decyzji.

Wspinacze często trenują:

* w skałach,
* bez zasięgu,
* bez internetu.

Dlatego projektujemy z myślą:

```txt
mobile = offline first
```

Docelowo:

Telefon
→ lokalna baza

Po odzyskaniu internetu
→ synchronizacja

Ta decyzja powinna wpływać na wszystkie przyszłe rozwiązania.

---

# 7. Historia treningowa

Najważniejsza decyzja domenowa.

## Użytkownik może edytować stare sesje

Przykład:

* zapomniał dodać drogę
* źle wpisał styl przejścia
* pomylił liczbę prób

To jest dozwolone.

---

## Historia nie może zostać utracona

Nie może wydarzyć się sytuacja:

```txt
usuwam drogę

↓

znika historia treningów
```

---

# 8. Snapshoty

TrainingEntry powinien przechowywać snapshot danych.

Przykładowo:

* ClimbNameSnapshot
* GradeSnapshot
* AreaNameSnapshot
* PointsSnapshot

Dzięki temu:

* zmiana wyceny drogi
* zmiana nazwy
* usunięcie drogi

nie wpływa na stare treningi.

---

# 9. Archiwizacja zamiast usuwania

Drogi i baldy nie powinny być fizycznie usuwane.

Zamiast tego:

```txt
IsArchived = true
```

Skutki:

* nie pojawiają się na listach wyboru
* pozostają w historii
* nie psują statystyk

---

# 10. Daty i strefy czasowe

Wprowadzono rozróżnienie:

## SessionDate

Data treningowa.

Format:

```txt
YYYY-MM-DD
```

Przykład:

```txt
2026-05-10
```

Nie zawiera:

* godziny
* strefy czasowej

Kalendarz działa wyłącznie na SessionDate.

---

## Future

W przyszłości:

* StartedAtUtc
* EndedAtUtc

do analizy czasu trwania.

---

Powód:

Japoński użytkownik wspinający się w Hiszpanii po powrocie do Japonii nadal powinien widzieć sesję w tym samym dniu kalendarzowym.

---

# 11. Obciążenie treningowe

Jedna z kluczowych funkcji ClimbBetter.

Widoki treningowe mają koncentrować się na:

* punktach treningowych
* obciążeniu
* progresji

Nie tylko na typie aktywności.

---

Przykład:

Zamiast:

```txt
Bouldering
Sport
Circuit
```

ważniejsze jest:

```txt
100
110
120
80
130
```

czyli progresja obciążeń.

---

# 12. Wykres obciążenia

Planowana funkcja.

Dla Month View:

* oś X = dni
* oś Y = obciążenie

Pozwala obserwować:

* progresję
* deload
* narastanie zmęczenia

---

Dla Cycle View:

* Daily Load
* Weekly Load

Przykład:

```txt
Week 1 = 400
Week 2 = 450
Week 3 = 520
Week 4 = 300 (deload)
```

---

# 13. Kierunek rozwoju produktu

Najpierw:

* Dashboard
* Training
* Areas

Dopiero później:

* Community
* Social Feed
* Komentarze
* Partnerzy wspinaczkowi
* Community-maintained route database

Społeczność jest dodatkiem.

Rdzeniem produktu pozostaje trening i analiza wspinaczkowa.

---

# 14. Aktualny status mobile Flutter

Projekt Flutter zostal utworzony w `frontend-mobile`.

Aktualny stan implementacji:

* aplikacja ma pierwszy prototyp UI,
* `main.dart` zostal odchudzony do punktu startowego,
* kod zostal podzielony na `app`, `features` i `shared`,
* `Board` pokazuje ostatnie treningi,
* `Nowy` pokazuje formularz startu treningu,
* `Ty` pokazuje metryki, kalendarz i prosty wykres obciazenia,
* menu `...` na karcie treningu ma akcje `Ponow trening`,
* testy widgetowe przechodza.

Kierunek dalszej pracy mobile:

* dodac szczegoly treningu,
* rozbudowac formularz nowego treningu,
* wybrac lokalna baze danych dla offline first,
* przygotowac lokalne repozytorium danych,
* zaprojektowac synchronizacje mobile-backend.
