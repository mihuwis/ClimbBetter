package pl.climbbetter.training.internal.model;

record MoveCount(
        int totalMoves,
        int executedMoves
) {

    MoveCount {
        if (totalMoves <= 0) {
            throw new IllegalArgumentException(
                    "Total moves must be positive"
            );
        }

        if (executedMoves < 0) {
            throw new IllegalArgumentException(
                    "Executed moves cannot be negative"
            );
        }
    }
}