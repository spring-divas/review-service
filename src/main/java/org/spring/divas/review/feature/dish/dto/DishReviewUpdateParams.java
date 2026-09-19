package org.spring.divas.review.feature.dish.dto;

public record DishReviewUpdateParams(
    Long reviewId,
    String content,
    Short rating,
    Long userId
) {

  public static DishReviewUpdateParams of(DishReviewUpdateRequest request, Long reviewId,
      Long userId) {
    return new DishReviewUpdateParams(reviewId, request.content(), request.rating(), userId);
  }
}
