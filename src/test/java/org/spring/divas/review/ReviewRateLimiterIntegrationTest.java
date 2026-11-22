package org.spring.divas.review;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.spring.divas.review.feature.dish.DishReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
    "resilience4j.ratelimiter.instances.reviewApi.limit-for-period=3",
    "resilience4j.ratelimiter.instances.reviewApi.limit-refresh-period=1m",
    "resilience4j.ratelimiter.instances.reviewApi.timeout-duration=0s"
})
@AutoConfigureMockMvc
class ReviewRateLimiterIntegrationTest {

  private static final String DISH_REVIEW_JSON = """
      {"content": "Tasty", "rating": 5, "dishId": 1, "userId": 1}
      """;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DishReviewService dishReviewService;

  @Test
  void shouldRejectRequestsWithProblemDetailWhenRateLimitIsExceeded() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(createDishReview()).andExpect(status().isCreated());
    }

    mockMvc.perform(createDishReview())
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1"))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.title").value("Too Many Requests"));

    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().is(not(HttpStatus.TOO_MANY_REQUESTS.value())));
  }

  private MockHttpServletRequestBuilder createDishReview() {
    return post("/review/dish")
        .contentType(MediaType.APPLICATION_JSON)
        .content(DISH_REVIEW_JSON);
  }
}
