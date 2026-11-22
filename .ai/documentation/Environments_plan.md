# Environments plan — dev local i production

Aktualizacja: 2026-11-09. **Plan produkcji, bez zakupu i wdrożenia.** DEC-049: lokalny dev + płatna produkcja dla pierwszych 5–10 zaproszonych osób, bez QA/staging. Użytkownik akceptuje samodzielne utrzymanie; utrata danych przy awarii serwera jest niedopuszczalna.

## Co jest przygotowane

Local działa: Java/Vite, PostgreSQL Compose, Google/Firebase dev, profil i mapowanie Mihu oraz osobny emulator z bazą modeli. [Uruchomienie](development.md), [auth](firebase-authentication-local.md).

Produkcja wymaga Dockerfile web/API, konfiguracji serwowania SPA i `/api`, proxy HTTPS, konfiguracji prod/Firebase, izolacji seedów local, backupów/replikacji i procesu wydania. W repo nie ma workflowów `.github/workflows`; obecne pliki `.github/modernize` nie stanowią CI/CD. Compose uruchamia tylko PostgreSQL.

## Hosting i ochrona danych

Zachowana propozycja: dwa małe VPS w różnych centrach UE (aplikacja + primary, drugi z synchronicznym standby) i niezależny szyfrowany backup poza dostawcą. Alternatywa: VPS aplikacji + zarządzana trwała baza, np. Neon. Jeden VPS z okresowym backupem może utracić ostatnie zapisy przy awarii i nie spełnia wymogu. Dostawca, rozmiar i zakup pozostają w OPEN-21. [Porównanie cen z 2026-11-08](../archive/documentation-2026-11-09/snapshot/.ai/documentation/Environments_plan.md#2-sensowne-tanie-opcje) jest historyczne; budżet wymaga aktualnej oferty wraz z odnowieniem, domeną i backupem.

Proponowany cel **RPO=0 dotyczy potwierdzonych zapisów przy utracie jednego VPS i sprawnej drugiej kopii**. Nie jest wdrożoną gwarancją ani ochroną przed każdym rodzajem utraty danych. Mechanizm i dopuszczalna przerwa wymagają zatwierdzenia (OPEN-23):

- PostgreSQL 17: wymagany synchroniczny standby, `synchronous_commit=on`, `fsync=on`; API potwierdza sukces dopiero po commit i trwałym WAL na obu serwerach. Szyfrowane łącze i pomiar opóźnienia. [Reguły PostgreSQL](https://www.postgresql.org/docs/17/warm-standby.html#SYNCHRONOUS-REPLICATION).
- Brak repliki blokuje potwierdzanie zapisów, bez cichego przejścia na async. Timeout pozostawia wynik niepewny; szkic i stabilne retry muszą przetrwać.
- Failover ręczny: odciąć stary primary, promować sprawną kopię, przenieść aplikację. Wznowić nowe zapisy po odbudowie drugiej synchronicznej kopii. Dwa VPS nadal tworzą jedno środowisko prod.
- Replika powiela błędne usunięcia. Osobno pgBackRest: pełny backup tygodniowo, różnicowy codziennie, ciągły WAL, szyfrowanie, retencja pełnych łańcuchów min. 30 dni i klucz poza serwerami. Niezależny magazyn UE, np. B2; dodatkowy eksport z Object Lock wymaga sprawdzenia zgodności. [pgBackRest](https://pgbackrest.org/user-guide.html).
- Alarmy: replikacja, backup/WAL, dysk i zatrzymany WAL. Przed startem próby awarii/restore, potem miesięczny restore w izolacji. Przy utracie obu kopii/błędzie operatora odzysk zależy od backupu; proponowany lag WAL ≤5 min wymaga pomiaru.

## GitHub Actions i wydanie — projekt procesu

Nazwy poniżej są propozycją plików do utworzenia, nie istniejącą konfiguracją:

| Etap                  | Zakres                                                                                                                                                                                                                                    |
| --------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ci.yml`: PR/push     | Java 21 + Maven verify z PostgreSQL Testcontainers; Node 22 + npm ci, lint, model, build i kontrolowane Playwright; osobno auth SDK/emulator. Dostosować instalację `msedge` i start emulatora do runnera. Bez danych i poświadczeń prod. |
| Budowa wydania        | Po zielonych kontrolach budowa jednego obrazu web/API, lokalnie sprawdzonego także z routingiem SPA; publikacja do GHCR z identyfikatorem commita i zachowanym digestem. Wdrożenie dokładnie tego artefaktu.                              |
| `deploy.yml`: ręcznie | `workflow_dispatch`, wybór zatwierdzonego digestu i zabezpieczenie przed równoległymi wydaniami. Dostępy prod tylko w kroku wydania; PR nie uruchamia wdrożenia.                                                                          |
| Migracja i restart    | Sprawdzić backup/replikację; migrację wykonać raz osobną rolą, potem zmienić obraz. Zgodność starej/nowej aplikacji ze schematem; lokalna próba pustej bazy i upgrade przed wydaniem.                                                     |
| Odbiór i rollback     | Health, Google Sign-In, profil, zapis/odczyt na koncie testowym, izolacja dwóch kont i oba raporty. Poprzedni digest zachowany; cofnięcie obrazu nie cofa migracji ani danych.                                                            |

Ręczny workflow musi istnieć na domyślnej gałęzi. GHCR obsługuje publikację z `GITHUB_TOKEN` i pobranie dokładnej wersji przez digest. Szczegóły ról, transport na VPS i przechowywanie sekretów ustalimy przy implementacji. Źródła: [workflow_dispatch](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow), [GHCR i digest](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry).

## Warunki pierwszego udostępnienia

- [ ] Wybrany hosting, pełny koszt, cel odzyskania i operator alarmów (OPEN-21/23/25).
- [ ] Działający obraz/proxy HTTPS, firewall/SSH na klucz, baza bez publicznego portu; runtime bez superusera DB/Docker socket, osobna rola migratora.
- [ ] Oddzielny Firebase prod, backend weryfikuje projekt prod; brak stub/emulatora i seedów `R__local_data.sql`. Sekrety poza Git/obrazem, MFA operatorów.
- [ ] Zaproszone tożsamości kontrolowane przez backend; test A/B zabrania odczytu i zmian cudzych danych. ADMIN ma osobno ustalony zakres (OPEN-22), Mihu pozostaje USER.
- [ ] Potwierdzony zapis przeżywa utratę primary; brak repliki blokuje nowe potwierdzenia; failover i odbudowa sprawdzone. Udany restore backupu/WAL i alarmy.
- [ ] Przećwiczone wydanie/cofnięcie aplikacji, domena, kontakt, prywatność/retencja oraz eksport/usunięcie konta. Baza/backup UE nie oznaczają danych Auth wyłącznie w UE; dotychczasowy plan opisuje Firebase Auth jako przetwarzanie w USA ([źródło](https://firebase.google.com/support/privacy)).

Płatności wyłączone w pierwszej grupie. Operator i zasady dostępu pozostają w OPEN-24. Rejestr wszystkich otwartych ustaleń: [decisions](decisions.md).
