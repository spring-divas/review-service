package org.spring.divas.review.feature.venue.dto;

public record VenueReviewUpdateParams(
    Long reviewId,
    String content,
    Short rating,
    Long userId
) {

  public static VenueReviewUpdateParams of(VenueReviewUpdateRequest request, Long reviewId,
      Long userId) {
    return new VenueReviewUpdateParams(reviewId, request.content(), request.rating(), userId);
  }
}
