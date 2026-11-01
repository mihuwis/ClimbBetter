package pl.climbbetter.training.internal.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.climbbetter.training.internal.AscentModeHistoryConflictException;
import pl.climbbetter.training.internal.TrainingSessionConflictException;
import pl.climbbetter.training.internal.TrainingSessionVersionConflictException;

@RestControllerAdvice(
        assignableTypes = TrainingSessionController.class
)
class TrainingApiExceptionHandler {

    @ExceptionHandler(AscentModeHistoryConflictException.class)
    ResponseEntity<ProblemDetail> handleAscentModeHistoryConflict(
            AscentModeHistoryConflictException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle(
                "Ascent mode conflicts with climb history"
        );
        problem.setProperty(
                "code",
                "ASCENT_MODE_HISTORY_CONFLICT"
        );
        problem.setProperty(
                "clientEntryId",
                exception.clientEntryId()
        );
        problem.setProperty(
                "ascentMode",
                exception.ascentMode()
        );
        problem.setProperty(
                "confirmationRequired",
                true
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }

    @ExceptionHandler(TrainingSessionVersionConflictException.class)
    ResponseEntity<ProblemDetail> handleVersionConflict(
            TrainingSessionVersionConflictException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage()
        );
        problem.setTitle("Training session version conflict");
        problem.setProperty("code", "SESSION_VERSION_CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(TrainingSessionConflictException.class)
    ResponseEntity<ProblemDetail> handleSessionConflict(
            TrainingSessionConflictException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Training session conflict");
        problem.setProperty(
                "code",
                "CLIENT_SESSION_ID_CONFLICT"
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleInvalidSession(
            IllegalArgumentException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );

        problem.setTitle("Invalid training session");
        problem.setProperty(
                "code",
                "INVALID_TRAINING_SESSION"
        );

        return ResponseEntity
                .badRequest()
                .body(problem);
    }
}
