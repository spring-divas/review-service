package org.spring.divas.review.config;

import java.net.http.HttpClient;
import java.time.Duration;
import lombok.experimental.UtilityClass;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@UtilityClass
public final class ClientFactory {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

  private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

  public static <T> T create(RestClient.Builder builder, String baseUrl, Class<T> clientType) {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();

    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(READ_TIMEOUT);

    RestClient restClient = builder
        .requestFactory(requestFactory)
        .baseUrl(baseUrl)
        .requestInterceptor(new CorrelationIdInterceptor())
        .build();

    HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(restClient))
        .build();

    return proxyFactory.createClient(clientType);
  }
}
