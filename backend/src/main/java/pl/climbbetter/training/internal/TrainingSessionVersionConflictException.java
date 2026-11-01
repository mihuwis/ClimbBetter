package pl.climbbetter.training.internal;

public class TrainingSessionVersionConflictException extends RuntimeException {

    public TrainingSessionVersionConflictException() {
        super("Training session was changed; reload it before editing");
    }
}
