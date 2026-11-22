package org.spring.divas.review.feature.venue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.spring.divas.review.config.RateLimitFilter;
import org.spring.divas.review.feature.user.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(
    controllers = VenueReviewController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class)
)
class VenueReviewControllerTest {

  private static final String URL = "/review/venue";

  @Autowired private MockMvc mockMvc;

  @Autowired private JsonMapper jsonMapper;

  @MockitoBean private VenueReviewService venueReviewService;

  @Test
  void returnsAllReviews() throws Exception {
    VenueReviewResponseDto dto =
        new VenueReviewResponseDto(
            100L, new UserDto(1L, "Alice Smith"), "Cozy", (short) 4, 20L, LocalDate.of(2026, 1, 2));
    when(venueReviewService.getAll()).thenReturn(List.of(dto));

    mockMvc
        .perform(get(URL))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(100))
        .andExpect(jsonPath("$[0].venueId").value(20))
        .andExpect(jsonPath("$[0].user.fullName").value("Alice Smith"));
  }

  @Test
  void returns201WithCreatedReview() throws Exception {
    VenueReviewResponseDto dto =
        new VenueReviewResponseDto(
            100L, new UserDto(1L, "Alice Smith"), "Cozy", (short) 4, 20L, LocalDate.of(2026, 1, 2));
    when(venueReviewService.create(new VenueReviewRequestDto("Cozy", (short) 4, 20L, 1L)))
        .thenReturn(dto);

    VenueReviewRequestDto request = new VenueReviewRequestDto("Cozy", (short) 4, 20L, 1L);

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(100));
  }

  @Test
  void returns400WithFieldErrorsForInvalidReview() throws Exception {
    VenueReviewRequestDto request = new VenueReviewRequestDto(null, (short) 0, 20L, null);

    mockMvc
        .perform(
            post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.rating").exists())
        .andExpect(jsonPath("$.errors.userId").exists());

    verify(venueReviewService, never()).create(any());
  }

  @Test
  void returns400ForMalformedJson() throws Exception {
    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest());

    verify(venueReviewService, never()).create(any());
  }

  @Test
  void deletesReviewOfCurrentUser() throws Exception {
    mockMvc.perform(delete(URL + "/5")).andExpect(status().isNoContent());

    verify(venueReviewService).delete(5L, 1L);
  }
}
