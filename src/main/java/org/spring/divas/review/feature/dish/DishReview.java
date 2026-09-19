package org.spring.divas.review.feature.dish;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.spring.divas.review.common.entity.BaseEntity;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(
    name = "dish_review",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "dish_id"})
)
public class DishReview extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @Column(name = "rating", nullable = false)
  private Short rating;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDate createdAt;

  @Column(name = "dish_id", nullable = false)
  private Long dishId;
}
