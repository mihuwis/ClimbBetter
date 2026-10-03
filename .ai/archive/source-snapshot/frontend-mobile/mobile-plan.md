# ClimbBetter mobile plan

Status: roboczy plan aplikacji mobilnej Flutter.

## Najblizsze TODO

### 1. Przebudowac akcje `Nowy`

Status: In progress.

Dolny przycisk `Nowy` / plus nie powinien od razu pokazywac sztywnego ekranu
formularza. Ma otwierac modal z wyborem intencji uzytkownika.

Po kliknieciu `Nowy` pokazujemy modal:

| Akcja | Status | Co oznacza |
| --- | --- | --- |
| Start new session | Stub | Uzytkownik zaczyna trening teraz, na zywo. |
| Repeat logged session | Stub | Uzytkownik chce ponowic wczesniej zapisany trening. |
| Log done session | In progress | Uzytkownik wpisuje sesje, ktora juz zrobil dzisiaj/wczoraj. |
| Plan session | Stub | Uzytkownik planuje przyszly trening. |

Na razie implementujemy tylko `Log done session`. Pozostale akcje sa zaslepkami.

### 2. `Log done session`: parametry sesji

Status: Todo.

Cel: szybko wpisac trening wykonany wczesniej, np. dzisiaj rano albo wczoraj.

Minimalne pola sesji:

- miejsce,
- data: dzisiaj / wczoraj / pozniej wlasna data,
- poczatek treningu,
- czas trwania wybierany szybko: `1h`, `1.5h`, `2h`, `3h`,
- pozniej mozliwe: dokladny koniec treningu.

Nie uzywamy suwaka `intensywnosc`. Na tym etapie nie wiadomo, co mialby
realnie znaczyc i jak mialby wplywac na dane.

### 3. `Log done session`: dodawanie baldow/drog

Status: Todo.

Po ustawieniu parametrow sesji uzytkownik dodaje wpisy wspinaczkowe.

Minimalne pola wpisu:

- nazwa / identyfikacja, np. `czerwone placki`,
- miejsce / lokacja, np. `Bronx`,
- typ profilu wspinu,
- wycena,
- czy zrobiony,
- styl,
- EDL, czyli estimated difficulty length / startowa liczba ruchow,
- liczba ruchow wykonanych.

Po rozpoczeciu wpisywania nazwy aplikacja powinna pokazywac sugestie znanych
baldow/drog. Na razie beda to mocki.

Jesli wpis jest oznaczony jako zrobiony, liczba ruchow wykonanych domyslnie
rowna sie EDL. Przy probie / attempt uzytkownik moze wpisac mniej ruchow, np.
`2 z 6`.

### 4. Presety profilu wspinu i EDL

Status: Todo.

EDL jest na razie proponowana liczba ruchow. To preset startowy, ktory uzytkownik
moze zmienic.

| Profil | Kiedy uzywac | EDL startowe |
| --- | --- | --- |
| Bald | Klasyczny bald 4-10 ruchow. | 4 |
| Krotka droga / baldowa | 4-12 ruchow, w sumie bald z lina. | 8 |
| Krotka droga ciagowa | 8-15 ruchow ciagowe. | 10 |
| Srednia ciagowa | 15-40 ruchow bez wyraznego cruxa. | 28 |
| Srednia cruxowa | Wyrazny crux, odpoczynki, latwiejsze sekcje. | 15 |
| Dluga ciagowa | Dluga, ponad 35-40 ruchow. | 40 |
| Dluga cruxowa | Wyrazny crux, odpoczynki, latwiejsze sekcje. | 22 |
| Obwod | Treningowy obwod na panelu. | 20 |
| Rozgrzewka | Rozgrzewka. | 0 |

### 5. Style i wplyw na punktacje

Status: Todo.

Styl ma wplywac na punktacje. Na razie zapisujemy mnoznik i opis. Sam wzor
punktowy dopracujemy pozniej.

| Styl | Mnoznik | Opis |
| --- | ---: | --- |
| OS/Flash | 1.5 | Przejscie od razu albo bardzo szybkie. |
| RP slaba znajomosc | 1.3 | Pelny RP po slabym rozeznaniu. |
| RP normalny | 1.0 | RP / attempt RP okolo 5-12 prob w sezonie. |
| RP staly | 0.75 | Proby powtarzane, znana droga/bald. |
| Rozgrzewka | 0.0 | Nie liczy sie do punktow, liczy sie do ruchow. |
| Attempt OS/FL | 1.5 | Proba od razu albo bardzo szybka. |
| Attempt slaba znajomosc | 1.2 | Proba po slabym rozeznaniu. |
| Attempt normalny | 0.9 | Proba okolo 5-12 prob w sezonie. |
| Attempt staly | 0.5 | Regularne probowanie tego samego problemu. |

### 6. Projektowac pod funkcje, nie sztywne ekrany

Status: Todo.

Kazda kolejna funkcja powinna isc takim schematem:

1. Opisac intencje uzytkownika.
2. Zdefiniowac dane, ktore trzeba zebrac.
3. Dopiero potem dobrac ekran, modal albo flow.
4. Mocki trzymac w `shared/data`.
5. Modele trzymac w `shared/models` lub pozniej w module domenowym.
6. UI funkcji trzymac w `features/<feature>`.
7. Po zmianie uruchomic:

```powershell
dart format lib test
flutter analyze
flutter test
```

## Szybki start

Wejscie do projektu:

```powershell
cd C:\Users\micha\developer\ClimbBetter\frontend-mobile
```

Sprawdzenie bez telefonu:

```powershell
flutter pub get
flutter analyze
flutter test
```

Uruchomienie w Chrome:

```powershell
flutter run -d chrome
```

Uruchomienie w Edge:

```powershell
flutter run -d edge
```

Sprawdzenie targetow:

```powershell
flutter devices
```

Build web:

```powershell
flutter build web
```

Zatrzymanie aplikacji:

- `q` w terminalu,
- albo `Ctrl + C`,
- jesli terminal zapyta `Terminate batch job (Y/N)?`, wpisac `Y` i Enter.

## Android SDK i emulator

Status: Todo.

Obecnie `flutter doctor -v` pokazuje, ze Flutter/Dart i Java 17 sa gotowe, ale
Android SDK nie jest jeszcze wykryty.

Kroki:

1. Pobrac Android Studio: `https://developer.android.com/studio`.
2. W Android Studio otworzyc `SDK Manager`.
3. Zainstalowac Android SDK, Platform-Tools, Command-line Tools i Android Emulator.
4. Uruchomic:

```powershell
flutter doctor --android-licenses
flutter doctor -v
```

5. Utworzyc emulator w Android Studio przez `Device Manager`.
6. Po starcie emulatora:

```powershell
flutter devices
flutter run -d <device_id>
```

## Struktura kodu

Status: Done jako pierwszy refaktor.

```txt
lib/
  main.dart
  app/
    app_theme.dart
    climbbetter_app.dart
    climbbetter_shell.dart
  shared/
    data/
    models/
    widgets/
  features/
    home/
    profile/
    training/
```

Zasada:

- `app` sklada aplikacje,
- `features` trzyma funkcje/ekrany,
- `shared` trzyma wspolne modele, mocki i widgety.

## DONE

- Utworzony projekt Flutter w `frontend-mobile`.
- Dodane platformy Android, iOS i web.
- Board pokazuje ostatnie treningi.
- `Ty` pokazuje metryki, kalendarz i wykres obciazenia.
- Karty treningow maja menu `...` z akcja `Ponow trening`.
- Wykonany pierwszy refaktor: `main.dart` jest maly, kod jest w `app`, `features`, `shared`.
- Dodane testy widgetowe.
- `flutter analyze` przechodzi.
- `flutter test` przechodzi.
- `flutter build web` przechodzi.

## Pytania otwarte

- Czy nazwa aktywnej akcji ma byc `Log done session`, `Log whole session` czy polska nazwa?
- Jak dokladnie liczymy punkty z wyceny, EDL, stylu i wykonanych ruchow?
- Czy rozgrzewka ma byc osobnym typem wpisu, czy stylem wpisu?
- Jaka lokalna baza: Drift, Isar, SQLite czy Hive?
- Czy mobile startuje bez logowania, czy wymaga auth?
