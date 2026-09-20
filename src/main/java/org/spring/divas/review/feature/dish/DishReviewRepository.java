package org.spring.divas.review.feature.dish;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DishReviewRepository extends JpaRepository<DishReview, Long> {
  boolean existsByUserIdAndDishId(Long userId, Long dishId);
}
