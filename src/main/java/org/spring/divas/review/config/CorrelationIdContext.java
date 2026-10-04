package org.spring.divas.review.config;

import java.util.Optional;

public final class CorrelationIdContext {

  public static final String HEADER = "X-Correlation-Id";

  private static final ThreadLocal<String> CORRELATION_ID =
      new ThreadLocal<>();

  private CorrelationIdContext() {
  }

  public static void set(String id) {
    CORRELATION_ID.set(id);
  }

  public static Optional<String> get() {
    return Optional.ofNullable(CORRELATION_ID.get());
  }

  public static void clear() {
    CORRELATION_ID.remove();
  }
}
