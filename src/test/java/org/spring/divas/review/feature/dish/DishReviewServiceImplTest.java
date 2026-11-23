package org.spring.divas.review.feature.dish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.review.common.exception.ResourceAccessDeniedException;
import org.spring.divas.review.common.exception.ResourceAlreadyExistsException;
import org.spring.divas.review.common.exception.ResourceNotFoundException;
import org.spring.divas.review.common.rating.RatingChangedEvent;
import org.spring.divas.review.feature.order.ResilientOrderClient;
import org.spring.divas.review.feature.user.UserDto;
import org.spring.divas.review.feature.user.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class DishReviewServiceImplTest {

  @Mock private DishReviewRepository dishReviewRepository;

  @Mock private DishReviewMapper dishReviewMapper;

  @Mock private UserService userService;

  @Mock private ResilientOrderClient orderClient;

  @Mock private TransactionTemplate transactionTemplate;

  @Mock private ApplicationEventPublisher eventPublisher;

  private DishReviewServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new DishReviewServiceImpl(
            dishReviewRepository,
            dishReviewMapper,
            userService,
            orderClient,
            transactionTemplate,
            eventPublisher);
  }

  @Test
  void savesReviewAndPublishesRatingChange() {
    DishReviewRequestDto request = new DishReviewRequestDto("Amazing pasta", (short) 5, 10L);
    DishReview entity =
        DishReview.builder()
            .id(100L)
            .userId(1L)
            .dishId(10L)
            .rating((short) 5)
            .content("Amazing pasta")
            .build();
    UserDto userDto = new UserDto(1L, "John Doe");
    final DishReviewResponseDto responseDto =
        new DishReviewResponseDto(100L, userDto, "Amazing pasta", (short) 5, 10L, LocalDate.now());

    when(dishReviewRepository.existsByUserIdAndDishId(1L, 10L)).thenReturn(false);
    when(orderClient.hasUserOrderedDish(1L, 10L)).thenReturn(true);
    when(dishReviewMapper.toEntity(request, 1L)).thenReturn(entity);
    runTransactionCallback();
    when(dishReviewRepository.saveAndFlush(entity)).thenReturn(entity);
    when(userService.findById(1L)).thenReturn(Optional.of(userDto));
    when(dishReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

    DishReviewResponseDto result = service.create(request, 1L);

    assertThat(result).isEqualTo(responseDto);
    verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
  }

  @Test
  void rejectsSecondReviewBySameUser() {
    DishReviewRequestDto request = new DishReviewRequestDto("Duplicate", (short) 5, 10L);
    when(dishReviewRepository.existsByUserIdAndDishId(1L, 10L)).thenReturn(true);

    assertThatThrownBy(() -> service.create(request, 1L))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("Review by the user for this dish already exists");
  }

  @Test
  void rejectsConcurrentDuplicateReview() {
    final DishReviewRequestDto request =
        new DishReviewRequestDto("Concurrent duplicate", (short) 5, 10L);

    when(dishReviewRepository.existsByUserIdAndDishId(1L, 10L)).thenReturn(false);
    when(orderClient.hasUserOrderedDish(1L, 10L)).thenReturn(true);
    runTransactionCallback();
    when(dishReviewMapper.toEntity(request, 1L)).thenReturn(new DishReview());
    when(dishReviewRepository.saveAndFlush(any(DishReview.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> service.create(request, 1L))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("Review by the user for this dish already exists");
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void rejectsReviewOfDishNotOrdered() {
    DishReviewRequestDto request = new DishReviewRequestDto("Not ordered", (short) 5, 10L);
    when(dishReviewRepository.existsByUserIdAndDishId(1L, 10L)).thenReturn(false);
    when(orderClient.hasUserOrderedDish(1L, 10L)).thenReturn(false);

    assertThatThrownBy(() -> service.create(request, 1L))
        .isInstanceOf(DishNotOrderedException.class)
        .hasMessage("User cannot rate the dish they have not ordered");
  }

  @Test
  void returnsReviewById() {
    DishReview entity =
        DishReview.builder()
            .id(100L)
            .userId(1L)
            .dishId(10L)
            .rating((short) 5)
            .content("Good")
            .build();
    UserDto userDto = new UserDto(1L, "John Doe");
    DishReviewResponseDto responseDto =
        new DishReviewResponseDto(100L, userDto, "Good", (short) 5, 10L, LocalDate.now());

    when(dishReviewRepository.findById(100L)).thenReturn(Optional.of(entity));
    when(userService.findById(1L)).thenReturn(Optional.of(userDto));
    when(dishReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

    DishReviewResponseDto result = service.getById(100L);

    assertThat(result).isEqualTo(responseDto);
  }

  @Test
  void throwsWhenReviewIsMissing() {
    when(dishReviewRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Dish review is not found");
  }

  @Test
  void returnsAllReviewsWithAuthors() {
    DishReview entity =
        DishReview.builder()
            .id(100L)
            .userId(1L)
            .dishId(10L)
            .rating((short) 5)
            .content("Good")
            .build();
    UserDto userDto = new UserDto(1L, "John Doe");
    DishReviewResponseDto responseDto =
        new DishReviewResponseDto(100L, userDto, "Good", (short) 5, 10L, LocalDate.now());

    when(dishReviewRepository.findAll()).thenReturn(List.of(entity));
    when(userService.getByIds(List.of(1L))).thenReturn(Map.of(1L, userDto));
    when(dishReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

    List<DishReviewResponseDto> result = service.getAll();

    assertThat(result).containsExactly(responseDto);
  }

  @Test
  void updatesOwnReviewAndPublishesRatingChange() {
    DishReview entity =
        DishReview.builder()
            .id(100L)
            .userId(1L)
            .dishId(10L)
            .rating((short) 4)
            .content("Old")
            .build();
    DishReviewRequestDto request =
        new DishReviewRequestDto("Updated content", (short) 5, 10L);
    UserDto userDto = new UserDto(1L, "John Doe");
    DishReviewResponseDto responseDto =
        new DishReviewResponseDto(
            100L, userDto, "Updated content", (short) 5, 10L, LocalDate.now());

    when(dishReviewRepository.findById(100L)).thenReturn(Optional.of(entity));
    when(userService.findById(1L)).thenReturn(Optional.of(userDto));
    when(dishReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

    DishReviewResponseDto result = service.update(100L, request, 1L);

    assertThat(result).isEqualTo(responseDto);
    assertThat(entity.getRating()).isEqualTo((short) 5);
    assertThat(entity.getContent()).isEqualTo("Updated content");
    verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
  }

  @Test
  void rejectsUpdateByNonOwner() {
    DishReview entity =
        DishReview.builder()
            .id(100L)
            .userId(2L)
            .dishId(10L)
            .rating((short) 4)
            .content("Old")
            .build();
    DishReviewRequestDto request = new DishReviewRequestDto("Updated", (short) 5, 10L);

    when(dishReviewRepository.findById(100L)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.update(100L, request, 1L))
        .isInstanceOf(ResourceAccessDeniedException.class)
        .hasMessage("Review does not belong to the user");
  }

  @Test
  void deletesOwnReviewAndPublishesRatingChange() {
    DishReview entity =
        DishReview.builder().id(100L).userId(1L).dishId(10L).rating((short) 5).build();

    when(dishReviewRepository.findById(100L)).thenReturn(Optional.of(entity));

    service.delete(100L, 1L);

    verify(dishReviewRepository).deleteById(100L);
    verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
  }

  @Test
  void rejectsDeletionByNonOwner() {
    DishReview entity =
        DishReview.builder().id(100L).userId(2L).dishId(10L).rating((short) 5).build();

    when(dishReviewRepository.findById(100L)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.delete(100L, 1L))
        .isInstanceOf(ResourceAccessDeniedException.class)
        .hasMessage("Review does not belong to the user");
    verify(dishReviewRepository, never()).deleteById(any());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void rejectsDeletionOfMissingReview() {

    when(dishReviewRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(99L, 1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Dish review is not found");
    verify(dishReviewRepository, never()).deleteById(any());
    verifyNoInteractions(eventPublisher);
  }

  private void runTransactionCallback() {
    when(transactionTemplate.execute(any()))
        .thenAnswer(
            invocation -> {
              TransactionCallback<?> callback = invocation.getArgument(0);
              return callback.doInTransaction(mock(TransactionStatus.class));
            });
  }
}
