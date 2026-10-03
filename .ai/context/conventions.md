# Konwencje projektu

Status: zebrane ustalenia i obowiązujące nazwy; 2026-10-03.

## Nazwy i język

Kod i identyfikatory są po angielsku. Dokumentację objaśniamy po polsku, zachowując oryginalne nazwy domeny i API. Nowe pliki dokumentacyjne używają angielskiego `kebab-case`. Nie zmieniamy nazw historycznych plików archiwum.

| Nazwa kanoniczna                                                    | Znaczenie / rozróżnienie                                                   |
| ------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `TrainingSession`, `TrainingEntry`                                  | sesja i jedna próba/przejście/rozgrzewka --------------------------------- |
| `Area`, `Sector`, `Climb`                                           | miejsce, opcjonalny sektor, stabilna tożsamość wspinaczki ---------------- |
| `climbType`, `effortProfileId`                                      | dyscyplina i profil wysiłku to oddzielne pojęcia ------------------------- |
| `baseEdl`, `edlCount`, `totalMoves`, `executedMoves`                | cztery różne wartości, EDL nie jest długością drogi ---------------------- |
| `resultType`, `ascentMode`, `familiarityBand`                       | rezultat, deklarowany tryb i znajomość wyprowadzana z historii ----------- |
| `moveIntensity`, `classicLoad`, `adjustedLoad`                      | jawne metryki zamiast niejednoznacznych `score`, `points`, `LengthDivisor` |
| `sessionDate`, `timeZoneId`                                         | lokalna data treningowa i strefa IANA ------------------------------------ |
| `clientSessionId`, `clientEntryId`, `clientClimbId`, `clientAreaId` | stabilne identyfikatory klienta, niezależne od nazw ---------------------- |
| `entryOrder`, `calculationModelVersion`, `version`                  | kolejność domenowa, wersja reguł i wersja rekordu ------------------------ |

## Granice odpowiedzialności

Backend wylicza wyniki i zapisuje snapshoty. Frontendy wyświetlają dane, przechowują szkic i formatują wartości. DTO HTTP, model domeny, encja persystencji i model widoku mają oddzielne role. Sesja należy do `training`; model karty Dashboardu nie definiuje sesji.

W Javie docelowo pakiety zaczynają się od `pl.climbbetter`; obecne moduły to `identity`, `catalog`, `training`, `reporting`, `shared`. Planu modułów nie przedstawiamy jako wdrożonej logiki domenowej. Szczegóły: [architektura](../documentation/architecture.md).

## Dane i historia

Wyniki dziesiętne obliczamy według [modelu wyceny](../business/grading-and-scoring.md), bez `double` jako źródła zapisanych wyników. Lokalnej daty sesji nie przesuwamy przez aktualną strefę urządzenia. Zmiana katalogu nie przepisuje historycznych wyników. Korekta historii wymaga jawnej operacji i audytu.

## Praca z dokumentacją

Każdy istotny dokument określa datę, status i źródła. Decyzje zachowują swoje ID; ich zmiany mają uzasadnienie i odsyłacz do zastąpionego ustalenia. Bieżące luki trafiają do [decisions](../documentation/decisions.md), nie do niezależnych list pytań w każdym pliku. Sekrety i lokalne wartości połączeń nie trafiają do dokumentacji.

## Formatowanie tabel Markdown

Preferencja użytkownika z 2026-10-03, na podstawie tabeli „Od czego zacząć” w `.ai/README.md`:

- W tworzonych i aktualizowanych tabelach wyrównujemy znaki `|` pionowo, aby tabela była czytelna również w widoku tekstowym na pełnym ekranie.
- Szerokość każdej kolumny dopasowujemy do jej najdłuższej komórki; w źródle Markdown uwzględniamy całą składnię linków i formatowania.
- Krótsze komórki opisowe możemy dopełniać ciągiem `-`, zgodnie z przykładem użytkownika. Wypełnienie oddzielamy spacją od treści i umieszczamy poza linkami oraz fragmentami kodu. Liczby, daty i samodzielne identyfikatory wyrównujemy spacjami, zachowując czytelność wartości.
- Nagłówki i wiersz separatorów dopasowujemy do tych samych szerokości. Przy zmianie najdłuższego wpisu wyrównujemy całą tabelę.
- Formatowanie nie zmienia treści ustaleń ani adresów linków. Historyczne kopie w `archive/source-snapshot` pozostają niezmienione.
