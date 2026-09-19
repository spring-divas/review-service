package org.spring.divas.review.feature.dish;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.spring.divas.review.common.exception.ResourceAccessDeniedException;
import org.spring.divas.review.common.exception.ResourceAlreadyExistsException;
import org.spring.divas.review.common.exception.ResourceNotFoundException;
import org.spring.divas.review.feature.dish.dto.DishReviewCreateParams;
import org.spring.divas.review.feature.dish.dto.DishReviewDeleteParams;
import org.spring.divas.review.feature.dish.dto.DishReviewDto;
import org.spring.divas.review.feature.dish.dto.DishReviewUpdateParams;
import org.spring.divas.review.feature.user.UserDto;
import org.spring.divas.review.feature.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class DishReviewServiceImpl implements DishReviewService {

  private final DishReviewRepository dishReviewRepository;

  private final DishReviewMapper dishReviewMapper;

  private final UserService userService;

  @Override
  @Transactional
  public DishReviewDto create(DishReviewCreateParams params) { // TODO: check-then-act
    if (dishReviewRepository.existsByUserIdAndDishId(params.userId(), params.dishId())) {
      throw new ResourceAlreadyExistsException("Review by the user for this dish already exists");
    }
    DishReview entityToSave = DishReviewCreateParams.toEntity(params);
    DishReview savedEntity = dishReviewRepository.save(entityToSave);
    return toDto(savedEntity);
  }

  @Override
  public DishReviewDto getById(Long id) {
    return dishReviewRepository
        .findById(id)
        .map(this::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("Dish review is not found"));
  }

  @Override
  public List<DishReviewDto> getAll() {
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
  public DishReviewDto update(DishReviewUpdateParams params) {
    DishReview review = dishReviewRepository
        .findById(params.reviewId())
        .orElseThrow(() -> new ResourceNotFoundException("Dish review is not found"));

    if (!ownsReview(params.userId(), review)) {
      throw new ResourceAccessDeniedException("Review does not belong to the user");
    }

    review.setContent(params.content());
    review.setRating(params.rating());

    return toDto(review);
  }

  private boolean ownsReview(Long userId, DishReview review) {
    return review.getUserId().equals(userId);
  }

  @Override
  @Transactional
  public void delete(DishReviewDeleteParams params) {
    DishReview review = dishReviewRepository
        .findById(params.reviewId())
        .orElseThrow(() -> new ResourceNotFoundException("Dish review is not found"));

    if (!ownsReview(params.userId(), review)) {
      throw new ResourceAccessDeniedException("Review does not belong to the user");
    }
    dishReviewRepository.deleteById(params.reviewId());
  }

  private DishReviewDto toDto(DishReview review) {
    return dishReviewMapper.toDto(review, userService.findById(review.getUserId()).orElse(null));
  }
}
