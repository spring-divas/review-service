package org.spring.divas.review.feature.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper);
    }

    @Test
    void findById_shouldReturnUserWhenFound() {
        User user = User.builder().id(1L).firstName("John").lastName("Doe").build();
        UserDto userDto = new UserDto(1L, "John Doe");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        Optional<UserDto> result = userService.findById(1L);

        assertThat(result).contains(userDto);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void getByIds_shouldReturnMappedUsers() {
        User user1 = User.builder().id(1L).firstName("John").lastName("Doe").build();
        User user2 = User.builder().id(2L).firstName("Jane").lastName("Smith").build();
        UserDto dto1 = new UserDto(1L, "John Doe");
        UserDto dto2 = new UserDto(2L, "Jane Smith");

        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(user1, user2));
        when(userMapper.toDto(user1)).thenReturn(dto1);
        when(userMapper.toDto(user2)).thenReturn(dto2);

        Map<Long, UserDto> result = userService.getByIds(List.of(1L, 2L));

        assertThat(result).containsEntry(1L, dto1);
        assertThat(result).containsEntry(2L, dto2);
    }
}
