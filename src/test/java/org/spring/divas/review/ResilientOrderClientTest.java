package org.spring.divas.review;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.divas.review.feature.order.ResilientOrderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(properties = {
    "resilience4j.retry.instances.orderClient.max-attempts=1",
    "resilience4j.circuitbreaker.instances.orderClient.sliding-window-size=2",
    "resilience4j.circuitbreaker.instances.orderClient.minimum-number-of-calls=2",
    "resilience4j.circuitbreaker.instances.orderClient.failure-rate-threshold=50"
})
@EnableWireMock(@ConfigureWireMock(name = "order-service", baseUrlProperties = "order.service.url"))
class ResilientOrderClientTest {

  private static final String ORDERED_DISH_PATH = "/api/order/exists/dish";

  private static final String VISITED_VENUE_PATH = "/api/order/exists/venue";

  @InjectWireMock("order-service")
  private WireMockServer orderService;

  @Autowired
  private ResilientOrderClient orderClient;

  @Autowired
  private CircuitBreakerRegistry circuitBreakers;

  @BeforeEach
  void setUp() {
    circuitBreakers.circuitBreaker("orderClient").reset();
    orderService.resetAll();
    stubSlowResponse(ORDERED_DISH_PATH);
    stubSlowResponse(VISITED_VENUE_PATH);
  }

  @Test
  void usesFallbackWhenVenueCheckTimesOut() {
    assertThat(orderClient.hasUserBeenToVenue(1L, 20L)).isFalse();
    orderService.verify(getRequestedFor(urlPathEqualTo(VISITED_VENUE_PATH)));
  }

  @Test
  void returnsOrderCheckResultWhenOrderServiceResponds() {
    orderService.resetAll();
    orderService.stubFor(get(urlPathEqualTo(ORDERED_DISH_PATH))
        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("true")));

    assertThat(orderClient.hasUserOrderedDish(1L, 10L)).isTrue();
    assertThat(circuitBreakers.circuitBreaker("orderClient").getState())
        .isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void opensCircuitBreakerAndUsesFallbackWhenOrderServiceIsSlow() {
    CircuitBreaker circuitBreaker = circuitBreakers.circuitBreaker("orderClient");

    assertThat(orderClient.hasUserOrderedDish(1L, 10L)).isFalse();
    assertThat(orderClient.hasUserOrderedDish(1L, 10L)).isFalse();
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    orderService.resetRequests();

    assertThat(orderClient.hasUserOrderedDish(1L, 10L)).isFalse();
    orderService.verify(0, getRequestedFor(urlPathEqualTo(ORDERED_DISH_PATH)));
  }

  private void stubSlowResponse(String path) {
    orderService.stubFor(get(urlPathEqualTo(path))
        .willReturn(aResponse().withFixedDelay(3_100).withBody("true")));
  }
}
