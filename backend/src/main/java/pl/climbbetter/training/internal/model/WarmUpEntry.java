package pl.climbbetter.training.internal.model;

import java.math.BigDecimal;

record WarmUpEntry(int executedMoves) {

    private static final BigDecimal ZERO_LOAD =
            BigDecimal.ZERO.setScale(4);

    WarmUpEntry {
        if (executedMoves <= 0) {
            throw new IllegalArgumentException(
                    "Warm-up executed moves must be positive"
            );
        }
    }

    BigDecimal classicLoad() {
        return ZERO_LOAD;
    }

    BigDecimal adjustedLoad() {
        return ZERO_LOAD;
    }

    EntryContribution contribution() {
    return new EntryContribution(
            executedMoves,
            classicLoad(),
            adjustedLoad()
    );
}
}