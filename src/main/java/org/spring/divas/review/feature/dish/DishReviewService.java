package org.spring.divas.review.feature.dish;

import java.util.List;
import org.spring.divas.review.feature.dish.dto.DishReviewCreateParams;
import org.spring.divas.review.feature.dish.dto.DishReviewDeleteParams;
import org.spring.divas.review.feature.dish.dto.DishReviewDto;
import org.spring.divas.review.feature.dish.dto.DishReviewUpdateParams;

public interface DishReviewService {

  DishReviewDto create(DishReviewCreateParams params);

  DishReviewDto getById(Long id);

  List<DishReviewDto> getAll();

  DishReviewDto update(DishReviewUpdateParams params);

  void delete(DishReviewDeleteParams params);
}
