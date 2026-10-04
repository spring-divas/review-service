package org.spring.divas.review.feature.order;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ResilientOrderClient {

  private final OrderClient orderClient;

  @Retry(name = "orderClient")
  @CircuitBreaker(name = "orderClient")
  @Bulkhead(name = "orderClient")
  public boolean hasUserOrderedDish(Long userId, Long dishId) {
    return orderClient.hasUserOrderedDish(userId, dishId);
  }

  @Retry(name = "orderClient")
  @CircuitBreaker(name = "orderClient")
  @Bulkhead(name = "orderClient")
  public boolean hasUserBeenToVenue(Long userId, Long venueId) {
    return orderClient.hasUserBeenToVenue(userId, venueId);
  }
}
