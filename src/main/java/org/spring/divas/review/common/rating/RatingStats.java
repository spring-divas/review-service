package org.spring.divas.review.common.rating;

public record RatingStats(
    Long targetId,
    Double averageRating,
    Long reviewCount
) {

}
