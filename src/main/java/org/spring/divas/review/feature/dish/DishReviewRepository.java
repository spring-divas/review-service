package org.spring.divas.review.feature.dish;

import org.spring.divas.review.feature.rating.RatingStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DishReviewRepository extends JpaRepository<DishReview, Long> {
  boolean existsByUserIdAndDishId(Long userId, Long dishId);

  @Query(
      """
      select :dishId, coalesce(avg(r.rating), 0.0), count(r)
      from DishReview r
      where r.dishId = :dishId
      """
  )
  RatingStats getRatingStats(@Param("dishId") Long dishId);
}
