package org.spring.divas.review.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ResourceAlreadyExistsException.class)
  public ProblemDetail handleAlreadyExists(ResourceAlreadyExistsException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(ResourceAccessDeniedException.class)
  public ProblemDetail handleAccessDenied(ResourceAccessDeniedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
        "Request conflicts with existing data");
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
        "Unexpected error occurred");
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status,
      WebRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(error.getField(), error.getDefaultMessage());
    }

    ProblemDetail problem = ex.getBody();
    problem.setDetail("Validation failed");
    problem.setProperty("errors", errors);
    return handleExceptionInternal(ex, problem, headers, status, request);
  }
}
