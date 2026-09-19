package org.spring.divas.review.feature.user;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  private final UserMapper userMapper;

  @Override
  public Optional<UserDto> findById(Long id) {
    return userRepository.findById(id).map(userMapper::toDto);
  }

  @Override
  public Map<Long, UserDto> getByIds(Collection<Long> ids) {
    return userRepository.findAllById(ids).stream()
        .map(userMapper::toDto)
        .collect(Collectors.toMap(UserDto::id, Function.identity()));
  }
}
