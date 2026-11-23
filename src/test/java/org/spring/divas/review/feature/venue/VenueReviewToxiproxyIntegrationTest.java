package org.spring.divas.review.feature.venue;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.divas.review.TestJwt;
import org.spring.divas.review.feature.AbstractToxiproxyIntegrationTest;
import org.spring.divas.review.feature.order.ResilientOrderClient;
import org.spring.divas.review.feature.user.UserDto;
import org.spring.divas.review.feature.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

class VenueReviewToxiproxyIntegrationTest extends AbstractToxiproxyIntegrationTest {

  @Autowired
  private VenueReviewRepository venueReviewRepository;

  @Autowired
  private JsonMapper jsonMapper;

  @MockitoBean
  private ResilientOrderClient orderClient;

  @MockitoBean
  private UserService userService;

  @BeforeEach
  void cleanDatabase() {
    venueReviewRepository.deleteAll();
  }

  @Test
  void createsVenueReviewThroughPostgresProxy() throws Exception {
    VenueReviewRequestDto request = new VenueReviewRequestDto("Cozy", (short) 4, 42L);
    when(orderClient.hasUserBeenToVenue(1L, 42L)).thenReturn(true);
    when(userService.findById(1L)).thenReturn(Optional.of(new UserDto(1L, "Alice Smith")));
    mockMvc.perform(post("/review/venue")
            .with(TestJwt.customer(1L))
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.venueId").value(42))
        .andExpect(jsonPath("$.user.fullName").value("Alice Smith"));
  }

  @Test
  void createsVenueReviewReturnsServerErrorWhenPostgresResponseTimesOut() throws Exception {
    POSTGRES_PROXY.toxics().timeout("postgres-response-timeout", ToxicDirection.DOWNSTREAM, 0);
    evictConnections();
    mockMvc.perform(post("/review/venue")
            .with(TestJwt.customer(1L))
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new VenueReviewRequestDto("Cozy", (short) 4, 42L))))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.title").value("Internal Server Error"));
  }
}
