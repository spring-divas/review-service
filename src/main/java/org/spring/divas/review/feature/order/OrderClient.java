package org.spring.divas.review.feature.order;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/api/order")
public interface OrderClient {

  @GetExchange("/exists/dish")
  boolean hasUserOrderedDish(@RequestParam Long userId, @RequestParam Long dishId);

  @GetExchange("/exists/venue")
  boolean hasUserBeenToVenue(@RequestParam Long userId, @RequestParam Long venueId);
}
