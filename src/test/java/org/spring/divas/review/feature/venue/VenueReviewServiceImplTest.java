package org.spring.divas.review.feature.venue;

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
class VenueReviewServiceImplTest {

  @Mock private VenueReviewRepository venueReviewRepository;

  @Mock private VenueReviewMapper venueReviewMapper;

  @Mock private UserService userService;

  @Mock private ResilientOrderClient orderClient;

  @Mock private TransactionTemplate transactionTemplate;

  @Mock private ApplicationEventPublisher eventPublisher;

  private VenueReviewServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new VenueReviewServiceImpl(
            venueReviewRepository,
            venueReviewMapper,
            userService,
            orderClient,
            transactionTemplate,
            eventPublisher);
  }

  @Test
  void savesReviewAndPublishesRatingChange() {
    final VenueReviewRequestDto request =
        new VenueReviewRequestDto("Great atmosphere", (short) 5, 20L, 1L);
    final VenueReview entity =
        VenueReview.builder()
            .id(100L)
            .userId(1L)
            .venueId(20L)
            .rating((short) 5)
            .content("Great atmosphere")
            .build();
    UserDto userDto = new UserDto(1L, "Alice Smith");
    final VenueReviewResponseDto responseDto =
        new VenueReviewResponseDto(
            100L, userDto, "Great atmosphere", (short) 5, 20L, LocalDate.now());

    when(venueReviewRepository.existsByUserIdAndVenueId(1L, 20L)).thenReturn(false);
    when(orderClient.hasUserBeenToVenue(1L, 20L)).thenReturn(true);
    runTransactionCallback();
    when(venueReviewMapper.toEntity(request)).thenReturn(entity);
    when(venueReviewRepository.saveAndFlush(entity)).thenReturn(entity);
    when(userService.findById(1L)).thenReturn(Optional.of(userDto));
    when(venueReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

    VenueReviewResponseDto result = service.create(request);

    assertThat(result).isEqualTo(responseDto);
    verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
  }

  @Test
  void rejectsSecondReviewBySameUser() {
    VenueReviewRequestDto request = new VenueReviewRequestDto("Duplicate", (short) 5, 20L, 1L);
    when(venueReviewRepository.existsByUserIdAndVenueId(1L, 20L)).thenReturn(true);

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("Review by the user for this venue already exists");
  }

  @Test
  void rejectsConcurrentDuplicateReview() {
    final VenueReviewRequestDto request =
        new VenueReviewRequestDto("Concurrent duplicate", (short) 5, 20L, 1L);

    when(venueReviewRepository.existsByUserIdAndVenueId(1L, 20L)).thenReturn(false);
    when(orderClient.hasUserBeenToVenue(1L, 20L)).thenReturn(true);
    runTransactionCallback();
    when(venueReviewMapper.toEntity(request)).thenReturn(new VenueReview());
    when(venueReviewRepository.saveAndFlush(any(VenueReview.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessage("Review by the user for this venue already exists");
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void rejectsReviewOfVenueNotVisited() {
    VenueReviewRequestDto request = new VenueReviewRequestDto("Not visited", (short) 5, 20L, 1L);
    when(venueReviewRepository.existsByUserIdAndVenueId(1L, 20L)).thenReturn(false);
    when(orderClient.hasUserBeenToVenue(1L, 20L)).thenReturn(false);

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(VenueNotVisitedException.class)
        .hasMessage("User cannot review the venue they have not visited");
  }

  @Test
  void returnsReviewById() {
    VenueReview entity =
        VenueReview.builder()
            .id(100L)
            .userId(1L)
            .venueId(20L)
            .rating((short) 5)
            .content("Great")
            .build();
    UserDto userDto = new UserDto(1L, "Alice Smith");
    VenueReviewResponseDto responseDto =
        new VenueReviewResponseDto(100L, userDto, "Great", (short) 5, 20L, LocalDate.now());

    when(venueReviewRepository.findById(100L)).thenReturn(Optional.of(entity));
    when(userService.findById(1L)).thenReturn(Optional.of(userDto));
    when(venueReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

    VenueReviewResponseDto result = service.getById(100L);

    assertThat(result).isEqualTo(responseDto);
  }

  @Test
  void throwsWhenReviewIsMissing() {
    when(venueReviewRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(99L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Venue review is not found");
  }

  @Test
  void returnsAllReviewsWithAuthors() {
    VenueReview entity =
        VenueReview.builder()
            .id(100L)
            .userId(1L)
            .venueId(20L)
            .rating((short) 5)
            .content("Great")
            .build();
    UserDto userDto = new UserDto(1L, "Alice Smith");
    VenueReviewResponseDto responseDto =
        new VenueReviewResponseDto(100L, userDto, "Great", (short) 5, 20L, LocalDate.now());

    when(venueReviewRepository.findAll()).thenReturn(List.of(entity));
    when(userService.getByIds(List.of(1L))).thenReturn(Map.of(1L, userDto));
    when(venueReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

    List<VenueReviewResponseDto> result = service.getAll();

    assertThat(result).containsExactly(responseDto);
  }

  @Test
  void updatesOwnReviewAndPublishesRatingChange() {
    VenueReview entity =
        VenueReview.builder()
            .id(100L)
            .userId(1L)
            .venueId(20L)
            .rating((short) 3)
            .content("Ok")
            .build();
    VenueReviewRequestDto request =
        new VenueReviewRequestDto("Much better", (short) 4, 20L, 1L);
    UserDto userDto = new UserDto(1L, "Alice Smith");
    VenueReviewResponseDto responseDto =
        new VenueReviewResponseDto(100L, userDto, "Much better", (short) 4, 20L, LocalDate.now());

    when(venueReviewRepository.findById(100L)).thenReturn(Optional.of(entity));
    when(userService.findById(1L)).thenReturn(Optional.of(userDto));
    when(venueReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

    VenueReviewResponseDto result = service.update(100L, request);

    assertThat(result).isEqualTo(responseDto);
    assertThat(entity.getRating()).isEqualTo((short) 4);
    assertThat(entity.getContent()).isEqualTo("Much better");
    verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
  }

  @Test
  void rejectsUpdateByNonOwner() {
    VenueReview entity =
        VenueReview.builder()
            .id(100L)
            .userId(2L)
            .venueId(20L)
            .rating((short) 3)
            .content("Ok")
            .build();
    VenueReviewRequestDto request =
        new VenueReviewRequestDto("Much better", (short) 4, 20L, 1L);

    when(venueReviewRepository.findById(100L)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.update(100L, request))
        .isInstanceOf(ResourceAccessDeniedException.class)
        .hasMessage("Review does not belong to the user");
  }

  @Test
  void deletesOwnReviewAndPublishesRatingChange() {
    VenueReview entity =
        VenueReview.builder().id(100L).userId(1L).venueId(20L).rating((short) 5).build();

    when(venueReviewRepository.findById(100L)).thenReturn(Optional.of(entity));

    service.delete(100L, 1L);

    verify(venueReviewRepository).deleteById(100L);
    verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
  }

  @Test
  void rejectsDeletionByNonOwner() {
    VenueReview entity =
        VenueReview.builder().id(100L).userId(2L).venueId(10L).rating((short) 5).build();

    when(venueReviewRepository.findById(100L)).thenReturn(Optional.of(entity));

    assertThatThrownBy(() -> service.delete(100L, 1L))
        .isInstanceOf(ResourceAccessDeniedException.class)
        .hasMessage("Review does not belong to the user");
    verify(venueReviewRepository, never()).deleteById(any());
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void rejectsDeletionOfMissingReview() {

    when(venueReviewRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(99L, 1L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Venue review is not found");
    verify(venueReviewRepository, never()).deleteById(any());
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
