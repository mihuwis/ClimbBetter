# Frontend mobile

Status: prototyp Flutter; odczyt plików 2026-10-03, bez uruchamiania narzędzi Flutter.

## Co istnieje

[pubspec.yaml](../../frontend-mobile/pubspec.yaml) określa aplikację `climbbetter_mobile`, Dart `^3.11.0`, Flutter, `cupertino_icons`, `flutter_test` i `flutter_lints`. Repozytorium zawiera katalogi Android, iOS oraz web. Nie jest to dowód gotowości środowisk do builda każdej platformy.

Kod `lib` jest podzielony na `app`, `features` i `shared`. Shell ma Board/Home, akcję nowego treningu i profil. Istnieje formularz `log_done_session_sheet.dart` oraz widget test.

| Element         | Obecny stan                                                                               |
| --------------- | ----------------------------------------------------------------------------------------- |
| Board --------- | korzysta z `shared/data/mock_training_sessions.dart` ------------------------------------ |
| Formularz ----- | lokalne pola i `setState`, zapis kończy się `SnackBar` ---------------------------------- |
| Profile i style | lokalne presety ------------------------------------------------------------------------- |
| Ruchy --------- | `defaultEdl` zasila długość i wykonane ruchy; trzeba rozdzielić `baseEdl` od `totalMoves` |
| Model karty --- | model prezentacyjny w `shared/models/training_session.dart` ----------------------------- |
| Integracja ---- | brak zależności klienta HTTP i lokalnej bazy; brak trwałego outboxa --------------------- |

Historyczne twierdzenia o zielonych testach i brakującym Android SDK nie były ponownie sprawdzane. Nie uznajemy ich za aktualny wynik środowiska.

## Utrzymany kierunek

Mobile ma działać offline-first. Docelowo: widget → controller → `TrainingRepository` → lokalne źródło danych / zdalne API. Board obserwuje dane lokalne, a serwer dostarcza wiążące snapshoty obliczeń. Model widoku, DTO i zapis lokalny są oddzielne.

Każda nowa sesja, próba i roboczy obiekt katalogu otrzymują stabilne client ID. UI rozróżnia szkic, oczekiwanie na synchronizację, powodzenie i konflikt. Retry nie może tworzyć duplikatów; konflikty domenowe wymagają decyzji użytkownika. „Ponów trening” tworzy nowy szkic i nowe ID, bez kopiowania historycznych wyników jako wejścia.

Opcjonalne technologie ze starszego planu — `dio`/`http`, Drift/SQLite, Riverpod, `json_serializable`/`freezed` — są propozycjami, nie przyjętymi zależnościami. Wybór i szczegóły synchronizacji są odłożone (`OPEN-17`).

## Miejsce w kolejności prac

Pierwszy użyteczny przyrost jest obecnie webowy. Mobile później skorzysta ze wspólnego zapisu ukończonej sesji, listy, Details i słowników. Pełna synchronizacja, edycja na wielu urządzeniach, tombstones i polityka konfliktów są kolejnym przyrostem; stabilne ID i snapshoty trzeba zachować już w pierwszym API.

Szczegóły biznesowe formularza muszą odpowiadać [domenie](../business/domain.md) i [wycenie](../business/grading-and-scoring.md), w szczególności oddzielnym skalom, rezultatowi, trybowi i znajomości. Starsze presety nie są źródłem prawdy.

## Weryfikacja przyszłej integracji

Sesja ma przetrwać zamknięcie aplikacji i brak sieci po wdrożeniu lokalnej trwałości. Synchronizacja po utracie odpowiedzi nie tworzy duplikatu. Testy obejmują reset aplikacji, outbox, konflikt wersji i prywatne dane różnych użytkowników. Pierwszy przyrost online musi już pokazywać wyniki pochodzące z Javy.

Źródła: [szczegółowy plan mobile](../archive/source-snapshot/docs/CB_frontend-mobile.md), [starszy plan roboczy](../archive/source-snapshot/frontend-mobile/mobile-plan.md). Pełne checklisty zachowano w archiwum, bez odhaczania ich na podstawie samego przeniesienia dokumentacji.
