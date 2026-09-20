package org.spring.divas.review.feature.user;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface UserService {

  Optional<UserDto> findById(Long id);

  Map<Long, UserDto> getByIds(Collection<Long> ids);
}
