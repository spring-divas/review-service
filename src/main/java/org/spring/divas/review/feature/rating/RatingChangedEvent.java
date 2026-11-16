package org.spring.divas.review.feature.rating;

public record RatingChangedEvent(ReviewTarget target, Long targetId) {

}
