package org.spring.divas.review.feature.dish;

import org.spring.divas.review.common.exception.ResourceAccessDeniedException;

public class DishNotOrderedException extends ResourceAccessDeniedException {

  public DishNotOrderedException(String message) {
    super(message);
  }
}
