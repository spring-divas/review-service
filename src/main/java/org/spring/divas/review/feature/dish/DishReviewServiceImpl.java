package org.spring.divas.review.feature.dish;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class DishReviewServiceImpl implements DishReviewService {

  private final DishReviewRepository dishReviewRepository;

  private final DishReviewMapper dishReviewMapper;

  private final UserService userService;
  private final ResilientOrderClient orderClient;

  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public DishReviewResponseDto create(DishReviewRequestDto dto, Long userId) { // TODO: check-then-act
    if (dishReviewRepository.existsByUserIdAndDishId(userId, dto.dishId())) {
      throw new ResourceAlreadyExistsException("Review by the user for this dish already exists");
    }
    if (!orderClient.hasUserOrderedDish(userId,
        dto.dishId())) { // TODO: TransactionTemplate
      throw new DishNotOrderedException("User cannot rate the dish they have not ordered");
    }
    DishReview entityToSave = dishReviewMapper.toEntity(dto, userId);
    DishReview savedEntity = dishReviewRepository.save(entityToSave);
    eventPublisher.publishEvent(ratingChanged(savedEntity));
    return toDto(savedEntity);
  }

  @Override
  public DishReviewResponseDto getById(Long id) {
    return toDto(findReview(id));
  }

  @Override
  public List<DishReviewResponseDto> getAll() {
    List<DishReview> reviews = dishReviewRepository.findAll();
    Map<Long, UserDto> users = userService.getByIds(
        reviews.stream().map(DishReview::getUserId).distinct().toList()
    );
    return reviews.stream()
        .map(review -> dishReviewMapper.toDto(review, users.get(review.getUserId())))
        .toList();
  }

  @Override
  @Transactional
  public DishReviewResponseDto update(Long id, DishReviewRequestDto dto, Long userId) {
    DishReview review = findReview(id);

    if (!ownsReview(userId, review)) {
      throw new ResourceAccessDeniedException("Review does not belong to the user");
    }

    review.setContent(dto.content());
    review.setRating(dto.rating());
    eventPublisher.publishEvent(ratingChanged(review));

    return toDto(review);
  }

  private boolean ownsReview(Long userId, DishReview review) {
    return review.getUserId().equals(userId);
  }

  @Override
  @Transactional
  public void delete(Long id, Long userId) {
    DishReview review = findReview(id);

    if (!ownsReview(userId, review)) {
      throw new ResourceAccessDeniedException("Review does not belong to the user");
    }
    dishReviewRepository.deleteById(id);
    eventPublisher.publishEvent(ratingChanged(review));
  }

  private RatingChangedEvent ratingChanged(DishReview review) {
    return new RatingChangedEvent(ReviewTarget.DISH, review.getDishId());
  }

  private DishReview findReview(Long id) {
    return dishReviewRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Dish review is not found"));
  }

  private DishReviewResponseDto toDto(DishReview review) {
    return dishReviewMapper.toDto(review, userService.findById(review.getUserId()).orElse(null));
  }
}
