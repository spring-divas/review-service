package org.spring.divas.review.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void mapsNotFoundTo404() {
    ProblemDetail problem = handler.handleNotFound(new ResourceNotFoundException("Not here"));

    assertProblem(problem, HttpStatus.NOT_FOUND, "Not here");
  }

  @Test
  void mapsAlreadyExistsTo409() {
    ProblemDetail problem =
        handler.handleAlreadyExists(new ResourceAlreadyExistsException("Already there"));

    assertProblem(problem, HttpStatus.CONFLICT, "Already there");
  }

  @Test
  void mapsAccessDeniedTo403() {
    ProblemDetail problem =
        handler.handleAccessDenied(new ResourceAccessDeniedException("Not yours"));

    assertProblem(problem, HttpStatus.FORBIDDEN, "Not yours");
  }

  @Test
  void mapsDataIntegrityViolationTo409WithoutDetails() {
    ProblemDetail problem =
        handler.handleDataIntegrityViolation(
            new DataIntegrityViolationException("duplicate key value violates constraint"));

    assertProblem(problem, HttpStatus.CONFLICT, "Request conflicts with existing data");
  }

  @Test
  void mapsUpstreamTimeoutTo504() {
    ProblemDetail problem =
        handler.handleExternalServiceUnavailable(
            new ResourceAccessException("I/O error", new HttpTimeoutException("timed out")));

    assertProblem(problem, HttpStatus.GATEWAY_TIMEOUT, "External service did not respond in time");
  }

  @Test
  void mapsUpstreamIoErrorTo503() {
    ProblemDetail problem =
        handler.handleExternalServiceUnavailable(
            new ResourceAccessException("I/O error", new IOException("connection refused")));

    assertProblem(problem, HttpStatus.SERVICE_UNAVAILABLE, "External service is unavailable");
  }

  @Test
  void mapsOpenCircuitTo503() {
    CallNotPermittedException ex =
        CallNotPermittedException.createCallNotPermittedException(
            CircuitBreaker.ofDefaults("orderClient"));

    ProblemDetail problem = handler.handleExternalServiceRejected(ex);

    assertProblem(problem, HttpStatus.SERVICE_UNAVAILABLE, "External service is unavailable");
  }

  @Test
  void mapsFullBulkheadTo503() {
    BulkheadFullException ex =
        BulkheadFullException.createBulkheadFullException(Bulkhead.ofDefaults("orderClient"));

    ProblemDetail problem = handler.handleExternalServiceRejected(ex);

    assertProblem(problem, HttpStatus.SERVICE_UNAVAILABLE, "External service is unavailable");
  }

  @Test
  void mapsSecurityAccessDeniedTo403() {
    ProblemDetail problem = handler.handleSecurityAccessDenied(
        new AccessDeniedException("Access Denied"));

    assertProblem(problem, HttpStatus.FORBIDDEN, "Access Denied");
  }

  @Test
  void mapsRateLimitTo429() {
    RequestNotPermitted ex =
        RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("orderClient"));

    ResponseEntity<ProblemDetail> response = handler.handleRateLimitExceeded(ex);

    assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
    assertProblem(
        response.getBody(), HttpStatus.TOO_MANY_REQUESTS,
        "Rate limit exceeded. Please try again later.");
  }

  @Test
  void mapsUpstream4xxTo500() {
    ProblemDetail problem = handler.handleExternalServiceError(responseException(400));

    assertProblem(
        problem, HttpStatus.INTERNAL_SERVER_ERROR, "External service rejected the request");
  }

  @Test
  void mapsUpstream5xxTo502() {
    ProblemDetail problem = handler.handleExternalServiceError(responseException(503));

    assertProblem(problem, HttpStatus.BAD_GATEWAY, "External service returned an error");
  }

  @Test
  void mapsUnexpectedErrorTo500WithoutDetails() {
    ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("secret internals"));

    assertProblem(problem, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred");
  }

  private static RestClientResponseException responseException(int status) {
    return new RestClientResponseException(
        "upstream error", HttpStatusCode.valueOf(status), "status " + status, null, null, null);
  }

  private static void assertProblem(ProblemDetail problem, HttpStatus status, String detail) {
    assertThat(problem.getStatus()).isEqualTo(status.value());
    assertThat(problem.getTitle()).isEqualTo(status.getReasonPhrase());
    assertThat(problem.getDetail()).isEqualTo(detail);
    assertThat(problem.getProperties()).containsKey("timestamp");
  }
}
