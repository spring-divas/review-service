package org.spring.divas.review.feature.venue;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.spring.divas.review.common.exception.ResourceAccessDeniedException;
import org.spring.divas.review.common.exception.ResourceAlreadyExistsException;
import org.spring.divas.review.common.exception.ResourceNotFoundException;
import org.spring.divas.review.feature.user.UserDto;
import org.spring.divas.review.feature.user.UserService;
import org.spring.divas.review.feature.venue.dto.VenueReviewCreateParams;
import org.spring.divas.review.feature.venue.dto.VenueReviewDeleteParams;
import org.spring.divas.review.feature.venue.dto.VenueReviewDto;
import org.spring.divas.review.feature.venue.dto.VenueReviewUpdateParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class VenueReviewServiceImpl implements VenueReviewService {

  private final VenueReviewRepository venueReviewRepository;

  private final VenueReviewMapper venueReviewMapper;

  private final UserService userService;

  @Override
  @Transactional
  public VenueReviewDto create(VenueReviewCreateParams params) { // TODO: check-then-act
    if (venueReviewRepository.existsByUserIdAndVenueId(params.userId(), params.venueId())) {
      throw new ResourceAlreadyExistsException("Review by the user for this venue already exists");
    }
    VenueReview entityToSave = VenueReviewCreateParams.toEntity(params);
    VenueReview savedEntity = venueReviewRepository.save(entityToSave);
    return toDto(savedEntity);
  }

  @Override
  public VenueReviewDto getById(Long id) {
    return venueReviewRepository
        .findById(id)
        .map(this::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("Venue review is not found"));
  }

  @Override
  public List<VenueReviewDto> getAll() {
    List<VenueReview> reviews = venueReviewRepository.findAll();
    Map<Long, UserDto> users = userService.getByIds(
        reviews.stream().map(VenueReview::getUserId).distinct().toList()
    );
    return reviews.stream()
        .map(review -> venueReviewMapper.toDto(review, users.get(review.getUserId())))
        .toList();
  }

  @Override
  @Transactional
  public VenueReviewDto update(VenueReviewUpdateParams params) {
    VenueReview review = venueReviewRepository
        .findById(params.reviewId())
        .orElseThrow(() -> new ResourceNotFoundException("Venue review is not found"));

    if (!ownsReview(params.userId(), review)) {
      throw new ResourceAccessDeniedException("Review does not belong to the user");
    }

    review.setContent(params.content());
    review.setRating(params.rating());

    return toDto(review);
  }

  private boolean ownsReview(Long userId, VenueReview review) {
    return review.getUserId().equals(userId);
  }

  @Override
  @Transactional
  public void delete(VenueReviewDeleteParams params) {
    VenueReview review = venueReviewRepository
        .findById(params.reviewId())
        .orElseThrow(() -> new ResourceNotFoundException("Venue review is not found"));

    if (!ownsReview(params.userId(), review)) {
      throw new ResourceAccessDeniedException("Review does not belong to the user");
    }
    venueReviewRepository.deleteById(params.reviewId());
  }

  private VenueReviewDto toDto(VenueReview review) {
    return venueReviewMapper.toDto(review, userService.findById(review.getUserId()).orElse(null));
  }
}
