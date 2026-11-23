package org.spring.divas.review.feature.dish;

import java.util.List;

public interface DishReviewService {

  DishReviewResponseDto create(DishReviewRequestDto dto, Long userId);

  DishReviewResponseDto getById(Long id);

  List<DishReviewResponseDto> getAll();

  DishReviewResponseDto update(Long id, DishReviewRequestDto dto, Long userId);

  void delete(Long id, Long userId);
}
