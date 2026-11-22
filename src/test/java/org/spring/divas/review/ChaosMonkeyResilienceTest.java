package org.spring.divas.review;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultException;
import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeySettings;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.divas.review.feature.dish.DishReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.ResourceAccessException;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(properties = {
    "chaos.monkey.watcher.service=false",
    "chaos.monkey.watcher.rest-controller=false",
    "resilience4j.retry.instances.orderClient.wait-duration=50ms",
    "resilience4j.circuitbreaker.instances.orderClient.wait-duration-in-open-state=1s"
})
@AutoConfigureMockMvc
@ActiveProfiles("chaos-monkey")
@EnableWireMock(@ConfigureWireMock(name = "order-service", baseUrlProperties = "order.service.url"))
class ChaosMonkeyResilienceTest {

  private static final long USER_ID = 1L;

  @Value("${chaos-test.order-service.ordered-dish-path}")
  private String orderedDishPath;

  @Value("${chaos-test.dish-review-path}")
  private String dishReviewPath;

  @InjectWireMock("order-service")
  private WireMockServer orderService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ChaosMonkeySettings chaosMonkey;

  @Autowired
  private CircuitBreakerRegistry circuitBreakers;

  @Autowired
  private DishReviewRepository dishReviewRepository;

  @BeforeEach
  void setUp() {
    circuitBreakers.circuitBreaker("orderClient").reset();
    orderService.stubFor(get(urlPathEqualTo(orderedDishPath))
        .willReturn(okJson("true")));

    AssaultProperties assaults = chaosMonkey.getAssaultProperties();
    assaults.setLevel(1);
    assaults.setDeterministic(true);
    assaults.setLatencyActive(false);
    assaults.setExceptionsActive(false);
  }

  @AfterEach
  void stopChaos() {
    chaosMonkey.getChaosMonkeyProperties().setEnabled(false);
  }

  @Test
  void reviewsDegradeGracefullyWhileOrderServiceIsDownAndRecoverAfterwards() throws Exception {
    createReview(101L).andExpect(status().isCreated());

    unleashConnectionFailures();
    orderService.resetRequests();

    for (int i = 0; i < 5; i++) {
      createReview(102L)
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.detail").value("User cannot rate the dish they have not ordered"));
    }
    assertThat(dishReviewRepository.existsByUserIdAndDishId(USER_ID, 102L)).isFalse();

    CircuitBreaker circuitBreaker = circuitBreakers.circuitBreaker("orderClient");
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    orderService.verify(0, getRequestedFor(urlPathEqualTo(orderedDishPath)));

    chaosMonkey.getChaosMonkeyProperties().setEnabled(false);
    await().atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(200))
        .untilAsserted(() -> createReview(103L).andExpect(status().isCreated()));
    assertThat(circuitBreaker.getState()).isNotEqualTo(CircuitBreaker.State.OPEN);
    orderService.verify(getRequestedFor(urlPathEqualTo(orderedDishPath)));
  }

  @Test
  void chaosMonkeyCannotBeControlledOverHttp() throws Exception {
    mockMvc.perform(post("/actuator/chaosmonkey/enable")).andExpect(status().isNotFound());
    mockMvc.perform(post("/actuator/chaosmonkey/assaults")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"exceptionsActive\": true}"))
        .andExpect(status().isNotFound());

    assertThat(chaosMonkey.getChaosMonkeyProperties().isEnabled()).isFalse();
  }

  private void unleashConnectionFailures() {
    AssaultException exception = new AssaultException();
    exception.setType(ResourceAccessException.class.getName());
    exception.setArguments(List.of(new AssaultException.ExceptionArgument(
        String.class.getName(), "Chaos Monkey: order-service is unreachable")));

    chaosMonkey.getAssaultProperties().setException(exception);
    chaosMonkey.getAssaultProperties().setExceptionsActive(true);
    chaosMonkey.getChaosMonkeyProperties().setEnabled(true);
  }

  private ResultActions createReview(long dishId) throws Exception {
    return mockMvc.perform(post(dishReviewPath)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"content\": \"Tasty\", \"rating\": 5, \"dishId\": %d, \"userId\": %d}"
            .formatted(dishId, USER_ID)));
  }
}
