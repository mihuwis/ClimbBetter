package pl.climbbetter.training.internal;

public final class TrainingSessionTimeConflictException extends RuntimeException {

    TrainingSessionTimeConflictException(Throwable cause) {
        super("You already have a session during this time. Change the start or end time.", cause);
    }
}
