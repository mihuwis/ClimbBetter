package pl.climbbetter.catalog.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CatalogController.class)
class CatalogApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleInvalidClimb(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid catalog climb");
        problem.setProperty("code", "INVALID_CLIMB");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(CatalogWriteService.ClientClimbIdConflictException.class)
    ResponseEntity<ProblemDetail> handleClientClimbIdConflict(
            CatalogWriteService.ClientClimbIdConflictException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage()
        );
        problem.setTitle("Client climb ID conflict");
        problem.setProperty("code", "CLIENT_CLIMB_ID_CONFLICT");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
