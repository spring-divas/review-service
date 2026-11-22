package org.spring.divas.review.feature.order;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ResilientOrderClient {

  private final OrderClient orderClient;

  @Retry(name = "orderClient")
  @CircuitBreaker(name = "orderClient", fallbackMethod = "orderCheckFallback")
  @Bulkhead(name = "orderClient")
  public boolean hasUserOrderedDish(Long userId, Long dishId) {
    return orderClient.hasUserOrderedDish(userId, dishId);
  }

  @Retry(name = "orderClient")
  @CircuitBreaker(name = "orderClient", fallbackMethod = "orderCheckFallback")
  @Bulkhead(name = "orderClient")
  public boolean hasUserBeenToVenue(Long userId, Long venueId) {
    return orderClient.hasUserBeenToVenue(userId, venueId);
  }

  private boolean orderCheckFallback(Long userId, Long targetId, Throwable throwable) {
    log.warn("Order check fallback activated for userId={}, targetId={}: {}",
        userId, targetId, throwable.getMessage());
    return false;
  }
}
