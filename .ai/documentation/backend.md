# Backend Java

Aktualizacja: 2026-11-09. Stan potwierdzony odczytem `backend/src/main`; [datowane wyniki testów](backend-verification.md).

Java 21, Spring Boot 4.1.0, Maven Wrapper, Spring Modulith, Spring Security, Firebase Admin, PostgreSQL/Flyway i springdoc. `ddl-auto=validate`; schemat zmieniają migracje, nie Hibernate.

Wdrożone:

- Firebase ID token → UUID użytkownika, bootstrap i profil. Domyślny tryb `firebase`; `stub` i `emulator` wyłącznie z profilem local/test.
- Atomowy POST sesji, Details, zakres dat, PUT z wersją/powodem i DELETE z audytem. Identyczny retry czyta wynik; zmienione dane pod użytym ID dają konflikt.
- Od 2026-11-15 zapis i edycja chronią czas sesji według [DEC-053](decisions.md); kolizja daje `409 SESSION_TIME_CONFLICT`, a bazę chroni V25.
- Katalog własny/publiczny, szybkie prywatne obiekty, samodzielny zapis Climb, sektory, metry i aliasy Kurtyki.
- Kalkulatory profili/cruxów oraz poziom odniesienia v3. Historyczne v1/v2 pozostają czytelne.
- Dashboard/personal bests, piramida z listą i estymacją informacyjną, raport miesiąca/roku.

`TrainingSessionService` koordynuje zapis; resolvery dostarczają katalog, historię i reguły, a `TrainingMetricsFacade` liczy wyniki. Edycja wyklucza stare wpisy tej sesji z własnej historii, przelicza agregat i zachowuje rewizję. Błąd wycofuje transakcję. Usunięta lub cudza sesja jest niedostępna w odczytach.

Poza wdrożonym zakresem: ADMIN, płatności, serwerowy preview, trwały `UserGradeReference`, masowa reewaluacja, produkcyjny runtime i CI/CD.

[API](api.md) · [baza](database.md) · [model obliczeń](../business/grading-and-scoring.md) · [uruchomienie](development.md).
