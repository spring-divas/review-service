package org.spring.divas.review.feature.venue;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.spring.divas.review.feature.user.UserDto;
import org.spring.divas.review.feature.venue.dto.VenueReviewDto;

@Mapper(componentModel = "spring")
@FunctionalInterface
public interface VenueReviewMapper {

  @Mapping(target = "id", source = "review.id")
  @Mapping(target = "user", source = "user")
  VenueReviewDto toDto(VenueReview review, UserDto user);
}
