package org.spring.divas.review;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@TestConfiguration(proxyBeanMethods = false)
@EnableWebSecurity
public class TestJwtAuthenticationConfig {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  SecurityFilterChain testSecurityFilterChain(HttpSecurity http) {
    return http.csrf(csrf -> csrf.disable())
        .addFilterBefore(new TestJwtAuthenticationFilter(), AnonymousAuthenticationFilter.class)
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .build();
  }

  /**
   * Authenticates every request as customer 1 inside the security filter chain, so that
   * {@code @AuthenticationPrincipal Jwt} and outgoing token propagation see a real JWT.
   */
  private static final class TestJwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
      Jwt jwt = Jwt.withTokenValue("test-token")
          .header("alg", "none")
          .subject("1")
          .claim("roles", List.of("CUSTOMER"))
          .build();
      SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
          jwt, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
      filterChain.doFilter(request, response);
    }
  }
}
