package pl.climbbetter.identity.internal;

import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CurrentUserController.class)
class IdentityApiExceptionHandler {
    @ExceptionHandler(IdentityAccessException.class)
    ResponseEntity<ProblemDetail> handle(IdentityAccessException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(exception.status()), exception.getMessage());
        detail.setProperty("code", exception.code());
        return ResponseEntity.status(exception.status()).body(detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> invalidProfile() {
        return handle(new IdentityAccessException(400, "INVALID_DISPLAY_NAME", "Name must contain between 1 and 80 characters."));
    }
}
