package org.spring.divas.review.common.exception;

import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ResourceAlreadyExistsException.class)
  public ProblemDetail handleAlreadyExists(ResourceAlreadyExistsException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(ResourceAccessDeniedException.class)
  public ProblemDetail handleAccessDenied(ResourceAccessDeniedException ex) {
    return problem(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    return problem(HttpStatus.CONFLICT, "Request conflicts with existing data");
  }

  @ExceptionHandler(ResourceAccessException.class)
  public ProblemDetail handleExternalServiceUnavailable(ResourceAccessException ex) {
    if (ex.contains(HttpTimeoutException.class)) {
      return problem(HttpStatus.GATEWAY_TIMEOUT, "External service did not respond in time");
    }
    return problem(HttpStatus.SERVICE_UNAVAILABLE, "External service is unavailable");
  }

  @ExceptionHandler(RestClientResponseException.class)
  public ProblemDetail handleExternalServiceError(RestClientResponseException ex) {
    return problem(HttpStatus.BAD_GATEWAY, "External service returned an error");
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred");
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

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      @NonNull Exception ex, Object body, @NonNull HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    if (body instanceof ProblemDetail problem) {
      problem.setProperty("timestamp", Instant.now());
    }
    return super.handleExceptionInternal(ex, body, headers, status, request);
  }

  private ProblemDetail problem(HttpStatus status, String detail) {
    var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    problemDetail.setTitle(status.getReasonPhrase());
    problemDetail.setProperty("timestamp", Instant.now());
    return problemDetail;
  }
}
