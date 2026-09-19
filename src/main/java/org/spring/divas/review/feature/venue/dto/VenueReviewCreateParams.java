package org.spring.divas.review.feature.venue.dto;

import org.spring.divas.review.feature.venue.VenueReview;

public record VenueReviewCreateParams(
    Long venueId,
    Long userId,
    String content,
    Short rating
) {

  public static VenueReviewCreateParams of(VenueReviewCreateRequest request, Long userId) {
    return new VenueReviewCreateParams(request.venueId(), userId, request.content(),
        request.rating());
  }

  public static VenueReview toEntity(VenueReviewCreateParams params) {
    return VenueReview.builder()
        .venueId(params.venueId())
        .content(params.content())
        .rating(params.rating())
        .userId(params.userId())
        .build();
  }
}
