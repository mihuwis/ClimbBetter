# Produkt ClimbBetter

Aktualizacja: 2026-11-09. Kierunek produktu i zakres potwierdzony w kodzie; decyzje w [rejestrze](../documentation/decisions.md).

ClimbBetter służy do zapisu treningów na ścianie i w skałach, analizy obciążenia i obserwacji postępów. Pierwszy klient to web, zastępujący prowadzenie historii w Excelu.

Obecny zakres: logowanie Google, nazwa profilu, zapis i korekty sesji, odzyskiwalny szkic, Dashboard, Calendar, katalog oraz Statistics. Adjusted Load jest główną metryką, Classic Load porównawczą. Piramida pokazuje osiągnięcia, a podsumowanie miesiąca/roku — wykonaną pracę.

**Jedna sesja obejmuje jeden rejon albo jedną ściankę.** Może zawierać wiele sektorów i wspinaczek tego miejsca. Inne miejsce oznacza osobną sesję, także tego samego dnia (DEC-018).

Warunek użyteczności: próba i późniejsze przejście tej samej wspinaczki zapisują się trwale, wcześniejszy kontakt wpływa na obliczenie, retry nie dubluje danych, a wyniki po ponownym otwarciu pochodzą z backendu.

Najbliższy cel to płatne utrzymanie produkcji dla 5–10 zaproszonych osób; [warunki wydania](../documentation/Environments_plan.md). Planowanie treningów, ankieta wspinaczkowa, płatności, media i społeczność są dalszym rozwojem. Flutter pozostaje prototypem; offline i synchronizacja wymagają implementacji.
