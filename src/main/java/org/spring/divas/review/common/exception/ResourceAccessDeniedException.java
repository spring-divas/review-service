package org.spring.divas.review.common.exception;

public class ResourceAccessDeniedException extends RuntimeException {

  public ResourceAccessDeniedException(String message) {
    super(message);
  }
}
