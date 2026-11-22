# Adjusted Load v3 — implementacja poziomu odniesienia

Aktualizacja: 2026-11-09. Wdrożone 2026-11-04, DEC-047, Flyway V23. [Pełne wzory i parametry](../business/grading-and-scoring.md#poziom-odniesienia-v3) są jednym źródłem reguł.

`RecencyWeight` liczy wagę, `RecencyWeightedLevelEstimator` zbiera masę dowodów, `ClimberLevelResolver.resolveWeighted` wybiera historię i wyjątek małej próbki, a `RelativeEffortInterpolator` obsługuje ułamkową różnicę. `TrainingSessionService` łączy odniesienie z istniejącą intensywnością profili/cruxów.

`RecencyLoadSettings`: `horizonDays=730`, `includeTopRope=false`, `weightSmallSample=false`. Wersja estymatora zapisuje również przełączniki kwalifikacji. Powtórzenie odświeża najnowszy dowód Climb; raportowa piramida celowo wybiera pierwsze przejście w swoim okresie.

Snapshot v3 zapisuje efektywny indeks/różnicę, źródło, wersję estymatora, horyzont, sample size i masę historii. Całkowity `currentLevelIndex` pozostaje podłogą indeksu dla zgodności. WARMUP i v1/v2 nie wymagają nowych metadanych. [Kolumny](database.md), [API](api.md).

Nowy POST/PUT stosuje v3. GET i identyczny retry nie używają dzisiejszej daty do ponownych obliczeń. PUT wyklucza poprzednie wpisy tej sesji z historii; dodanie sesji wstecz nie przelicza dalszych zapisów. Wycen i punktów nie postarzamy. Pyramid Support nie zasila tego obliczenia; Banister i trwały `UserGradeReference` nie są wdrożone.

V23 dodała strukturę bez zmiany starych wyników. Osobno wykonany replay z 2026-11-04 przeliczył 16 sesji/133 wpisy, zachowując wejścia i oryginalną historię. Nie jest zadaniem oczekującym ani operacją do ponowienia przy auth. [Kopia i stan odtworzenia](training-history-export.md), [dowody testów](backend-verification.md).
