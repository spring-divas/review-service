package org.spring.divas.review.feature.dish;

import java.time.LocalDate;
import org.spring.divas.review.feature.user.UserDto;

public record DishReviewResponseDto(
    Long id,
    UserDto user,
    String content,
    Short rating,
    Long dishId,
    LocalDate createdAt
) {

}
