package org.spring.divas.review.feature.dish.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DishReviewUpdateRequest(
    @Nullable String content,
    @NotNull @Min(1) @Max(5) Short rating
) {

}
