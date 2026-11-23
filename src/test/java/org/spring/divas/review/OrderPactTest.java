package org.spring.divas.review;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.spring.divas.review.config.ClientFactory;
import org.spring.divas.review.feature.order.OrderClient;
import org.springframework.web.client.RestClient;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "order-service")
class OrderPactTest {

  @Pact(consumer = "review-service", provider = "order-service")
  public V4Pact createOrderPact(PactBuilder builder) {
    return builder
        .expectsToReceiveHttpInteraction("a request to check a dish order", interaction -> interaction
            .withRequest(request -> request
                .path("/api/order/exists/dish")
                .queryParameter("dishId", "456")
                .method("GET"))
            .willRespondWith(response -> response
                .status(200)
                .body("true", "application/json")))
        .expectsToReceiveHttpInteraction("a request to check a venue visit", interaction -> interaction
            .withRequest(request -> request
                .path("/api/order/exists/venue")
                .queryParameter("venueId", "456")
                .method("GET"))
            .willRespondWith(response -> response
                .status(200)
                .body("true", "application/json")))
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "createOrderPact")
  void clientReadsReviewEligibilityFromContract(MockServer mockServer) {
    OrderClient client = ClientFactory.create(RestClient.builder(), mockServer.getUrl(), OrderClient.class);
    assertThat(client.hasUserOrderedDish(456L)).isTrue();
    assertThat(client.hasUserBeenToVenue(456L)).isTrue();
  }
}
