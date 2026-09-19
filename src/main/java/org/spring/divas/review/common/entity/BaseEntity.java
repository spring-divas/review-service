package org.spring.divas.review.common.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.MappedSuperclass;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.Id;

@Getter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (Hibernate.getClass(this) != Hibernate.getClass(o)) {
      return false;
    }
    BaseEntity that = (BaseEntity) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Hibernate.getClass(this).hashCode();
  }
}