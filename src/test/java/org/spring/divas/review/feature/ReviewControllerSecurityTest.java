package org.spring.divas.review.feature;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.spring.divas.review.config.JwtSecurityConfig;
import org.spring.divas.review.config.RateLimitFilter;
import org.spring.divas.review.feature.dish.DishReviewController;
import org.spring.divas.review.feature.dish.DishReviewRequestDto;
import org.spring.divas.review.feature.dish.DishReviewService;
import org.spring.divas.review.feature.venue.VenueReviewController;
import org.spring.divas.review.feature.venue.VenueReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(
    controllers = {DishReviewController.class, VenueReviewController.class},
    properties = "security.jwt.enabled=true",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class)
)
@Import({JwtSecurityConfig.class, ReviewControllerSecurityTest.MethodSecurity.class})
class ReviewControllerSecurityTest {

  private static final String REVIEW_JSON_WITH_FOREIGN_USER =
      """
      {"content": "Tasty", "rating": 5, "dishId": 10, "userId": 99}
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DishReviewService dishReviewService;

  @MockitoBean private VenueReviewService venueReviewService;

  @MockitoBean(name = "jwtPublicKey")
  private RSAPublicKey jwtPublicKey;

  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void rejectsAnonymousReviewCreation() throws Exception {
    mockMvc.perform(createDishReview()).andExpect(status().isUnauthorized());

    verifyNoInteractions(dishReviewService);
  }

  @Test
  void takesAuthorFromTokenAndIgnoresUserIdInBody() throws Exception {
    mockMvc.perform(createDishReview().with(customer())).andExpect(status().isCreated());

    verify(dishReviewService).create(new DishReviewRequestDto("Tasty", (short) 5, 10L), 10L);
  }

  @Test
  void returns403WhenTokenHasNoReviewerRole() throws Exception {
    RequestPostProcessor venueStaff = jwt()
        .jwt(token -> token.subject("20").claim("roles", List.of("SENIOR 5")))
        .authorities(new SimpleGrantedAuthority("ROLE_SENIOR 5"));

    mockMvc.perform(createDishReview().with(venueStaff)).andExpect(status().isForbidden());

    verifyNoInteractions(dishReviewService);
  }

  @Test
  void deletesReviewOnBehalfOfTokenOwner() throws Exception {
    mockMvc.perform(delete("/review/venue/{id}", 5L).with(customer()))
        .andExpect(status().isNoContent());

    verify(venueReviewService).delete(eq(5L), eq(10L));
  }

  @Test
  void rejectsAnonymousReviewDeletion() throws Exception {
    mockMvc.perform(delete("/review/venue/{id}", 5L)).andExpect(status().isUnauthorized());

    verify(venueReviewService, never()).delete(any(), any());
  }

  private static MockHttpServletRequestBuilder createDishReview() {
    return post("/review/dish")
        .contentType(MediaType.APPLICATION_JSON)
        .content(REVIEW_JSON_WITH_FOREIGN_USER);
  }

  private static RequestPostProcessor customer() {
    return jwt()
        .jwt(token -> token.subject("10").claim("roles", List.of("CUSTOMER")))
        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }

  @TestConfiguration
  @EnableWebSecurity
  @EnableMethodSecurity
  static class MethodSecurity {
  }
}
