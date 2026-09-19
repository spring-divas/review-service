package org.spring.divas.review.feature.dish.dto;

import org.spring.divas.review.feature.dish.DishReview;

public record DishReviewCreateParams(
    Long dishId,
    Long userId,
    String content,
    Short rating
) {

  public static DishReviewCreateParams of(DishReviewCreateRequest request, Long userId) {
    return new DishReviewCreateParams(request.dishId(), userId, request.content(),
        request.rating());
  }

  public static DishReview toEntity(DishReviewCreateParams params) {
    return DishReview.builder()
        .dishId(params.dishId())
        .content(params.content())
        .rating(params.rating())
        .userId(params.userId())
        .build();
  }
}
