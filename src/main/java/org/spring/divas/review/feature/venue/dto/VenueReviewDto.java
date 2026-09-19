package org.spring.divas.review.feature.venue.dto;

import java.time.LocalDate;
import org.spring.divas.review.feature.user.UserDto;

public record VenueReviewDto(
    Long id,
    UserDto user,
    String content,
    Short rating,
    Long venueId,
    LocalDate createdAt
) {

}
