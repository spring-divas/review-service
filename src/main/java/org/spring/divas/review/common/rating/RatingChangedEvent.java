package org.spring.divas.review.common.rating;

public record RatingChangedEvent(ReviewTarget target, Long targetId) {

}
