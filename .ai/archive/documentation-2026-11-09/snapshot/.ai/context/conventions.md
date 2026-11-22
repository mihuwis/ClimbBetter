# Konwencje projektu

Status: zebrane ustalenia i obowiązujące nazwy; aktualizacja 2026-11-04 o snapshot poziomu odniesienia v3.

## Nazwy i język

Kod i identyfikatory są po angielsku. Dokumentację objaśniamy po polsku, zachowując oryginalne nazwy domeny i API. Nowe pliki dokumentacyjne używają angielskiego `kebab-case`. Nie zmieniamy nazw historycznych plików archiwum.

| Nazwa kanoniczna                                                    | Znaczenie / rozróżnienie                                                   |
| ------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `TrainingSession`, `TrainingEntry`                                  | sesja i jedna próba/przejście/rozgrzewka --------------------------------- |
| `Area`, `Sector`, `Climb`                                           | miejsce, opcjonalny sektor, stabilna tożsamość wspinaczki ---------------- |
| `climbType`, `effortProfileId`                                      | dyscyplina i profil wysiłku to oddzielne pojęcia ------------------------- |
| `baseEdl`, `edlCount`, `totalMoves`, `executedMoves`                | cztery różne wartości, EDL nie jest długością drogi ---------------------- |
| `resultType`, `ascentMode`, `familiarityBand`                       | rezultat, deklarowany tryb i znajomość wyprowadzana z historii ----------- |
| `protectionMode`                                                    | sposób asekuracji `LEAD` albo `TOP_ROPE`; oddzielny od OS/Flash/RP ------- |
| `gradeIndex`, `gradePoints`, `gradeCalibrationVersion`              | indeks wewnątrz skali, wspólna kalibracja loadu i wersja tej kalibracji -- |
| `gradeSystemCode`, `inputGradeLabel`, `canonicalGradeId`            | system i etykieta użytkownika oraz kanoniczna wycena użyta w obliczeniach |
| `moveIntensity`, `classicLoad`, `adjustedLoad`                      | jawne metryki zamiast niejednoznacznych `score`, `points`, `LengthDivisor` |
| `sessionDate`, `timeZoneId`                                         | lokalna data treningowa i strefa IANA ------------------------------------ |
| `clientSessionId`, `clientEntryId`, `clientClimbId`, `clientAreaId` | stabilne identyfikatory klienta, niezależne od nazw ---------------------- |
| `entryOrder`, `calculationModelVersion`, `version`                  | kolejność domenowa, wersja reguł i wersja rekordu ------------------------ |

Aktualizacja DEC-043–044 z 2026-11-01: wdrożony model v2 używa `profileIntensityIndex` jako wagi istniejącego `EffortProfile`. Dla cruxów snapshot zawiera `hardMoveCount`, `easyMoveCount`, `hardMoveIntensity`, `easyMoveIntensity` i etykiety części. Nazwy EDL w powyższej tabeli dotyczą wyłącznie historycznego v1; kalkulator EDL został usunięty. `totalMoves` i `executedMoves` zachowują znaczenie. Przy wdrożeniu v2 nowe zapisy i edycje liczyły v2, a API odróżniało wersję wyniku. [Model](../business/grading-and-scoring.md#zatwierdzona-zmiana-v2--edl-zastępujemy-przez-profileintensityindex).

Aktualizacja v3 z 2026-11-04: `effectiveCurrentLevelIndex` i `effectiveRelativeGradeDifference` oznaczają ciągły poziom odniesienia i rzeczywistą różnicę względem niezmienionego `gradeIndex`. Nie są obniżoną wyceną ani punktacją drogi. `currentLevelIndex` pozostaje całkowitą podłogą tej referencji. `levelReferenceSource`, `levelEstimatorVersion`, `levelReferenceHorizonDays`, `levelReferenceSampleSize` i `levelReferenceHistoryWeight` opisują pochodzenie obliczenia; nie zastępują `calculationModelVersion` ani `gradeCalibrationVersion`. Są metadanymi odpowiedzi API, bez nowych pól formularza lub elementów UI. Nowy POST i jawny PUT zapisują v3; GET i identyczne retry zachowują historyczne snapshoty. [Zasady v3](../documentation/recency-load.md).

## Granice odpowiedzialności

Backend wylicza wyniki i zapisuje snapshoty. Frontendy wyświetlają dane, przechowują szkic i formatują wartości. DTO HTTP, model domeny, encja persystencji i model widoku mają oddzielne role. Sesja należy do `training`; model karty Dashboardu nie definiuje sesji.

W Javie docelowo pakiety zaczynają się od `pl.climbbetter`; obecne moduły to `identity`, `catalog`, `training`, `reporting`, `shared`. Planu modułów nie przedstawiamy jako wdrożonej logiki domenowej. Szczegóły: [architektura](../documentation/architecture.md).

## Dane i historia

Wyniki dziesiętne obliczamy według [modelu wyceny](../business/grading-and-scoring.md), bez `double` jako źródła zapisanych wyników. Lokalnej daty sesji nie przesuwamy przez aktualną strefę urządzenia. Zmiana katalogu nie przepisuje historycznych wyników. Korekta historii wymaga jawnej operacji i audytu.

W v3 efektywny indeks i wagi zachowują 10 miejsc dziesiętnych. Interpolowany mnożnik względnego wysiłku zaokrąglamy HALF_UP do 4 miejsc przed obliczeniem loadu, zgodnie z jego typem w bazie. Wynik ma być odtwarzalny z zapisanych wejść; odczyt nie wyznacza ponownie poziomu według dzisiejszej daty. Zanik wagi historii zmienia referencję Adjusted Load, a nie wycenę, punkty czy osiągnięcie wspinaczki.

## Praca z dokumentacją

Każdy istotny dokument określa datę, status i źródła. Decyzje zachowują swoje ID; ich zmiany mają uzasadnienie i odsyłacz do zastąpionego ustalenia. Bieżące luki trafiają do [decisions](../documentation/decisions.md), nie do niezależnych list pytań w każdym pliku. Sekrety i lokalne wartości połączeń nie trafiają do dokumentacji.

## Formatowanie tabel Markdown

Preferencja użytkownika z 2026-10-03, na podstawie tabeli „Od czego zacząć” w `.ai/README.md`:

- W tworzonych i aktualizowanych tabelach wyrównujemy znaki `|` pionowo, aby tabela była czytelna również w widoku tekstowym na pełnym ekranie.
- Szerokość każdej kolumny dopasowujemy do jej najdłuższej komórki; w źródle Markdown uwzględniamy całą składnię linków i formatowania.
- Krótsze komórki opisowe możemy dopełniać ciągiem `-`, zgodnie z przykładem użytkownika. Wypełnienie oddzielamy spacją od treści i umieszczamy poza linkami oraz fragmentami kodu. Liczby, daty i samodzielne identyfikatory wyrównujemy spacjami, zachowując czytelność wartości.
- Nagłówki i wiersz separatorów dopasowujemy do tych samych szerokości. Przy zmianie najdłuższego wpisu wyrównujemy całą tabelę.
- Formatowanie nie zmienia treści ustaleń ani adresów linków. Historyczne kopie w `archive/source-snapshot` pozostają niezmienione.
