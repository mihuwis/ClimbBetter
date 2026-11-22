package pl.climbbetter.reporting.internal.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.DateTimeException;

@RestControllerAdvice(assignableTypes = PeriodSummaryController.class)
class PeriodSummaryApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, DateTimeException.class,
            MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ProblemDetail> handleInvalidRequest(Exception exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid training summary request");
        problem.setProperty("code", "INVALID_PERIOD_SUMMARY_REQUEST");
        return ResponseEntity.badRequest().body(problem);
    }
}
