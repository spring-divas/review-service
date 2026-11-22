package org.spring.divas.review.feature.venue;

import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.spring.divas.review.feature.user.UserDto;

@Mapper(componentModel = "spring")
public interface VenueReviewMapper {

  @Mapping(target = "id", source = "review.id")
  @Mapping(target = "user", source = "user")
  VenueReviewResponseDto toDto(VenueReview review, UserDto user);

  @BeanMapping(builder = @Builder(disableBuilder = true))
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  VenueReview toEntity(VenueReviewRequestDto dto);
}
