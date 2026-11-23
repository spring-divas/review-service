package org.spring.divas.review.feature.dish;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.spring.divas.review.TestJwtAuthenticationConfig;
import org.spring.divas.review.common.exception.ResourceAccessDeniedException;
import org.spring.divas.review.common.exception.ResourceAlreadyExistsException;
import org.spring.divas.review.common.exception.ResourceNotFoundException;
import org.spring.divas.review.config.CorrelationIdContext;
import org.spring.divas.review.config.RateLimitFilter;
import org.spring.divas.review.feature.user.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(
    controllers = DishReviewController.class,
    properties = "security.jwt.enabled=false",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class)
)
@AutoConfigureMockMvc
@Import(TestJwtAuthenticationConfig.class)
class DishReviewControllerTest {

  private static final String URL = "/review/dish";

  private static final String REQUEST_WITHOUT_USER_ID =
      """
      {"content": "Tasty", "rating": 5, "dishId": 10}
      """;

  @Autowired private MockMvc mockMvc;

  @Autowired private JsonMapper jsonMapper;

  @MockitoBean private DishReviewService dishReviewService;

  @Test
  void returnsAllReviews() throws Exception {
    DishReviewResponseDto dto =
        new DishReviewResponseDto(
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
    DishReviewResponseDto dto =
        new DishReviewResponseDto(
            100L, new UserDto(1L, "John Doe"), "Tasty", (short) 5, 10L, LocalDate.of(2026, 1, 2));
    when(dishReviewService.create(new DishReviewRequestDto("Tasty", (short) 5, 10L), 1L))
        .thenReturn(dto);

    DishReviewRequestDto request = new DishReviewRequestDto("Tasty", (short) 5, 10L);

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(100))
        .andExpect(jsonPath("$.rating").value(5));
  }

  @Test
  void returns201WithAuthorFromTokenWhenBodyHasNoUserId() throws Exception {
    when(dishReviewService.create(new DishReviewRequestDto("Tasty", (short) 5, 10L, 1L)))
        .thenReturn(new DishReviewResponseDto(
            100L, new UserDto(1L, "John Doe"), "Tasty", (short) 5, 10L, LocalDate.of(2026, 1, 2)));

    mockMvc
        .perform(
            post(URL)
                .with(jwt().jwt(token -> token.subject("1"))
                    .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_WITHOUT_USER_ID))
        .andExpect(status().isCreated());

    verify(dishReviewService).create(new DishReviewRequestDto("Tasty", (short) 5, 10L, 1L));
  }

  @Test
  void returns400WithFieldErrorsForInvalidReview() throws Exception {
    DishReviewRequestDto request = new DishReviewRequestDto(null, (short) 7, null);

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(jsonPath("$.errors.rating").exists())
        .andExpect(jsonPath("$.errors.dishId").exists())
        .andExpect(jsonPath("$.timestamp").value(notNullValue()));

    verify(dishReviewService, never()).create(any(), any());
  }

  @Test
  void returns409WhenReviewAlreadyExists() throws Exception {
    when(dishReviewService.create(any(), any()))
        .thenThrow(new ResourceAlreadyExistsException("Review already exists"));

    DishReviewRequestDto request = new DishReviewRequestDto("Again", (short) 4, 10L);

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value("Review already exists"));
  }

  @Test
  void deletesReviewOfCurrentUser() throws Exception {
    mockMvc.perform(delete(URL + "/5")).andExpect(status().isNoContent());

    verify(dishReviewService).delete(5L, 1L);
  }

  @Test
  void returns404WhenReviewIsMissing() throws Exception {
    doThrow(new ResourceNotFoundException("Dish review is not found"))
        .when(dishReviewService)
        .delete(any(), any());

    mockMvc
        .perform(delete(URL + "/5"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Dish review is not found"));
  }

  @Test
  void returns403WhenDeletingSomeoneElsesReview() throws Exception {
    doThrow(new ResourceAccessDeniedException("Review does not belong to the user"))
        .when(dishReviewService)
        .delete(any(), any());

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
