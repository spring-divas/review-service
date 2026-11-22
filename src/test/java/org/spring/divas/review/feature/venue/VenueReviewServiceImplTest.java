package org.spring.divas.review.feature.venue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.divas.review.common.exception.ResourceAccessDeniedException;
import org.spring.divas.review.common.exception.ResourceAlreadyExistsException;
import org.spring.divas.review.common.exception.ResourceNotFoundException;
import org.spring.divas.review.feature.order.ResilientOrderClient;
import org.spring.divas.review.feature.rating.RatingChangedEvent;
import org.spring.divas.review.feature.user.UserDto;
import org.spring.divas.review.feature.user.UserService;
import org.spring.divas.review.feature.venue.dto.VenueReviewCreateParams;
import org.spring.divas.review.feature.venue.dto.VenueReviewDeleteParams;
import org.spring.divas.review.feature.venue.dto.VenueReviewDto;
import org.spring.divas.review.feature.venue.dto.VenueReviewUpdateParams;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueReviewServiceImplTest {

    @Mock
    private VenueReviewRepository venueReviewRepository;

    @Mock
    private VenueReviewMapper venueReviewMapper;

    @Mock
    private UserService userService;

    @Mock
    private ResilientOrderClient orderClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private VenueReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VenueReviewServiceImpl(
                venueReviewRepository,
                venueReviewMapper,
                userService,
                orderClient,
                eventPublisher
        );
    }

    @Test
    void create_shouldSaveReviewAndPublishEventWhenVisited() {
        VenueReviewCreateParams params = new VenueReviewCreateParams(20L, 1L, "Great atmosphere", (short) 5);
        VenueReview entity = VenueReview.builder().id(100L).userId(1L).venueId(20L).rating((short) 5).content("Great atmosphere").build();
        UserDto userDto = new UserDto(1L, "Alice Smith");
        VenueReviewDto responseDto = new VenueReviewDto(100L, userDto, "Great atmosphere", (short) 5, 20L, LocalDate.now());

        when(venueReviewRepository.existsByUserIdAndVenueId(1L, 20L)).thenReturn(false);
        when(orderClient.hasUserBeenToVenue(1L, 20L)).thenReturn(true);
        when(venueReviewRepository.save(any(VenueReview.class))).thenReturn(entity);
        when(userService.findById(1L)).thenReturn(Optional.of(userDto));
        when(venueReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

        VenueReviewDto result = service.create(params);

        assertThat(result).isEqualTo(responseDto);
        verify(venueReviewRepository).save(any(VenueReview.class));
        verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
    }

    @Test
    void create_shouldThrowWhenReviewAlreadyExists() {
        VenueReviewCreateParams params = new VenueReviewCreateParams(20L, 1L, "Duplicate", (short) 5);
        when(venueReviewRepository.existsByUserIdAndVenueId(1L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(params))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("Review by the user for this venue already exists");
    }

    @Test
    void create_shouldThrowWhenUserHasNotVisitedVenue() {
        VenueReviewCreateParams params = new VenueReviewCreateParams(20L, 1L, "Not visited", (short) 5);
        when(venueReviewRepository.existsByUserIdAndVenueId(1L, 20L)).thenReturn(false);
        when(orderClient.hasUserBeenToVenue(1L, 20L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(params))
                .isInstanceOf(VenueNotVisitedException.class)
                .hasMessage("User cannot review the venue they have not visited");
    }

    @Test
    void getById_shouldReturnDtoWhenFound() {
        VenueReview entity = VenueReview.builder().id(100L).userId(1L).venueId(20L).rating((short) 5).content("Great").build();
        UserDto userDto = new UserDto(1L, "Alice Smith");
        VenueReviewDto responseDto = new VenueReviewDto(100L, userDto, "Great", (short) 5, 20L, LocalDate.now());

        when(venueReviewRepository.findById(100L)).thenReturn(Optional.of(entity));
        when(userService.findById(1L)).thenReturn(Optional.of(userDto));
        when(venueReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

        VenueReviewDto result = service.getById(100L);

        assertThat(result).isEqualTo(responseDto);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(venueReviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue review is not found");
    }

    @Test
    void getAll_shouldReturnAllReviews() {
        VenueReview entity = VenueReview.builder().id(100L).userId(1L).venueId(20L).rating((short) 5).content("Great").build();
        UserDto userDto = new UserDto(1L, "Alice Smith");
        VenueReviewDto responseDto = new VenueReviewDto(100L, userDto, "Great", (short) 5, 20L, LocalDate.now());

        when(venueReviewRepository.findAll()).thenReturn(List.of(entity));
        when(userService.getByIds(List.of(1L))).thenReturn(Map.of(1L, userDto));
        when(venueReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

        List<VenueReviewDto> result = service.getAll();

        assertThat(result).containsExactly(responseDto);
    }

    @Test
    void update_shouldUpdateWhenUserOwnsReview() {
        VenueReview entity = VenueReview.builder().id(100L).userId(1L).venueId(20L).rating((short) 3).content("Ok").build();
        VenueReviewUpdateParams params = new VenueReviewUpdateParams(100L, "Much better", (short) 4, 1L);
        UserDto userDto = new UserDto(1L, "Alice Smith");
        VenueReviewDto responseDto = new VenueReviewDto(100L, userDto, "Much better", (short) 4, 20L, LocalDate.now());

        when(venueReviewRepository.findById(100L)).thenReturn(Optional.of(entity));
        when(userService.findById(1L)).thenReturn(Optional.of(userDto));
        when(venueReviewMapper.toDto(entity, userDto)).thenReturn(responseDto);

        VenueReviewDto result = service.update(params);

        assertThat(result).isEqualTo(responseDto);
        assertThat(entity.getRating()).isEqualTo((short) 4);
        assertThat(entity.getContent()).isEqualTo("Much better");
        verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
    }

    @Test
    void update_shouldThrowWhenUserDoesNotOwnReview() {
        VenueReview entity = VenueReview.builder().id(100L).userId(2L).venueId(20L).rating((short) 3).content("Ok").build();
        VenueReviewUpdateParams params = new VenueReviewUpdateParams(100L, "Much better", (short) 4, 1L);

        when(venueReviewRepository.findById(100L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.update(params))
                .isInstanceOf(ResourceAccessDeniedException.class)
                .hasMessage("Review does not belong to the user");
    }

    @Test
    void delete_shouldDeleteWhenExists() {
        VenueReview entity = VenueReview.builder().id(100L).userId(1L).venueId(20L).rating((short) 5).build();
        VenueReviewDeleteParams params = new VenueReviewDeleteParams(1L, 100L);

        when(venueReviewRepository.findById(100L)).thenReturn(Optional.of(entity));

        service.delete(params);

        verify(venueReviewRepository).deleteById(100L);
        verify(eventPublisher).publishEvent(any(RatingChangedEvent.class));
    }
}
