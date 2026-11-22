package org.spring.divas.review.feature.venue;

import java.time.LocalDate;
import org.spring.divas.review.feature.user.UserDto;

public record VenueReviewResponseDto(
    Long id,
    UserDto user,
    String content,
    Short rating,
    Long venueId,
    LocalDate createdAt
) {

}
