package org.spring.divas.review.config;

import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request,
      byte[] body,
      ClientHttpRequestExecution execution
  ) throws IOException {

    var correlationIdOptional = CorrelationIdContext.get();
    String correlationId = correlationIdOptional.orElseGet(() -> UUID.randomUUID().toString());

    request.getHeaders().set(CorrelationIdContext.HEADER, correlationId);
    if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
      request.getHeaders().setBearerAuth(auth.getToken().getTokenValue());
    }

    return execution.execute(request, body);
  }
}
