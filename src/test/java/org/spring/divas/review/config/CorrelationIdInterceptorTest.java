package org.spring.divas.review.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

class CorrelationIdInterceptorTest {

  private final CorrelationIdInterceptor interceptor = new CorrelationIdInterceptor();

  private final MockClientHttpRequest request =
      new MockClientHttpRequest(HttpMethod.GET, URI.create("http://order-service/api/order"));

  private final byte[] body = new byte[0];

  @AfterEach
  void clearContext() {
    CorrelationIdContext.clear();
  }

  @Test
  void propagatesCurrentCorrelationId() throws Exception {
    final ClientHttpRequestExecution execution = mockExecution();
    CorrelationIdContext.set("abc-123");

    interceptor.intercept(request, body, execution);

    assertThat(request.getHeaders().getFirst(CorrelationIdContext.HEADER)).isEqualTo("abc-123");
    verify(execution).execute(request, body);
  }

  @Test
  void generatesCorrelationIdWhenNoneIsSet() throws Exception {
    interceptor.intercept(request, body, mockExecution());

    String header = request.getHeaders().getFirst(CorrelationIdContext.HEADER);
    assertThat(header).isNotBlank();
    assertThat(UUID.fromString(header)).isNotNull();
  }

  @Test
  void clearsCorrelationIdContext() {
    CorrelationIdContext.set("abc-123");

    CorrelationIdContext.clear();

    assertThat(CorrelationIdContext.get()).isEmpty();
  }

  private static ClientHttpRequestExecution mockExecution() throws Exception {
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));
    return execution;
  }
}
