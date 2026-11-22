package org.spring.divas.review.feature.dish;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.spring.divas.review.common.exception.ResourceAccessDeniedException;
import org.spring.divas.review.common.exception.ResourceAlreadyExistsException;
import org.spring.divas.review.common.exception.ResourceNotFoundException;
import org.spring.divas.review.config.CorrelationIdContext;
import org.spring.divas.review.feature.dish.dto.DishReviewCreateParams;
import org.spring.divas.review.feature.dish.dto.DishReviewDeleteParams;
import org.spring.divas.review.feature.dish.dto.DishReviewDto;
import org.spring.divas.review.feature.user.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DishReviewController.class)
class DishReviewControllerTest {

  private static final String URL = "/review/dish";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DishReviewService dishReviewService;

  @Test
  void returnsAllReviews() throws Exception {
    DishReviewDto dto =
        new DishReviewDto(
            100L, new UserDto(1L, "John Doe"), "Tasty", (short) 5, 10L, LocalDate.of(2026, 1, 2));
    when(dishReviewService.getAll()).thenReturn(List.of(dto));

    mockMvc
        .perform(get(URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(100))
        .andExpect(jsonPath("$[0].user.fullName").value("John Doe"))
        .andExpect(jsonPath("$[0].dishId").value(10))
        .andExpect(jsonPath("$[0].createdAt").value("2026-01-02"));
  }

  @Test
  void returns201WithCreatedReview() throws Exception {
    DishReviewDto dto =
        new DishReviewDto(
            100L, new UserDto(1L, "John Doe"), "Tasty", (short) 5, 10L, LocalDate.of(2026, 1, 2));
    when(dishReviewService.create(new DishReviewCreateParams(10L, 1L, "Tasty", (short) 5)))
        .thenReturn(dto);

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content": "Tasty", "rating": 5, "dishId": 10, "userId": 1}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(100))
        .andExpect(jsonPath("$.rating").value(5));
  }

  @Test
  void returns400WithFieldErrorsForInvalidReview() throws Exception {
    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"rating": 7, "userId": 1}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(jsonPath("$.errors.rating").exists())
        .andExpect(jsonPath("$.errors.dishId").exists())
        .andExpect(jsonPath("$.timestamp").value(notNullValue()));

    verify(dishReviewService, never()).create(any());
  }

  @Test
  void returns409WhenReviewAlreadyExists() throws Exception {
    when(dishReviewService.create(any()))
        .thenThrow(new ResourceAlreadyExistsException("Review already exists"));

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"content": "Again", "rating": 4, "dishId": 10, "userId": 1}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value("Review already exists"));
  }

  @Test
  void deletesReviewOfCurrentUser() throws Exception {
    mockMvc.perform(delete(URL + "/5")).andExpect(status().isNoContent());

    verify(dishReviewService).delete(new DishReviewDeleteParams(1L, 5L));
  }

  @Test
  void returns404WhenReviewIsMissing() throws Exception {
    doThrow(new ResourceNotFoundException("Dish review is not found"))
        .when(dishReviewService)
        .delete(any());

    mockMvc
        .perform(delete(URL + "/5"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Dish review is not found"));
  }

  @Test
  void returns403WhenDeletingSomeoneElsesReview() throws Exception {
    doThrow(new ResourceAccessDeniedException("Review does not belong to the user"))
        .when(dishReviewService)
        .delete(any());

    mockMvc.perform(delete(URL + "/5")).andExpect(status().isForbidden());
  }

  @Test
  void echoesIncomingCorrelationId() throws Exception {
    when(dishReviewService.getAll()).thenReturn(List.of());

    mockMvc
        .perform(get(URL).header(CorrelationIdContext.HEADER, "abc-123"))
        .andExpect(header().string(CorrelationIdContext.HEADER, "abc-123"));
  }

  @Test
  void generatesCorrelationIdWhenMissing() throws Exception {
    when(dishReviewService.getAll()).thenReturn(List.of());

    mockMvc
        .perform(get(URL))
        .andExpect(header().string(CorrelationIdContext.HEADER, notNullValue()));
  }
}
