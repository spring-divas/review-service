package org.spring.divas.review.feature.dish;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DishReviewRepository extends JpaRepository<DishReview, Long> {
  boolean existsByUserIdAndDishId(Long userId, Long dishId);
  @Query("""
        SELECT COALESCE(AVG(r.rating), 0.0)
        FROM DishReview r
        WHERE r.dishId = :dishId
        """)
  Double findAverageRatingByDishId(@Param("dishId") Long dishId);

  long countByDishId(Long dishId);
}
