package org.spring.divas.review.feature.dish;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.spring.divas.review.feature.dish.dto.DishReviewDto;
import org.spring.divas.review.feature.user.UserDto;

@Mapper(componentModel = "spring")
@FunctionalInterface
public interface DishReviewMapper {

  @Mapping(target = "id", source = "review.id")
  @Mapping(target = "user", source = "user")
  DishReviewDto toDto(DishReview review, UserDto user);
}
