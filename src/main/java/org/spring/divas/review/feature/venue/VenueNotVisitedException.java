package org.spring.divas.review.feature.venue;

import org.spring.divas.review.common.exception.ResourceAccessDeniedException;

public class VenueNotVisitedException extends ResourceAccessDeniedException {

  public VenueNotVisitedException(String message) {
    super(message);
  }
}
