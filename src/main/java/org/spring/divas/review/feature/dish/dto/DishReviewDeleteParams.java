package org.spring.divas.review.feature.dish.dto;

public record DishReviewDeleteParams(
    Long userId,
    Long reviewId
) {

}
