# Osiągnięcia i piramida

Aktualizacja: 2026-11-09. Reguły DEC-045 i DEC-032, wdrożone w backendzie i webie. Źródła: `AchievementEligibility`, `PyramidLevelEstimator`, raport `reporting`; [kontrakt](../documentation/achievement-pyramid-backend.md).

## Kwalifikacja i reprezentant

Osiągnięcie to ASCENT balda/obwodu albo drogi z LEAD. TOP_ROPE, próby i rozgrzewki nie wchodzą do piramidy, maksimów/personal bests ani Fast RP. Wędka zachowuje ruchy, intensywność, load i kontakty. Reguła obejmuje również odczyt dawnej historii, bez przepisywania jej wyników.

Piramida ma 3 widoki (baldy, drogi/obwody, wspólne punkty), 2 okresy (all-time, ostatnie 2 lata kalendarzowe względem `asOf`) i 2 style: RP obejmuje OS/Flash/RP, OS tylko OS — 12 kombinacji. Obie granice dat są włączne; all-time również kończy się na `asOf`.

Najpierw filtr okresu/stylu i kwalifikacja, potem deduplikacja po `climbId`. Reprezentantem jest pierwsze kwalifikujące przejście w wybranym widoku, z jego snapshotem wyceny. Powtórzenia nie zwiększają liczności, aktualny katalog nie zastępuje snapshotu. Archiwizacja Climb nie usuwa osiągnięcia.

Wspólne punkty nie łączą indeksów Font/French. Różne kalibracje są osobnymi grupami, bez wspólnego maksimum; widok łączny nie ma estymatora poziomu. Liczności warstw pokazują fakty, bez limitowania ich do kształtu idealnej piramidy.

## Pyramid Support

Informacyjny poziom dla pojedynczej skali, zależny od okresu i stylu raportu:

```text
support(G) = min(countAtOrAbove(G), 2) × 1.00
           + min(countExactly(G - 1), 4) × 0.50
           + min(countExactly(G - 2), 8) × 0.25
```

Wybieramy najwyższe G nieprzekraczające maksimum, ze wsparciem ≥3. Kolejne indeksy to stopnie słownika, nie pełne stopnie przesunięcia cruxa. Brak osiągnięć → NO_ASCENTS/UNASSESSED. Próbka 1–9 → LOW_SAMPLE_MAX/LOW. Od 10: PYRAMID_SUPPORT, confidence HIGH dopiero przy support ≥4,50 i dowodach we wszystkich 3 warstwach, inaczej MEDIUM. Brak kandydata → PEAK_FALLBACK/LOW.

Kontrola: 1×7a + 3×6c + 2×6b+ + 5×6b → 6c+ / 3,00; 5×6c + 3×7a + 1×7a+ + 2×7b + 1×7b+ → 7b / 3,25. Użytkownik odrzucił wymóg pełnych warstw 1+2+4+8; analiza jest w archiwum.

Ten estymator nie zasila Adjusted Load. V3 ma oddzielny [poziom z aktualnością dowodów](grading-and-scoring.md#poziom-odniesienia-v3): najnowszy reprezentant i horyzont 730 dni. Piramida zachowuje pierwszego reprezentanta i 2 lata kalendarzowe.

Personal best `rp` oznacza dokładnie RP; szersze znaczenie RP dotyczy piramidy. Koło stylów miesiąca/roku wybiera najlepszy OS > Flash > RP raz na wspinaczkę, według [osobnej reguły](../documentation/period-training-summary.md). Recent personal best ma okno 12 miesięcy, niezależne od piramidy.
