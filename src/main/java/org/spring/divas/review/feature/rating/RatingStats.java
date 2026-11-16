package org.spring.divas.review.feature.rating;

public record RatingStats(
    Long targetId,
    Double averageRating,
    Long reviewCount
) {

}
