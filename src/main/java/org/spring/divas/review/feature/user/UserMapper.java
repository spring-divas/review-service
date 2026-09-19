package org.spring.divas.review.feature.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
@FunctionalInterface
public interface UserMapper {

  @Mapping(
      target = "fullName",
      expression = "java(String.join(\" \", user.getFirstName(), user.getLastName()))"
  )
  UserDto toDto(User user);
}
