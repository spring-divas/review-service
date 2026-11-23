package org.spring.divas.review.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.spring.divas.review.feature.dish.DishReview;
import org.spring.divas.review.feature.dish.DishReviewMapperImpl;
import org.spring.divas.review.feature.dish.DishReviewRequestDto;
import org.spring.divas.review.feature.dish.DishReviewResponseDto;
import org.spring.divas.review.feature.user.User;
import org.spring.divas.review.feature.user.UserDto;
import org.spring.divas.review.feature.user.UserMapperImpl;
import org.spring.divas.review.feature.venue.VenueReview;
import org.spring.divas.review.feature.venue.VenueReviewMapperImpl;
import org.spring.divas.review.feature.venue.VenueReviewRequestDto;
import org.spring.divas.review.feature.venue.VenueReviewResponseDto;

class MapperTest {

  private static final LocalDate CREATED_AT = LocalDate.of(2026, 1, 2);

  private static final UserDto USER = new UserDto(1L, "John Doe");

  @Test
  void mapsDishReviewWithUser() {
    DishReview review =
        DishReview.builder()
            .id(100L)
            .userId(1L)
            .dishId(10L)
            .content("Tasty")
            .rating((short) 5)
            .createdAt(CREATED_AT)
            .build();

    DishReviewResponseDto dto = new DishReviewMapperImpl().toDto(review, USER);

    assertThat(dto)
        .isEqualTo(new DishReviewResponseDto(100L, USER, "Tasty", (short) 5, 10L, CREATED_AT));
  }

  @Test
  void mapsDishReviewWithoutUser() {
    DishReview review = DishReview.builder().id(100L).dishId(10L).rating((short) 5).build();

    DishReviewResponseDto dto = new DishReviewMapperImpl().toDto(review, null);

    assertThat(dto.id()).isEqualTo(100L);
    assertThat(dto.user()).isNull();
  }

  @Test
  void mapsMissingDishReviewAndUserToNull() {
    assertThat(new DishReviewMapperImpl().toDto(null, null)).isNull();
  }

  @Test
  void mapsVenueReviewWithUser() {
    VenueReview review =
        VenueReview.builder()
            .id(100L)
            .userId(1L)
            .venueId(20L)
            .content("Cozy")
            .rating((short) 4)
            .createdAt(CREATED_AT)
            .build();

    VenueReviewResponseDto dto = new VenueReviewMapperImpl().toDto(review, USER);

    assertThat(dto)
        .isEqualTo(new VenueReviewResponseDto(100L, USER, "Cozy", (short) 4, 20L, CREATED_AT));
  }

  @Test
  void mapsUserWithoutVenueReview() {
    VenueReviewResponseDto dto = new VenueReviewMapperImpl().toDto(null, USER);

    assertThat(dto.user()).isEqualTo(USER);
    assertThat(dto.id()).isNull();
  }

  @Test
  void mapsMissingVenueReviewAndUserToNull() {
    assertThat(new VenueReviewMapperImpl().toDto(null, null)).isNull();
  }

  @Test
  void joinsFirstAndLastNameIntoFullName() {
    User user = User.builder().id(1L).firstName("John").lastName("Doe").build();

    assertThat(new UserMapperImpl().toDto(user)).isEqualTo(USER);
  }

  @Test
  void mapsMissingUserToNull() {
    assertThat(new UserMapperImpl().toDto(null)).isNull();
  }

  @Test
  void mapsDishReviewRequestToEntity() {
    DishReview review =
        new DishReviewMapperImpl()
            .toEntity(new DishReviewRequestDto("Tasty", (short) 5, 10L), 1L);

    assertThat(review.getId()).isNull();
    assertThat(review.getContent()).isEqualTo("Tasty");
    assertThat(review.getRating()).isEqualTo((short) 5);
    assertThat(review.getDishId()).isEqualTo(10L);
    assertThat(review.getUserId()).isEqualTo(1L);
  }

  @Test
  void mapsVenueReviewRequestToEntity() {
    VenueReview review =
        new VenueReviewMapperImpl()
            .toEntity(new VenueReviewRequestDto("Cozy", (short) 4, 20L), 1L);

    assertThat(review.getId()).isNull();
    assertThat(review.getContent()).isEqualTo("Cozy");
    assertThat(review.getRating()).isEqualTo((short) 4);
    assertThat(review.getVenueId()).isEqualTo(20L);
    assertThat(review.getUserId()).isEqualTo(1L);
  }
}
