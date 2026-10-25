package org.spring.divas.review.config;

import org.spring.divas.review.feature.order.OrderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

  @Value("${order.service.url}")
  private String orderClientUrl;

  @Bean
  public OrderClient orderClient(RestClient.Builder builder) {
    return ClientFactory.create(builder, orderClientUrl, OrderClient.class);
  }
}
