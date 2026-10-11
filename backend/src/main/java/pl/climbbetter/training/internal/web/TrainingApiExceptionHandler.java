package pl.climbbetter.training.internal.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.climbbetter.training.internal.TrainingSessionConflictException;

@RestControllerAdvice(
        assignableTypes = TrainingSessionController.class
)
class TrainingApiExceptionHandler {

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
