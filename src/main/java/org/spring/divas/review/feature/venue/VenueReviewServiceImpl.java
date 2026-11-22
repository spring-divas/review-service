package org.spring.divas.review.feature.venue;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.spring.divas.review.common.exception.ResourceAccessDeniedException;
import org.spring.divas.review.common.exception.ResourceAlreadyExistsException;
import org.spring.divas.review.common.exception.ResourceNotFoundException;
import org.spring.divas.review.common.rating.RatingChangedEvent;
import org.spring.divas.review.common.rating.ReviewTarget;
import org.spring.divas.review.feature.order.ResilientOrderClient;
import org.spring.divas.review.feature.user.UserDto;
import org.spring.divas.review.feature.user.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@AllArgsConstructor
@Service
public class VenueReviewServiceImpl implements VenueReviewService {

  private final VenueReviewRepository venueReviewRepository;

  private final VenueReviewMapper venueReviewMapper;

  private final UserService userService;
  private final ResilientOrderClient orderClient;
  private final TransactionTemplate transactionTemplate;

  private final ApplicationEventPublisher eventPublisher;

  @Override
  public VenueReviewResponseDto create(VenueReviewRequestDto dto) {
    if (venueReviewRepository.existsByUserIdAndVenueId(dto.userId(), dto.venueId())) {
      throw new ResourceAlreadyExistsException("Review by the user for this venue already exists");
    }

    if (!orderClient.hasUserBeenToVenue(dto.userId(), dto.venueId())) {
      throw new VenueNotVisitedException("User cannot review the venue they have not visited");
    }

    try {
      return transactionTemplate.execute(status -> {
        VenueReview saved = venueReviewRepository.saveAndFlush(venueReviewMapper.toEntity(dto));
        eventPublisher.publishEvent(ratingChanged(saved));
        return toDto(saved);
      });
    } catch (DataIntegrityViolationException _) {
      throw new ResourceAlreadyExistsException("Review by the user for this venue already exists");
    }
  }

  @Override
  public VenueReviewResponseDto getById(Long id) {
    return toDto(findReview(id));
  }

  @Override
  public List<VenueReviewResponseDto> getAll() {
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
  public VenueReviewResponseDto update(Long id, VenueReviewRequestDto dto) {
    VenueReview review = findReview(id);

    if (!ownsReview(dto.userId(), review)) {
      throw new ResourceAccessDeniedException("Review does not belong to the user");
    }

    review.setContent(dto.content());
    review.setRating(dto.rating());
    eventPublisher.publishEvent(ratingChanged(review));

    return toDto(review);
  }

  private boolean ownsReview(Long userId, VenueReview review) {
    return review.getUserId().equals(userId);
  }

  @Override
  @Transactional
  public void delete(Long id, Long userId) {
    VenueReview review = findReview(id);

    if (!ownsReview(userId, review)) {
      throw new ResourceAccessDeniedException("Review does not belong to the user");
    }
    venueReviewRepository.deleteById(id);
    eventPublisher.publishEvent(ratingChanged(review));
  }

  private RatingChangedEvent ratingChanged(VenueReview review) {
    return new RatingChangedEvent(ReviewTarget.VENUE, review.getVenueId());
  }

  private VenueReview findReview(Long id) {
    return venueReviewRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Venue review is not found"));
  }

  private VenueReviewResponseDto toDto(VenueReview review) {
    return venueReviewMapper.toDto(review, userService.findById(review.getUserId()).orElse(null));
  }
}
