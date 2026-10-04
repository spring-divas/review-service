package org.spring.divas.review.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String incoming = request.getHeader(CorrelationIdContext.HEADER);
    String correlationId = StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString();

    CorrelationIdContext.set(correlationId);
    response.setHeader(CorrelationIdContext.HEADER, correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      CorrelationIdContext.clear();
    }
  }
}
